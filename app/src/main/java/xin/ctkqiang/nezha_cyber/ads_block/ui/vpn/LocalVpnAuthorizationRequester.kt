package xin.ctkqiang.nezha_cyber.ads_block.ui.vpn

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 授权请求桥的组合根注入点。
 *
 * 与 [LocalVpnController] 同理，不提供默认值：默认缺省会让授权流程静默失败，用户点按钮没有任何反应，
 * 这比直接报错更难排查。
 */
val LocalVpnAuthorizationRequester = staticCompositionLocalOf<VpnAuthorizationRequester> {
    error("LocalVpnAuthorizationRequester 未提供：请在组合根（MainActivity）完成装配")
}
