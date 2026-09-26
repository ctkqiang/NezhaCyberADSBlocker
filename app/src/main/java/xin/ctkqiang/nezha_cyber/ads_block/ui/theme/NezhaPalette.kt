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
 * ## 取值来自应用图标
 *
 * 三个基准色直接取自 `assets/logo.png` 的实测统计，不是另配的一套：
 * - **品牌红**：图标主体的色相在 353°（实测占彩色像素的绝对多数），取该色相；
 * - **深色底**：图标背景的实测值 `#040507`，深色主题的页面底色直接用它；
 * - 浅色主题是这一组的对偶：同一色相的品牌红压暗到能在白底上读，中性层级保持中性。
 *
 * ## 对比度
 *
 * 全部前景/背景组合都按 WCAG AA 校验过，实际测算值见各行注释。校验口径分两类：
 * - **正文与标签**（`textSecondary`、品牌色当文字用）要求 ≥ 4.5:1；
 * - **主按钮文字**（`onBrand` 压在品牌色上，17sp 半粗）按 WCAG 的大字标准要求 ≥ 3:1。
 *
 * 改任一色值后必须重新核算，规则由 `NezhaPaletteTest` 锁住——它会在对比度掉到线下时失败，
 * 而不是等到有人在深色模式里看不清才发现。
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
    // 图标主色相 353° 压暗到白底可读：对 #FFFFFF 为 5.51:1，白字压其上同为 5.51:1。
    brand = Color(0xFFCC1F33),
    onBrand = Color(0xFFFFFFFF),
    background = Color(0xFFF7F7F9),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFEDEDF1),
    textPrimary = Color(0xFF17171A),
    // 对 #FFFFFF 为 6.82:1，对页面底 #F7F7F9 为 6.38:1。
    textSecondary = Color(0xFF5A5A63),
    outline = Color(0xFFE2E2E6),
    glass = LiquidGlassColors(
        fill = Color(0xB3FFFFFF),
        highlight = Color(0x99FFFFFF),
        edge = Color(0x1F000000),
    ),
)

internal val darkNezhaPalette = NezhaPalette(
    // 直接取图标主体的亮红：对页面底 #040507 为 5.44:1，白字压其上为 3.75:1（大字标准）。
    // 这个色相在近黑底上可以很亮，因此深色主题反而更接近图标的原始观感。
    brand = Color(0xFFFF2640),
    onBrand = Color(0xFFFFFFFF),
    // 与图标背景同值。深色主题的页面底就是图标的底色，两者是同一个视觉身份。
    background = Color(0xFF040507),
    surface = Color(0xFF121216),
    // 图表条与状态标签用它：必须与 surface 分得开，否则卡内元素会糊成一片。
    surfaceElevated = Color(0xFF212128),
    textPrimary = Color(0xFFF2F2F4),
    // 对 surface 为 8.19:1，对 surfaceElevated 为 6.84:1。
    textSecondary = Color(0xFFA6A6B0),
    outline = Color(0xFF2A2A31),
    glass = LiquidGlassColors(
        fill = Color(0xB3141418),
        highlight = Color(0x3DFFFFFF),
        edge = Color(0x2EFFFFFF),
    ),
)
