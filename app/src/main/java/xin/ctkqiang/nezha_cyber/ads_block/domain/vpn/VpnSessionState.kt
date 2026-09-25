package xin.ctkqiang.nezha_cyber.ads_block.domain.vpn

import java.time.Instant

/**
 * VPN 会话状态。
 *
 * 用封闭类型而不是布尔开关：启动中与停止中必须能被界面区分（主按钮需要禁用并给出进度反馈），
 * 失败也必须携带原因，否则界面只能说「出错了」，无法解释给用户。
 */
sealed interface VpnSessionState {
    data object Stopped : VpnSessionState

    data object Starting : VpnSessionState

    data class Running(val since: Instant) : VpnSessionState

    data object Stopping : VpnSessionState

    data class Failed(val reason: VpnFailureReason) : VpnSessionState

    /** 是否处于「已开启」或过渡态，供界面统一切换主按钮的语义。 */
    val isActive: Boolean
        get() = when (this) {
            Stopped, is Failed -> false
            Starting, is Running, Stopping -> true
        }
}
