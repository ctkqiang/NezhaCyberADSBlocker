package xin.ctkqiang.nezha_cyber.ads_block.ui.analysis

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 文件选择桥的注入点，由组合根提供。
 *
 * 刻意不提供默认值：缺失说明装配没完成，必须立刻失败，而不是让「选择文件」按钮点了没反应。
 */
val LocalApkPicker = staticCompositionLocalOf<ApkPicker> {
    error("LocalApkPicker 未提供：请在组合根补上 CompositionLocalProvider")
}
