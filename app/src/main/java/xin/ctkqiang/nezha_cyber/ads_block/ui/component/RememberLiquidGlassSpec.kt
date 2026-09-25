package xin.ctkqiang.nezha_cyber.ads_block.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme

/** 无法做背景模糊时玻璃底的不透明度。见 [rememberLiquidGlassSpec] 的说明。 */
private const val OPAQUE_FALLBACK_ALPHA = 0.94f

/**
 * 按当前设备能力组装一块玻璃。
 *
 * 集中在这里而不是让每个调用点各自判断版本：低版本没有 `RenderEffect`，玻璃必须换成**更不透明**
 * 的底色，否则文字会直接压在滚动的背景上，谁也读不清。把这套降级逻辑只写一次，
 * 调用点就只需描述「玻璃在哪」，不可能有人漏判。
 *
 * 传入 `null` 背景时同样走降级路径——没有可采样的内容层，模糊就无从谈起。
 */
@Composable
fun rememberLiquidGlassSpec(
    shape: Shape,
    backdrop: LiquidGlassBackdrop?,
    blurRadius: Dp = NezhaDimens.glassBlurRadius,
): LiquidGlassSpec {
    val palette = NezhaTheme.palette
    val canSampleBackdrop = backdrop != null && supportsLiquidGlassBlur()
    return remember(shape, backdrop, blurRadius, canSampleBackdrop, palette) {
        LiquidGlassSpec(
            shape = shape,
            colors = if (canSampleBackdrop) {
                palette.glass
            } else {
                palette.glass.copy(fill = palette.glass.fill.copy(alpha = OPAQUE_FALLBACK_ALPHA))
            },
            blurRadius = if (canSampleBackdrop) blurRadius else 0.dp,
            backdrop = backdrop,
        )
    }
}
