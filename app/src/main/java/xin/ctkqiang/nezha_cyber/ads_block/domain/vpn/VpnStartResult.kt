package xin.ctkqiang.nezha_cyber.ads_block.domain.vpn

/**
 * VPN 启动的结果。
 *
 * 用封闭类型而非异常建模：授权被拒绝属于预期内的领域结果，不是缺陷（工程规则第 37.3 节）。
 * 界面据此区分三件事：已启动、需要拉系统授权、以及带着具体原因的失败。
 */
sealed interface VpnStartResult {
    data object Started : VpnStartResult

    data object PermissionDenied : VpnStartResult

    data class Failed(val reason: VpnFailureReason) : VpnStartResult
}
