package xin.ctkqiang.nezha_cyber.ads_block.ui.vpn

import androidx.compose.runtime.staticCompositionLocalOf
import xin.ctkqiang.nezha_cyber.ads_block.domain.vpn.VpnController

/**
 * VPN 控制端口的组合根注入点。
 *
 * 接口定义在 domain，实现（`AndroidVpnController`）由 MainActivity 构造后从这里下发。
 * 界面层因此只依赖领域端口，不依赖任何 Android 网络 API（第 38.1、38.2 节）。
 *
 * 故意不提供默认值：缺失说明组合根没有完成装配，属于开发期错误，必须立刻失败而不是
 * 退化成一个"看起来能用但没有效果"的空实现。
 */
val LocalVpnController = staticCompositionLocalOf<VpnController> {
    error("LocalVpnController 未提供：请在组合根（MainActivity）用 NezhaTheme 之外再包一层 CompositionLocalProvider")
}
