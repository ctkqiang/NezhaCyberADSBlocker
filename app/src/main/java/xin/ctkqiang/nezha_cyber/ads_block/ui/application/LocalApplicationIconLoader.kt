package xin.ctkqiang.nezha_cyber.ads_block.ui.application

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 应用图标端口的注入点。
 *
 * 与其它界面层端口一样用 staticCompositionLocalOf：图标适配器在组合期内不会变，
 * 用动态 CompositionLocal 只会为每个读取方增加一层订阅开销，换不来任何东西。
 */
val LocalApplicationIconLoader = staticCompositionLocalOf<ApplicationIconLoader> {
    error("LocalApplicationIconLoader 未提供：请在组合根补上 CompositionLocalProvider")
}
