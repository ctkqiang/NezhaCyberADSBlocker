package xin.ctkqiang.nezha_cyber.ads_block.ui.data

import androidx.compose.runtime.staticCompositionLocalOf
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.ApplicationPermissionSource

/**
 * 应用权限端口的注入点。
 *
 * 读取方只有应用列表（危险权限摘要）与应用详情（完整清单）。这个端口是只读的，
 * 因此没有「界面改权限」这条路径——平台不允许普通应用改其它应用的授权状态。
 */
val LocalApplicationPermissionSource = staticCompositionLocalOf<ApplicationPermissionSource> {
    error("LocalApplicationPermissionSource 未提供：请在组合根补上 CompositionLocalProvider")
}
