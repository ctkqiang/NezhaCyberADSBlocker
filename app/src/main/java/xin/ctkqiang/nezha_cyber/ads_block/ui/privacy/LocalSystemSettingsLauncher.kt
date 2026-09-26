package xin.ctkqiang.nezha_cyber.ads_block.ui.privacy

import androidx.compose.runtime.staticCompositionLocalOf

/** 系统隐私设置页跳转的注入点。 */
val LocalSystemSettingsLauncher = staticCompositionLocalOf<SystemSettingsLauncher> {
    error("LocalSystemSettingsLauncher 未提供：请在组合根补上 CompositionLocalProvider")
}
