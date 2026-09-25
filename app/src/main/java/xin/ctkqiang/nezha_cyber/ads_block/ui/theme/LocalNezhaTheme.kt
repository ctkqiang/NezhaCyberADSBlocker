package xin.ctkqiang.nezha_cyber.ads_block.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 配色的 CompositionLocal。
 *
 * 使用 static 变体：主题切换是低频事件，变化时整体重组比逐点失效更简单，代价可接受。
 * 默认值取浅色方案，使组件在 Theme 之外被单独渲染时也有确定行为。
 */
val LocalNezhaPalette = staticCompositionLocalOf { lightNezhaPalette }

/**
 * 字体层级的 CompositionLocal。
 *
 * 与配色分开存放，便于后续接入系统字号缩放时只替换这一项。
 */
val LocalNezhaTypography = staticCompositionLocalOf { nezhaTypography }
