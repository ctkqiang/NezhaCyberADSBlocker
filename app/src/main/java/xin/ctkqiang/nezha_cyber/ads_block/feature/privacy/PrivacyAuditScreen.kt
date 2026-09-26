package xin.ctkqiang.nezha_cyber.ads_block.feature.privacy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.LocalApplicationPermissionSource
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.LocalInstalledApplicationSource
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.NezhaDataPreviewHost
import xin.ctkqiang.nezha_cyber.ads_block.ui.privacy.LocalSystemSettingsLauncher
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaThemePreview

private const val PERMISSION_LABEL_MAX_LINES = 2

private const val EXPLAINER_ITEM_KEY = "privacy-audit-explainer"

private const val SENSORS_ITEM_KEY = "privacy-audit-sensors"

private const val VISIBILITY_ITEM_KEY = "privacy-audit-visibility"

private const val UNREADABLE_ITEM_KEY = "privacy-audit-unreadable"

private const val EMPTY_ITEM_KEY = "privacy-audit-empty"

/**
 * 隐私与传感器页。
 *
 * 这一页做两件事，都不是「替用户关掉什么」：
 * 1. **审计**：列出当前持有敏感权限的应用，并给出一键跳到该系统设置页的入口；
 * 2. **指路**：说清陀螺仪为什么关不掉，以及系统里唯一能真正停掉它的开关在哪里。
 *
 * 刻意不提供开关，是因为提供不了：Android 没有给运动传感器设权限，也**不允许**普通应用改动
 * 其它应用的授权状态。摆一个点不动的开关比不放开关更糟——用户会以为已经被保护了
 * （工程规则第 32 节）。页面把这条边界写在最前面，而不是藏进说明文字里。
 *
 * 页面只渲染状态、派发意图：权限读取与筛选都在领域层与 ViewModel，界面不接触 `PackageManager`
 * 也不接触 `Intent`（工程规则第 40.3、40.4 节）。
 */
@Composable
fun PrivacyAuditScreen(modifier: Modifier = Modifier) {
    val installedApplicationSource = LocalInstalledApplicationSource.current
    val applicationPermissionSource = LocalApplicationPermissionSource.current
    val settingsLauncher = LocalSystemSettingsLauncher.current
    val viewModel: PrivacyAuditViewModel = viewModel(
        factory = PrivacyAuditViewModel.factory(
            installedApplicationSource = installedApplicationSource,
            applicationPermissionSource = applicationPermissionSource,
        ),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actions = remember(settingsLauncher) {
        PrivacyAuditActions(
            canOpenPrivacyDashboard = settingsLauncher.canOpenPrivacyDashboard,
            openApplicationSettings = settingsLauncher::openApplicationSettings,
            openPrivacyDashboard = settingsLauncher::openPrivacyDashboard,
            openDeveloperOptions = settingsLauncher::openDeveloperOptions,
        )
    }
    PrivacyAuditContent(
        uiState = uiState,
        actions = actions,
        onIntent = viewModel::dispatch,
        modifier = modifier,
    )
}

/**
 * 系统设置跳转能力的打包。
 *
 * 四项打包成一个参数，而不是让内容函数的签名再长四行：它们属于同一类东西——**都要由
 * 上层注入，且都不改变状态**。理由与网络层的 `RelayInputs` 一致：一堆同源参数摊在签名里，
 * 读的人要先把它们重新归类才能读懂。
 */
private class PrivacyAuditActions(
    val canOpenPrivacyDashboard: Boolean,
    val openApplicationSettings: (String) -> Unit,
    val openPrivacyDashboard: () -> Unit,
    val openDeveloperOptions: () -> Unit,
)

@Composable
private fun PrivacyAuditContent(
    uiState: PrivacyAuditUiState,
    actions: PrivacyAuditActions,
    onIntent: (PrivacyAuditUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    NezhaListScaffold(
        modifier = modifier,
        // 表头钉在列表内部且必须保持矮：这里只放扫描进度与重扫入口，说明性卡片放进列表，
        // 让它们随内容滚走，否则矮屏上表头会把列表压没。
        header = { AuditSummary(uiState = uiState, onIntent = onIntent) },
    ) {
        item(key = EXPLAINER_ITEM_KEY) {
            BoundaryCard(
                canOpenPrivacyDashboard = actions.canOpenPrivacyDashboard,
                onOpenPrivacyDashboard = actions.openPrivacyDashboard,
            )
        }
        item(key = SENSORS_ITEM_KEY) { SensorsCard(onOpenDeveloperOptions = actions.openDeveloperOptions) }
        if (!uiState.hasPackageVisibility) {
            item(key = VISIBILITY_ITEM_KEY) {
                BasicText(
                    text = stringResource(R.string.privacy_audit_no_visibility),
                    style = NezhaTheme.typography.caption.copy(color = NezhaTheme.palette.brand),
                )
            }
        }
        if (uiState.unreadableApplicationCount > 0) {
            item(key = UNREADABLE_ITEM_KEY) {
                BasicText(
                    text = stringResource(R.string.privacy_audit_unreadable, uiState.unreadableApplicationCount),
                    style = NezhaTheme.typography.caption.copy(color = NezhaTheme.palette.textSecondary),
                )
            }
        }
        // 首次扫描期间不显示空状态：那时「一个都没有」只是还没读完，不是真的没有。
        // 把尚未完成的结果说成结论，正是这一页最不该犯的错。
        if (!uiState.isScanning) {
            if (uiState.entries.isEmpty()) {
                item(key = EMPTY_ITEM_KEY) {
                    NezhaEmptyState(
                        description = stringResource(R.string.privacy_audit_empty),
                        tag = stringResource(R.string.privacy_audit_empty_tag),
                    )
                }
            } else {
                items(items = uiState.entries, key = { row -> row.packageName }) { row ->
                    AuditRowCard(row = row, onOpenApplicationSettings = actions.openApplicationSettings)
                }
            }
        }
    }
}

@Composable
private fun AuditSummary(uiState: PrivacyAuditUiState, onIntent: (PrivacyAuditUiIntent) -> Unit) {
    val palette = NezhaTheme.palette
    BasicText(
        text = if (uiState.isScanning) {
            stringResource(R.string.privacy_audit_scanning)
        } else {
            stringResource(R.string.privacy_audit_summary, uiState.scannedApplicationCount, uiState.entries.size)
        },
        style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
    )
    Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
    NezhaPillButton(
        text = stringResource(R.string.privacy_audit_rescan),
        onClick = { onIntent(PrivacyAuditUiIntent.Rescan) },
    )
}

/**
 * 能力边界。
 *
 * 用品牌色标题而不是普通卡片：用户带着「我要关掉陀螺仪」的预期进来，第一眼看到的必须是
 * 「为什么这里没有那个开关」，否则他会以为功能藏在别处，然后去翻每一页。
 */
@Composable
private fun BoundaryCard(canOpenPrivacyDashboard: Boolean, onOpenPrivacyDashboard: () -> Unit) {
    val palette = NezhaTheme.palette
    NezhaSurfaceCard {
        BasicText(
            text = stringResource(R.string.privacy_audit_boundary_title),
            style = NezhaTheme.typography.title.copy(color = palette.brand),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        BasicText(
            text = stringResource(R.string.privacy_audit_boundary_note),
            style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        if (canOpenPrivacyDashboard) {
            NezhaPillButton(
                text = stringResource(R.string.privacy_audit_dashboard_action),
                onClick = onOpenPrivacyDashboard,
            )
        } else {
            BasicText(
                text = stringResource(R.string.privacy_audit_dashboard_unavailable),
                style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
            )
        }
    }
}

/** 系统传感器开关的指路卡片。这里给出唯一的真实手段，而不是本应用的一个假开关。 */
@Composable
private fun SensorsCard(onOpenDeveloperOptions: () -> Unit) {
    val palette = NezhaTheme.palette
    NezhaSurfaceCard {
        BasicText(
            text = stringResource(R.string.privacy_sensors_title),
            style = NezhaTheme.typography.title.copy(color = palette.textPrimary),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        BasicText(
            text = stringResource(R.string.privacy_sensors_note),
            style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        NezhaPillButton(
            text = stringResource(R.string.privacy_sensors_open_developer_options),
            onClick = onOpenDeveloperOptions,
        )
    }
}

@Composable
private fun AuditRowCard(row: PrivacyAuditRow, onOpenApplicationSettings: (String) -> Unit) {
    val palette = NezhaTheme.palette
    val icon = rememberApplicationIcon(packageName = row.packageName)
    val separator = stringResource(R.string.privacy_audit_permission_separator)
    NezhaSurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NezhaAppIcon(icon = icon, label = row.appLabel, size = NezhaDimens.appIconSize)
            Spacer(modifier = Modifier.width(NezhaDimens.blockGap))
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = row.appLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = NezhaTheme.typography.title.copy(color = palette.textPrimary),
                )
                Spacer(modifier = Modifier.height(NezhaDimens.tightGap))
                BasicText(
                    text = stringResource(R.string.privacy_audit_row_permissions, row.grantedCount),
                    style = NezhaTheme.typography.caption.copy(color = palette.brand),
                )
            }
        }
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        BasicText(
            text = row.permissionLabels.joinToString(separator = separator),
            maxLines = PERMISSION_LABEL_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
            style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        NezhaPillButton(
            text = stringResource(R.string.privacy_audit_row_action),
            onClick = { onOpenApplicationSettings(row.packageName) },
        )
    }
}

@Preview(name = "隐私与传感器 · 明暗对照", showBackground = true, widthDp = 820, heightDp = 1600)
@Composable
private fun PrivacyAuditScreenPreview() {
    NezhaThemePreview {
        NezhaDataPreviewHost {
            PrivacyAuditScreen()
        }
    }
}
