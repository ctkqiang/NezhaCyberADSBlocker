package xin.ctkqiang.nezha_cyber.ads_block.feature.application

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import xin.ctkqiang.nezha_cyber.ads_block.R
import xin.ctkqiang.nezha_cyber.ads_block.ui.application.rememberApplicationIcon
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaAppIcon
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaEmptyState
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaListScaffold
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaPillButton
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaSurfaceCard
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaSwitch
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.LocalApplicationPermissionSource
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.LocalInstalledApplicationSource
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.LocalObservationStore
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.LocalProtectedApplicationStore
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.NezhaDataPreviewHost
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaThemePreview
import xin.ctkqiang.nezha_cyber.ads_block.ui.vpn.LocalVpnAuthorizationRequester
import xin.ctkqiang.nezha_cyber.ads_block.ui.vpn.LocalVpnController
import xin.ctkqiang.nezha_cyber.ads_block.ui.vpn.NezhaVpnPreviewHost

/**
 * 应用列表：逐应用保护开关 + 侵入程度摘要。
 *
 * 这是「不影响其它应用」的执行入口：一旦用户明确选中若干应用，隧道就只接管这些应用，
 * 其余应用的流量完全不经过过滤层（工程规则第 6 节）。未选择任何应用时保持默认行为——
 * 接管全部应用的域名解析，这句语义写在页面顶部，避免用户误以为「没选就是没保护」。
 *
 * 每一行同时给出该应用**已授予的运行时权限数**，让「谁比较侵入」可以一眼扫出来；
 * 完整权限清单在应用详情页，点整行进入。权限不能在这里改：平台只允许设备所有者或
 * 资料所有者改动其它应用的授权状态，普通应用做不到（见 `ApplicationPermissionSource`）。
 */
@Composable
fun ApplicationsScreen(modifier: Modifier = Modifier, onOpenDetail: (String) -> Unit = {}) {
    val installedApplicationSource = LocalInstalledApplicationSource.current
    val applicationPermissionSource = LocalApplicationPermissionSource.current
    val protectedApplicationStore = LocalProtectedApplicationStore.current
    val observationStore = LocalObservationStore.current
    val vpnController = LocalVpnController.current
    val authorizationRequester = LocalVpnAuthorizationRequester.current
    val viewModel: ApplicationsViewModel = viewModel(
        factory = ApplicationsViewModel.factory(
            installedApplicationSource = installedApplicationSource,
            applicationPermissionSource = applicationPermissionSource,
            protectedApplicationStore = protectedApplicationStore,
            observationStore = observationStore,
            vpnController = vpnController,
        ),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ApplicationsUiEffect.RequestVpnAuthorization -> authorizationRequester.request { granted ->
                    viewModel.dispatch(ApplicationsUiIntent.AuthorizationResult(granted))
                }
                ApplicationsUiEffect.OpenApplicationSettings -> openApplicationDetailsSettings(context)
            }
        }
    }
    ApplicationsContent(
        uiState = uiState,
        onIntent = viewModel::dispatch,
        onOpenDetail = onOpenDetail,
        modifier = modifier,
    )
}

@Composable
private fun ApplicationsContent(
    uiState: ApplicationsUiState,
    onIntent: (ApplicationsUiIntent) -> Unit,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NezhaListScaffold(
        modifier = modifier,
        header = { ApplicationsHeader(uiState = uiState, onIntent = onIntent) },
    ) {
        if (uiState.rows.isEmpty()) {
            item(key = EMPTY_ITEM_KEY) {
                ApplicationsEmptyState(
                    isLoading = uiState.isLoading,
                    needsPackageVisibility = uiState.needsPackageVisibility,
                    onRetry = { onIntent(ApplicationsUiIntent.RetryLoad) },
                    onOpenSettings = { onIntent(ApplicationsUiIntent.OpenSettings) },
                )
            }
        } else {
            items(items = uiState.rows, key = { row -> row.packageName }) { row ->
                ApplicationRow(
                    row = row,
                    enabled = !uiState.isRestarting,
                    onToggle = { onIntent(ApplicationsUiIntent.ToggleProtection(row.packageName)) },
                    onOpenDetail = { onOpenDetail(row.packageName) },
                )
            }
        }
    }
}

/**
 * 应用列表空状态。
 *
 * 分三种情况：
 * - 加载中：只显示「正在读取」，不提供按钮（操作也没用）。
 * - 缺包可见性权限：给出权限引导文案 + 「打开权限设置」+「重新读取」两个按钮。
 * - 其它原因导致的空（罕见）：给出通用文案 + 「重新读取」。
 *
 * 按钮放在空状态卡片下方而不是塞进卡片里：卡片是信息容器，按钮是动作容器，
 * 两者职责不同，分开排版才能让视觉层次清晰。
 */
@Composable
private fun ApplicationsEmptyState(
    isLoading: Boolean,
    needsPackageVisibility: Boolean,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NezhaEmptyState(
            description = stringResource(
                when {
                    isLoading -> R.string.applications_loading
                    needsPackageVisibility -> R.string.applications_empty_no_permission
                    else -> R.string.applications_empty
                },
            ),
            tag = stringResource(R.string.applications_empty_tag),
        )
        if (!isLoading) {
            Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
            Row(
                horizontalArrangement = Arrangement.spacedBy(NezhaDimens.blockGap),
            ) {
                if (needsPackageVisibility) {
                    NezhaPillButton(
                        text = stringResource(R.string.applications_open_settings),
                        onClick = onOpenSettings,
                    )
                }
                NezhaPillButton(
                    text = stringResource(R.string.applications_retry),
                    onClick = onRetry,
                )
            }
        }
    }
}

@Composable
private fun ApplicationsHeader(uiState: ApplicationsUiState, onIntent: (ApplicationsUiIntent) -> Unit) {
    val palette = NezhaTheme.palette
    BasicText(
        text = if (uiState.protectedCount == 0) {
            stringResource(R.string.applications_summary_default)
        } else {
            stringResource(R.string.applications_summary_selected, uiState.protectedCount)
        },
        style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
    )
    if (uiState.protectedCount > 0) {
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        NezhaPillButton(
            text = stringResource(R.string.applications_clear_selection),
            enabled = !uiState.isRestarting,
            onClick = { onIntent(ApplicationsUiIntent.ClearSelection) },
        )
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        BasicText(
            text = stringResource(R.string.applications_restart_hint),
            style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
        )
    }
}

@Composable
private fun ApplicationRow(
    row: ApplicationRow,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onOpenDetail: () -> Unit,
) {
    val palette = NezhaTheme.palette
    val icon = rememberApplicationIcon(packageName = row.packageName)
    NezhaSurfaceCard(modifier = Modifier.clickable(role = Role.Button, onClick = onOpenDetail)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NezhaAppIcon(icon = icon, label = row.label)
            Spacer(modifier = Modifier.width(NezhaDimens.blockGap))
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = row.label,
                    style = NezhaTheme.typography.title.copy(color = palette.textPrimary),
                )
                Spacer(modifier = Modifier.height(NezhaDimens.tightGap))
                BasicText(
                    text = row.packageName,
                    style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
                )
                Spacer(modifier = Modifier.height(NezhaDimens.tightGap))
                BasicText(
                    text = stringResource(
                        R.string.applications_row_stats,
                        row.observedCount,
                        row.blockedCount,
                    ),
                    style = NezhaTheme.typography.caption.copy(
                        color = if (row.blockedCount > 0) palette.brand else palette.textSecondary,
                    ),
                )
                Spacer(modifier = Modifier.height(NezhaDimens.tightGap))
                BasicText(
                    text = permissionSummaryText(summary = row.permissions),
                    style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
                )
            }
            Spacer(modifier = Modifier.width(NezhaDimens.blockGap))
            NezhaSwitch(
                checked = row.isProtected,
                onCheckedChange = { checked -> if (enabled) onToggle(checked) },
            )
        }
    }
}

/**
 * 运行时权限摘要。
 *
 * 「读不到」与「没有」必须分开措辞：前者是数据缺失，后者是审计结论，
 * 把前者显示成「无运行时权限」等于替一个读不到的应用背书。
 */
@Composable
private fun permissionSummaryText(summary: DangerousPermissionSummary?): String = when {
    summary == null -> stringResource(R.string.applications_permissions_unknown)
    summary.total == 0 -> stringResource(R.string.applications_permissions_none)
    else -> stringResource(R.string.applications_permissions_runtime, summary.granted, summary.total)
}

/**
 * 打开本应用的系统详情设置页。
 *
 * `QUERY_ALL_PACKAGES` 不是运行时权限，不能用 `requestPermissions` 弹出系统对话框，
 * 用户只能在系统设置里手动授予。这里直接跳到应用详情页，用户在「权限」里找到
 * 「查询所有应用」并开启即可。返回后用户点「重新读取」即可刷新列表。
 */
private fun openApplicationDetailsSettings(context: android.content.Context) {
    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = android.net.Uri.fromParts("package", context.packageName, null)
        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

private const val EMPTY_ITEM_KEY = "applications-empty"

@Preview(name = "应用列表 · 明暗对照", showBackground = true, widthDp = 800, heightDp = 1100)
@Composable
private fun ApplicationsScreenPreview() {
    NezhaThemePreview {
        NezhaVpnPreviewHost {
            NezhaDataPreviewHost {
                ApplicationsScreen()
            }
        }
    }
}
