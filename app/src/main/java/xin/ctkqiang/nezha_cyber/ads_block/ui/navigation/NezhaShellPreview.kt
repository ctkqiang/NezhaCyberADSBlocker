package xin.ctkqiang.nezha_cyber.ads_block.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import java.time.Instant
import xin.ctkqiang.nezha_cyber.ads_block.domain.vpn.VpnSessionState
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.NezhaDataPreviewHost
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme
import xin.ctkqiang.nezha_cyber.ads_block.ui.vpn.NezhaVpnPreviewHost

/**
 * 外壳的设计预览。
 *
 * 本工程没有可用的模拟器，界面观感无法靠运行确认，因此这里按 412×892 的真实手机比例把外壳
 * 渲染到 Android Studio 的预览面板：液态玻璃底栏、分段控件、明暗两套配色都能直接对照查看。
 *
 * 独立成文件而不是塞回 NezhaNavigationShell，是为了让这些预览与真实界面调用同一个
 * NezhaNavigationShellContent，走同一条渲染路径，避免预览与实际界面各自漂移。
 * 外壳包含首页，因此必须经过 NezhaVpnPreviewHost 提供端口替身。
 */
@Preview(name = "外壳 · 首页 · 未保护", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun ShellHomeIdlePreview() {
    NezhaTheme(darkTheme = false) {
        NezhaVpnPreviewHost(state = VpnSessionState.Stopped) {
            NezhaNavigationShellContent(
                uiState = NezhaShellUiState(NezhaTab.Home, NezhaTab.Home.defaultSection),
                onIntent = {},
            )
        }
    }
}

@Preview(name = "外壳 · 首页 · 已保护 · 深色", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun ShellHomeProtectedDarkPreview() {
    NezhaTheme(darkTheme = true) {
        NezhaVpnPreviewHost(state = VpnSessionState.Running(Instant.EPOCH)) {
            NezhaNavigationShellContent(
                uiState = NezhaShellUiState(NezhaTab.Home, NezhaTab.Home.defaultSection),
                onIntent = {},
            )
        }
    }
}

@Preview(name = "外壳 · 应用标签 · 分段控件", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun ShellApplicationsPreview() {
    NezhaTheme(darkTheme = false) {
        NezhaVpnPreviewHost(state = VpnSessionState.Stopped) {
            NezhaDataPreviewHost {
                NezhaNavigationShellContent(
                    uiState = NezhaShellUiState(NezhaTab.Applications, NezhaSection.ApkAnalysis),
                    onIntent = {},
                )
            }
        }
    }
}
