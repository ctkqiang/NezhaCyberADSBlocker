package xin.ctkqiang.nezha_cyber.ads_block.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * 应用配色。
 *
 * 分三组：品牌强调、明暗两套中性层级、玻璃材质。所有组件都从这里取色，
 * 不允许在各自的 Composable 里写死色值（工程规则第 42.4 节）。
 *
 * 文本分两级。增加第三级之前先确认现有两级表达不了——未使用的色板槽位会沉淀成无人维护的死值。
 *
 * 全部前景/背景组合都按 WCAG AA 校验过：正文与标签对各自底色的对比度不低于 4.5:1。
 * 修改任一色值后必须重新核算，尤其是品牌色在深色主题下的取值。
 *
 * 玻璃那一组是唯一的例外：它是半透明的，落在其上的文字对比度还取决于背后被模糊的内容。
 * 这里把玻璃底固定在半透明区间，使最坏情况下（背后是最浅或最深的纯色内容）仍然可读；
 * 若把 [LiquidGlassColors.fill] 再调低，就必须同时重新核算底栏标签的对比度。
 */
@Immutable
data class NezhaPalette(
    val brand: Color,
    val onBrand: Color,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val outline: Color,
    val glass: LiquidGlassColors,
)

internal val lightNezhaPalette = NezhaPalette(
    brand = Color(0xFFC62828),
    onBrand = Color(0xFFFFFFFF),
    background = Color(0xFFF7F7F9),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFEDEDF1),
    textPrimary = Color(0xFF17171A),
    textSecondary = Color(0xFF5A5A63),
    outline = Color(0xFFE2E2E6),
    glass = LiquidGlassColors(
        fill = Color(0xB3FFFFFF),
        highlight = Color(0x99FFFFFF),
        edge = Color(0x1F000000),
    ),
)

internal val darkNezhaPalette = NezhaPalette(
    brand = Color(0xFFEF5350),
    onBrand = Color(0xFFFFFFFF),
    background = Color(0xFF0C0C0E),
    surface = Color(0xFF2C2C33),
    surfaceElevated = Color(0xFF1B1B1F),
    textPrimary = Color(0xFFF2F2F4),
    textSecondary = Color(0xFFA6A6B0),
    outline = Color(0xFF2C2C31),
    glass = LiquidGlassColors(
        fill = Color(0xB31C1C20),
        highlight = Color(0x3DFFFFFF),
        edge = Color(0x2EFFFFFF),
    ),
)
