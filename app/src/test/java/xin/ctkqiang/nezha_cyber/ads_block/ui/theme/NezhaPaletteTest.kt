package xin.ctkqiang.nezha_cyber.ads_block.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Test

/** 正文与标签的门槛。WCAG AA 对普通字号的要求。 */
private const val MIN_TEXT_CONTRAST = 4.5

/**
 * 大字门槛。
 *
 * 主按钮文字是 17sp 半粗，按 WCAG 的「大字」口径只要求 3:1。这一档单独写出来而不是统一
 * 用 4.5：把按钮文字也按 4.5 卡，会逼出一个比图标原始红暗得多的品牌色，而深色主题下那正好
 * 与图标失去联系。
 */
private const val MIN_LARGE_TEXT_CONTRAST = 3.0

private const val LUMINANCE_OFFSET = 0.05

private const val GAMMA_THRESHOLD = 0.04045

private const val GAMMA_LOW_DIVISOR = 12.92

private const val GAMMA_SCALE = 1.055

private const val GAMMA_OFFSET = 0.055

private const val GAMMA_EXPONENT = 2.4

private const val WEIGHT_RED = 0.2126

private const val WEIGHT_GREEN = 0.7152

private const val WEIGHT_BLUE = 0.0722

/**
 * 调色板的对比度契约。
 *
 * 规则第 0.2 节要求明暗两套主题都可用，而「可用」里最容易被破坏的一条就是可读性：
 * 改一个色值不会让任何测试变红，只会让文字在深色模式下变得难认。这个测试把口径写死，
 * 让改色的人在提交前就失败，而不是等用户在夜间看不清才发现。
 */
class NezhaPaletteTest {
    @Test
    fun `浅色主题的正文与品牌色满足 AA`() {
        assertPaletteIsReadable(palette = lightNezhaPalette, name = "浅色")
    }

    @Test
    fun `深色主题的正文与品牌色满足 AA`() {
        assertPaletteIsReadable(palette = darkNezhaPalette, name = "深色")
    }

    /** 页面底色与窗口底色必须一致，否则启动瞬间会闪一层色差。 */
    @Test
    fun `两套主题的卡片面与页面底面不重合`() {
        assertDistinct(lightNezhaPalette, "浅色")
        assertDistinct(darkNezhaPalette, "深色")
    }

    private fun assertPaletteIsReadable(palette: NezhaPalette, name: String) {
        assertContrast(name, "正文/页面底", palette.textPrimary, palette.background, MIN_TEXT_CONTRAST)
        assertContrast(name, "正文/卡片", palette.textPrimary, palette.surface, MIN_TEXT_CONTRAST)
        assertContrast(name, "次要文本/页面底", palette.textSecondary, palette.background, MIN_TEXT_CONTRAST)
        assertContrast(name, "次要文本/卡片", palette.textSecondary, palette.surface, MIN_TEXT_CONTRAST)
        assertContrast(name, "次要文本/浮起面", palette.textSecondary, palette.surfaceElevated, MIN_TEXT_CONTRAST)
        assertContrast(name, "品牌色/页面底", palette.brand, palette.background, MIN_TEXT_CONTRAST)
        assertContrast(name, "品牌色/卡片", palette.brand, palette.surface, MIN_TEXT_CONTRAST)
        assertContrast(name, "按钮文字/品牌色", palette.onBrand, palette.brand, MIN_LARGE_TEXT_CONTRAST)
    }

    private fun assertDistinct(palette: NezhaPalette, name: String) {
        assertTrue(
            "$name 主题的卡片面与页面底同为 ${palette.surface}，卡片会看不出边界",
            palette.surface != palette.background,
        )
    }

    private fun assertContrast(name: String, pair: String, foreground: Color, background: Color, minimum: Double) {
        val ratio = contrastRatio(foreground, background)
        assertTrue(
            "$name 主题「$pair」对比度 ${(ratio * 100).toInt() / 100.0}，低于要求的 $minimum",
            ratio >= minimum,
        )
    }
}

/** WCAG 2.1 相对亮度。 */
private fun relativeLuminance(color: Color): Double =
    WEIGHT_RED * linearize(color.red) + WEIGHT_GREEN * linearize(color.green) + WEIGHT_BLUE * linearize(color.blue)

private fun linearize(channel: Float): Double {
    val value = channel.toDouble()
    return if (value <= GAMMA_THRESHOLD) {
        value / GAMMA_LOW_DIVISOR
    } else {
        ((value + GAMMA_OFFSET) / GAMMA_SCALE).pow(GAMMA_EXPONENT)
    }
}

/** WCAG 2.1 对比度。与顺序无关，因此取较亮者为分子。 */
private fun contrastRatio(foreground: Color, background: Color): Double {
    val first = relativeLuminance(foreground)
    val second = relativeLuminance(background)
    val lighter = maxOf(first, second)
    val darker = minOf(first, second)
    return (lighter + LUMINANCE_OFFSET) / (darker + LUMINANCE_OFFSET)
}
