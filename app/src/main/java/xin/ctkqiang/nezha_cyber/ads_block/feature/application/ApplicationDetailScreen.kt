package xin.ctkqiang.nezha_cyber.ads_block.feature.application

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import xin.ctkqiang.nezha_cyber.ads_block.R
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.ApplicationPermission
import xin.ctkqiang.nezha_cyber.ads_block.ui.application.rememberApplicationIcon
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaAppIcon
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaEmptyState
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaScreenScaffold
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaSurfaceCard
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.LocalApplicationPermissionSource
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.LocalInstalledApplicationSource
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.LocalObservationStore
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.NezhaDataPreviewHost
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaThemePreview

/**
 * 应用详情：单个应用的权限清单与请求过的域名。
 *
 * 这一页是**审计视图**，不是控制台：它回答「它拿了哪些权限、它在跟谁通信」，
 * 但不提供任何修改入口。原因有两条，都写在页面底部的说明里，不藏起来：
 * Android 只允许设备所有者或资料所有者改动其它应用的授权状态；而「某次请求用了哪个权限」
 * 在系统层面根本不存在（第 32 节要求对能力边界保持透明）。
 *
 * 页面从「应用列表」点进来，选中的包名由外壳持有，因此这里的 [packageName] 是入参。
 */
@Composable
fun ApplicationDetailScreen(packageName: String?, modifier: Modifier = Modifier) {
    val observationStore = LocalObservationStore.current
    val installedApplicationSource = LocalInstalledApplicationSource.current
    val applicationPermissionSource = LocalApplicationPermissionSource.current
    val viewModel: ApplicationDetailViewModel = viewModel(
        // key 必须带上包名：ViewModel 默认按类型取实例，切换应用时不带 key 会继续用上一个应用的数据。
        key = packageName.orEmpty(),
        factory = ApplicationDetailViewModel.factory(
            packageName = packageName,
            observationStore = observationStore,
            installedApplicationSource = installedApplicationSource,
            applicationPermissionSource = applicationPermissionSource,
        ),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ApplicationDetailContent(uiState = uiState, modifier = modifier)
}

@Composable
private fun ApplicationDetailContent(uiState: ApplicationDetailUiState, modifier: Modifier = Modifier) {
    val palette = NezhaTheme.palette
    NezhaScreenScaffold(modifier = modifier) {
        when {
            uiState.packageName == null -> NezhaEmptyState(
                description = stringResource(R.string.application_detail_empty),
                tag = stringResource(R.string.application_detail_empty_tag),
            )

            uiState.isLoading -> BasicText(
                text = stringResource(R.string.application_detail_loading),
                style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
            )

            else -> {
                IdentityCard(uiState = uiState)
                Spacer(modifier = Modifier.height(NezhaDimens.sectionGap))
                PermissionCard(uiState = uiState)
                Spacer(modifier = Modifier.height(NezhaDimens.sectionGap))
                DomainCard(uiState = uiState)
                Spacer(modifier = Modifier.height(NezhaDimens.sectionGap))
                BasicText(
                    text = stringResource(R.string.application_detail_boundary_note),
                    style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
                )
            }
        }
    }
}

@Composable
private fun IdentityCard(uiState: ApplicationDetailUiState) {
    val palette = NezhaTheme.palette
    val icon = rememberApplicationIcon(packageName = uiState.packageName)
    NezhaSurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NezhaAppIcon(icon = icon, label = uiState.appLabel)
            Spacer(modifier = Modifier.width(NezhaDimens.blockGap))
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = uiState.appLabel ?: stringResource(R.string.application_detail_unknown_app),
                    style = NezhaTheme.typography.title.copy(color = palette.textPrimary),
                )
                Spacer(modifier = Modifier.height(NezhaDimens.tightGap))
                BasicText(
                    text = uiState.packageName.orEmpty(),
                    style = NezhaTheme.typography.mono.copy(color = palette.textSecondary),
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(uiState: ApplicationDetailUiState) {
    val palette = NezhaTheme.palette
    NezhaSurfaceCard {
        BasicText(
            text = stringResource(R.string.application_detail_permissions_title),
            style = NezhaTheme.typography.title.copy(color = palette.textPrimary),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        when {
            !uiState.isPermissionDataAvailable -> BasicText(
                text = stringResource(R.string.application_detail_permissions_unknown),
                style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
            )

            uiState.permissions.isEmpty() -> BasicText(
                text = stringResource(R.string.application_detail_permissions_empty),
                style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
            )

            else -> PermissionList(uiState = uiState)
        }
    }
}

@Composable
private fun PermissionList(uiState: ApplicationDetailUiState) {
    val palette = NezhaTheme.palette
    BasicText(
        text = stringResource(
            R.string.application_detail_permissions_summary,
            uiState.dangerousGrantedCount,
            uiState.dangerousTotalCount,
        ),
        style = NezhaTheme.typography.caption.copy(
            color = if (uiState.dangerousGrantedCount > 0) palette.brand else palette.textSecondary,
        ),
    )
    Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
    uiState.permissions.forEach { permission ->
        PermissionRow(permission = permission)
    }
    Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
    BasicText(
        text = stringResource(R.string.application_detail_permissions_note),
        style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
    )
}

/**
 * 一项权限。
 *
 * 第二行是**平台标识**而不是又一句自然语言：用户拿着 `android.permission.CAMERA`
 * 才能在系统设置里对上号，也才能去别处核实本应用的说法。
 */
@Composable
private fun PermissionRow(permission: ApplicationPermission) {
    val palette = NezhaTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = NezhaDimens.tightGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = permission.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = NezhaTheme.typography.body.copy(color = palette.textPrimary),
            )
            BasicText(
                text = stringResource(
                    if (permission.isDangerous) {
                        R.string.application_detail_permission_name_runtime
                    } else {
                        R.string.application_detail_permission_name
                    },
                    permission.name,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = NezhaTheme.typography.mono.copy(color = palette.textSecondary),
            )
        }
        Spacer(modifier = Modifier.width(NezhaDimens.blockGap))
        BasicText(
            text = stringResource(
                if (permission.isGranted) {
                    R.string.application_detail_permission_granted
                } else {
                    R.string.application_detail_permission_denied
                },
            ),
            style = NezhaTheme.typography.label.copy(
                color = if (permission.isDangerous && permission.isGranted) palette.brand else palette.textSecondary,
            ),
        )
    }
}

@Composable
private fun DomainCard(uiState: ApplicationDetailUiState) {
    val palette = NezhaTheme.palette
    NezhaSurfaceCard {
        BasicText(
            text = stringResource(R.string.application_detail_domains_title),
            style = NezhaTheme.typography.title.copy(color = palette.textPrimary),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        BasicText(
            text = stringResource(
                R.string.application_detail_domain_counts,
                uiState.totalObserved,
                uiState.totalBlocked,
            ),
            style = NezhaTheme.typography.caption.copy(
                color = if (uiState.totalBlocked > 0) palette.brand else palette.textSecondary,
            ),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        if (uiState.domains.isEmpty()) {
            BasicText(
                text = stringResource(R.string.application_detail_domains_empty),
                style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
            )
        } else {
            uiState.domains.forEach { domain -> DomainRow(domain = domain) }
            if (uiState.hiddenDomainCount > 0) {
                Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
                BasicText(
                    text = stringResource(R.string.application_detail_more, uiState.hiddenDomainCount),
                    style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
                )
            }
        }
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        BasicText(
            text = stringResource(R.string.application_detail_note),
            style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
        )
    }
}

@Composable
private fun DomainRow(domain: DomainUsage) {
    val palette = NezhaTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = NezhaDimens.tightGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = domain.host,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = NezhaTheme.typography.mono.copy(color = palette.textPrimary),
        )
        Spacer(modifier = Modifier.width(NezhaDimens.blockGap))
        BasicText(
            text = stringResource(
                R.string.application_detail_domain_counts,
                domain.observed,
                domain.blocked,
            ),
            style = NezhaTheme.typography.caption.copy(
                color = if (domain.blocked > 0) palette.brand else palette.textSecondary,
            ),
        )
    }
}

@Preview(name = "应用详情 · 明暗对照", showBackground = true, widthDp = 800, heightDp = 1400)
@Composable
private fun ApplicationDetailPreview() {
    NezhaThemePreview {
        NezhaDataPreviewHost {
            ApplicationDetailContent(
                uiState = ApplicationDetailUiState(
                    packageName = "com.example.browser",
                    appLabel = "示例浏览器",
                    permissions = PREVIEW_PERMISSIONS,
                    isPermissionDataAvailable = true,
                    domains = listOf(
                        DomainUsage(host = "ads.example.com", observed = 12, blocked = 12),
                        DomainUsage(host = "cdn.example.net", observed = 8, blocked = 0),
                    ),
                    totalObserved = 20,
                    totalBlocked = 12,
                    isLoading = false,
                ),
            )
        }
    }
}

@Preview(name = "应用详情 · 未选择应用", showBackground = true, widthDp = 412, heightDp = 720)
@Composable
private fun ApplicationDetailEmptyPreview() {
    NezhaThemePreview {
        ApplicationDetailContent(uiState = ApplicationDetailUiState(isLoading = false))
    }
}

private val PREVIEW_PERMISSIONS = listOf(
    ApplicationPermission("android.permission.CAMERA", "相机", isDangerous = true, isGranted = true),
    ApplicationPermission(
        "android.permission.ACCESS_FINE_LOCATION",
        "确切位置",
        isDangerous = true,
        isGranted = true,
    ),
    ApplicationPermission("android.permission.READ_CONTACTS", "通讯录", isDangerous = true, isGranted = false),
    ApplicationPermission("android.permission.INTERNET", "网络访问", isDangerous = false, isGranted = true),
)
