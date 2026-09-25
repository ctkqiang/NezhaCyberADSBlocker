package xin.ctkqiang.nezha_cyber.ads_block.ui.vpn

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import xin.ctkqiang.nezha_cyber.ads_block.domain.vpn.VpnSessionState

/** 预览环境里授权流程没有真实对话框，直接按「用户拒绝」返回，避免预览触发平台交互。 */
private val previewAuthorizationRequester = VpnAuthorizationRequester { onResult -> onResult(false) }

/**
 * 预览装配壳。
 *
 * 把 VPN 端口的替身集中在这一处，页面与外壳的 @Preview 只需包一层，不必各自重复写
 * CompositionLocalProvider，也就不会出现某个预览忘了补替身而崩溃。
 */
@Composable
internal fun NezhaVpnPreviewHost(state: VpnSessionState = VpnSessionState.Stopped, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalVpnController provides PreviewVpnController(state),
        LocalVpnAuthorizationRequester provides previewAuthorizationRequester,
        content = content,
    )
}
