package xin.ctkqiang.nezha_cyber.ads_block.ui.vpn

import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import xin.ctkqiang.nezha_cyber.ads_block.domain.vpn.VpnController
import xin.ctkqiang.nezha_cyber.ads_block.domain.vpn.VpnSessionState
import xin.ctkqiang.nezha_cyber.ads_block.domain.vpn.VpnStartResult

/**
 * 预览与界面测试用的 [VpnController] 替身。
 *
 * 组合根注入的两个端口故意没有默认值，缺失会立刻报错；因此凡是脱离组合根渲染界面（IDE 预览、
 * 界面测试）的地方都必须显式提供替身。这个替身只做一件事：把给定状态原样暴露出去，
 * 不做任何平台交互。
 */
internal class PreviewVpnController(state: VpnSessionState = VpnSessionState.Stopped) : VpnController {
    private val mutableSession = MutableStateFlow(state)

    override val session: StateFlow<VpnSessionState> = mutableSession

    override suspend fun isAuthorized(): Boolean = true

    override suspend fun start(): VpnStartResult {
        mutableSession.value = VpnSessionState.Running(Instant.now())
        return VpnStartResult.Started
    }

    override suspend fun stop() {
        mutableSession.value = VpnSessionState.Stopped
    }
}
