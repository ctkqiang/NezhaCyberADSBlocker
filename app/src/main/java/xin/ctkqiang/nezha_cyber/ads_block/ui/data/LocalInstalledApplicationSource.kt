package xin.ctkqiang.nezha_cyber.ads_block.ui.data

import androidx.compose.runtime.staticCompositionLocalOf
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.InstalledApplicationSource

/**
 * 已安装应用来源端口的注入点。
 *
 * 界面只依赖领域端口，`PackageManager` 与包可见性全部留在适配器里（工程规则第 38.2 节）。
 */
val LocalInstalledApplicationSource = staticCompositionLocalOf<InstalledApplicationSource> {
    error("LocalInstalledApplicationSource 未提供：请在组合根补上 CompositionLocalProvider")
}
