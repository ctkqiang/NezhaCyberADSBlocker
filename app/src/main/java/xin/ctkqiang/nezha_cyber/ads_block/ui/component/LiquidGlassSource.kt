package xin.ctkqiang.nezha_cyber.ads_block.ui.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize

/**
 * 标记「这一层内容会被玻璃取样」。
 *
 * 加在内容层的最外层。它做三件事：上报自己的窗口坐标（玻璃据此对齐）、把内容记录进
 * [LiquidGlassBackdrop] 的图形层、然后按原样绘出这一层。
 *
 * [background] 必须由调用方给出，且必须是这一层**实际的底色**。原因是 `Modifier.background`
 * 由外层节点绘制、发生在记录之前，因此不会被记录进图形层——若不自铺这一层，玻璃取到的样本
 * 在内容没画满的地方就是透明的，模糊出来是一片空洞。
 *
 * 记录完成后画的是图形层而不是再调一次 `drawContent()`：这是 Compose 里使用图形层的既定写法，
 * 内容只被绘制一次，玻璃那一侧拿到的就是同一份绘制结果。
 */
fun Modifier.liquidGlassSource(backdrop: LiquidGlassBackdrop, background: Color): Modifier = this
    .onGloballyPositioned { coordinates -> backdrop.reportSourcePosition(coordinates) }
    .drawWithContent {
        val scope = this
        backdrop.record(
            scope = scope,
            sizeInPixels = IntSize(
                width = scope.size.width.toInt(),
                height = scope.size.height.toInt(),
            ),
            draw = {
                drawRect(color = background)
                scope.drawContent()
            },
        )
        backdrop.drawSource(scope)
    }
