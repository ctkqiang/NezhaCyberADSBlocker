package xin.ctkqiang.nezha_cyber.ads_block.ui.data

import androidx.compose.runtime.staticCompositionLocalOf
import xin.ctkqiang.nezha_cyber.ads_block.domain.appearance.ThemePreferenceStore

/** 外观偏好端口的注入点。界面通过它读写主题选择，因此 ViewModel 里不出现平台类型。 */
val LocalThemePreferenceStore = staticCompositionLocalOf<ThemePreferenceStore> {
    error("LocalThemePreferenceStore 未提供：请在组合根补上 CompositionLocalProvider")
}
