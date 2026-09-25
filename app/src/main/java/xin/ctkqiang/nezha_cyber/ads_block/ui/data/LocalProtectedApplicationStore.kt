package xin.ctkqiang.nezha_cyber.ads_block.ui.data

import androidx.compose.runtime.staticCompositionLocalOf
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.ProtectedApplicationStore

/**
 * 受保护应用集合端口的注入点。
 *
 * 这个集合直接决定隧道路由，因此它的读取方只有两处：界面（展示与修改）与服务（建立隧道时应用）。
 * 两者共享同一份实现，避免出现「界面显示已保护但隧道没接管」这种不一致。
 */
val LocalProtectedApplicationStore = staticCompositionLocalOf<ProtectedApplicationStore> {
    error("LocalProtectedApplicationStore 未提供：请在组合根补上 CompositionLocalProvider")
}
