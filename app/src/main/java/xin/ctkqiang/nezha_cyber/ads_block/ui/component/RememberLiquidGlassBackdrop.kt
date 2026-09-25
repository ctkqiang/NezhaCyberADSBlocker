package xin.ctkqiang.nezha_cyber.ads_block.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.rememberGraphicsLayer

/**
 * 创建一块供玻璃取样的背景层。
 *
 * 必须在内容层与玻璃层的**共同祖先**里调用，然后把返回值分别交给
 * [liquidGlassSource]（加在内容层上）与 [LiquidGlassSpec.backdrop]（交给玻璃）。
 *
 * `rememberGraphicsLayer` 的实例在重组之间保持同一个图形层，因此不会每帧重新分配显存。
 *
 * 这里创建两个图层而非一个：原因写在 `LiquidGlassBackdrop` 的类注释里——单图层在同一帧内
 * 既要清晰上屏又要模糊取样，`renderEffect` 会互相污染，导致整屏被糊。
 */
@Composable
fun rememberLiquidGlassBackdrop(): LiquidGlassBackdrop {
    val sourceLayer = rememberGraphicsLayer()
    val blurLayer = rememberGraphicsLayer()
    return remember(sourceLayer, blurLayer) { LiquidGlassBackdrop(sourceLayer, blurLayer) }
}
