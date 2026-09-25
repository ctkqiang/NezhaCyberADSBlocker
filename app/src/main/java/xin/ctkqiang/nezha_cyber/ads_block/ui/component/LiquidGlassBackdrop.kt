package xin.ctkqiang.nezha_cyber.ads_block.ui.component

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntSize

/**
 * 玻璃背后那一层内容。
 *
 * **真实背景模糊在 Android 上做得到**，前提是被模糊的内容由我们自己绘制：把内容记录进一个
 * 图形层，玻璃再以「裁剪 + 模糊」把它重画一遍。这全程发生在**绘制阶段**——不重新组合、
 * 不重新布局，代价是一次额外绘制，而不是把整个页面组合两遍。
 *
 * 做不到的是模糊**别的应用**的窗口：那需要系统级权限。因此本应用切到后台时，
 * 玻璃后面只会是本应用自己的内容——这是平台边界，不是实现偷懒。
 *
 * 位置不靠尺寸令牌推算，而是由两层各自上报窗口坐标后取差值：玻璃与内容层之间隔着内边距、
 * 系统栏插入与柔光预留，用令牌硬算一旦有一处对不上就会偏移，而偏移多少肉眼看不出来。
 * 内容层在前、玻璃层在后，同一帧里先后上报，因此绘制时拿到的差值一定是本帧的。
 *
 * ## 为什么用两个图层
 *
 * `renderEffect` 是 [GraphicsLayer] 的属性，而 `drawLayer` 把「绘制该层」的指令记进显示列表，
 * 真正套用效果发生在本帧合成阶段。如果清晰上屏与模糊取样共用**同一个** layer、仅在画之前把
 * `renderEffect` 一会儿设 null 一会儿设 BlurEffect，那么合成时 layer 上残留的是最后写入的值，
 * 两次绘制都会糊——症状是整张页面都被底栏的模糊效果波及，而底栏自己的图标文字反而清晰。
 *
 * 因此拆成两层：[sourceLayer] 永远不带效果，负责把内容清晰地画到屏幕上；[blurLayer] 只承载
 * 模糊效果，负责给玻璃取样。内容只录一次到 sourceLayer，blurLayer 通过 drawLayer 复制它，
 * 避免把 drawContent() 跑两遍。
 */
@Stable
class LiquidGlassBackdrop internal constructor(
    private val sourceLayer: GraphicsLayer,
    private val blurLayer: GraphicsLayer,
) {
    private var sourcePosition: Offset = Offset.Zero

    private var consumerPosition: Offset = Offset.Zero

    /**
     * 记录内容层。
     *
     * 调用必须落在 [DrawScope] 上那个 `record` 扩展，而不是 [GraphicsLayer] 上的同名成员函数：
     * 两者签名看着等价，运行时分派却完全不同。`DrawScope` 上的那个会被 `LayoutNodeDrawScope`
     * 覆写，并在块内保存/恢复 `drawNode`，因此块里的 `drawContent()` 才有节点可画；若直接调
     * `GraphicsLayer` 的成员函数，`drawNode` 为 null，`drawContent()` 会抛
     * 「Attempting to drawContent for a null node」，症状是底色被正常记录、页面内容却整片空白。
     *
     * 尺寸与坐标取自内容层自身，因此记录下来的内容与屏幕上看到的**逐像素对得上**——
     * 一旦改用「屏幕尺寸」之类的近似值，玻璃里的内容就会与真实内容错位。
     */
    internal fun record(scope: DrawScope, sizeInPixels: IntSize, draw: DrawScope.() -> Unit) {
        with(scope) {
            // 内容只录一次，sourceLayer 永远保持锐利。
            sourceLayer.record(sizeInPixels, draw)
            // blurLayer 复制 sourceLayer 的内容；它自己的 renderEffect 由 drawBlurred 单独设置，
            // 不会影响 sourceLayer 的清晰上屏。
            blurLayer.record(sizeInPixels) {
                drawLayer(sourceLayer)
            }
        }
    }

    /**
     * 按原样绘制内容层。
     *
     * sourceLayer 永远不带 renderEffect，因此这一层始终锐利。模糊效果只活在 blurLayer 上，
     * 不会污染这里的绘制。
     */
    internal fun drawSource(scope: DrawScope) {
        scope.drawLayer(sourceLayer)
    }

    /**
     * 以模糊方式重画内容层，供玻璃取样。
     *
     * [blurRadiusPx] 为 0 时不设效果：低版本没有 `RenderEffect`，与其传一个会被忽略的半径，
     * 不如明确地不做模糊，让调用方改用更不透明的底色兜底。
     *
     * 只在 blurLayer 上设置模糊，sourceLayer 不受影响——这是「内容清晰、玻璃后模糊」的关键。
     */
    internal fun drawBlurred(scope: DrawScope, blurRadiusPx: Float) {
        if (blurRadiusPx <= 0f) return
        blurLayer.renderEffect = BlurEffect(blurRadiusPx, blurRadiusPx, TileMode.Clamp)
        val offset = sourcePosition - consumerPosition
        scope.translate(top = offset.y, left = offset.x) {
            drawLayer(blurLayer)
        }
    }

    internal fun reportSourcePosition(coordinates: LayoutCoordinates) {
        sourcePosition = coordinates.positionInWindow()
    }

    internal fun reportConsumerPosition(coordinates: LayoutCoordinates) {
        consumerPosition = coordinates.positionInWindow()
    }
}
