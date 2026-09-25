package xin.ctkqiang.nezha_cyber.ads_block.domain.vpn

import kotlinx.coroutines.flow.StateFlow

/**
 * 端口：VPN 会话的控制与观测。
 *
 * 只暴露领域概念，不出现 Context、Intent、VpnService 或 PackageManager 等平台类型（第 38.3 节）。
 * 拉起系统授权对话框属于平台交互，由界面层通过 `ui.vpn` 的授权桥完成，本端口不负责。
 */
interface VpnController {
    /**
     * 当前会话状态。
     *
     * 这是状态端口而不是数据流，因此用 StateFlow 而非冷流（第 37.5 节）：界面在任何时刻都需要
     * 立刻拿到当前值，冷流在这里只会增加一次多余的订阅与初始订阅延迟。
     */
    val session: StateFlow<VpnSessionState>

    /** 是否已获得系统 VPN 授权。 */
    suspend fun isAuthorized(): Boolean

    /** 请求建立隧道。 */
    suspend fun start(): VpnStartResult

    /** 停止隧道并释放占用的描述符。 */
    suspend fun stop()
}
