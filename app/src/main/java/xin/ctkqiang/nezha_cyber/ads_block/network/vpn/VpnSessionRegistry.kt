package xin.ctkqiang.nezha_cyber.ads_block.network.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xin.ctkqiang.nezha_cyber.ads_block.domain.vpn.VpnSessionState

/**
 * VPN 会话状态的进程内持有者。
 *
 * 为什么允许这样一个可变状态：`VpnService` 由系统实例化，无法通过构造函数注入依赖，
 * 而会话状态只有它一个写入者。改用绑定服务能让依赖方向更干净，但会把启动路径变成异步并引入
 * 连接失败、重连与解绑时序三类分支，在当前阶段收益不足（工程规则第 35.15 节要求先说明取舍）。
 *
 * 因此约束收敛为两条，任何修改都必须同时满足：
 * 1. 写入者只有 [NezhaVpnService]；
 * 2. 读取者只通过 [state]，不直接持有可变引用。
 */
internal object VpnSessionRegistry {
    private val mutableState = MutableStateFlow<VpnSessionState>(VpnSessionState.Stopped)

    val state: StateFlow<VpnSessionState> = mutableState.asStateFlow()

    fun publish(state: VpnSessionState) {
        mutableState.value = state
    }
}
