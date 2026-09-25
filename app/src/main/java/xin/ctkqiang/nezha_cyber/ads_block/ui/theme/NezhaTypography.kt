package xin.ctkqiang.nezha_cyber.ads_block.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 字体层级。
 *
 * 只使用系统默认字族，不引入字体文件：层次靠字号与字重建立，而不是靠字形装饰。
 * 七档各有明确职责，新增层级前先确认现有档位表达不了：
 * hero 用于单个醒目的数字，display 用于页面主标题，title 用于区块标题，
 * body 用于正文，label 用于控件标签，caption 用于次要说明，
 * mono 用于需要按列对齐的流水。
 */
@Immutable
data class NezhaTypography(
    val hero: TextStyle,
    val display: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val mono: TextStyle,
)

internal val nezhaTypography = NezhaTypography(
    hero = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.8).sp,
    ),
    display = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.4).sp,
    ),
    title = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    body = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
    ),
    label = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.1.sp,
    ),
    caption = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    /**
     * 等宽正文。
     *
     * 实时流水里每一行的时间戳与域名必须**按列对齐**：比例字体下 1 与 8 的宽度不同，
     * 时间戳的冒号会左右晃，整列看起来是歪的，扫读时无法沿着一列往下看。
     * 这是等宽字体在本工程里唯一的用途，因此不额外提供多个字号档位。
     */
    mono = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
)
