package xin.ctkqiang.nezha_cyber.ads_block.ui.component

import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.LiquidGlassColors
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens

/**
 * 一块玻璃的完整描述。
 *
 * 收成一个值而不是让 `liquidGlass` 收下七八个参数：这些值总是由调用点一起决定，
 * 散着传会让每个调用点重复一长串参数名，也让「这块玻璃长什么样」无法被单独传递或复用。
 *
 * [blurRadius] 为 0 或 [backdrop] 为 null 时不做背景模糊：玻璃退化为一层半透明底，
 * 调用方必须相应提高 [colors] 的不透明度，否则文字会落在不可控的背景上。
 * 低版本（无 `RenderEffect`）走的正是这条路径。
 */
data class LiquidGlassSpec(
    val shape: Shape,
    val colors: LiquidGlassColors,
    val edgeWidth: Dp = NezhaDimens.hairline,
    val blurRadius: Dp = 0.dp,
    val backdrop: LiquidGlassBackdrop? = null,
)
