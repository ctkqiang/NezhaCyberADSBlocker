package xin.ctkqiang.nezha_cyber.ads_block.ui.component

import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.onGloballyPositioned

/** 体内高光自上而下的衰减位置。 */
private const val SHEEN_FADE_STOP = 0.6f

/** 内缘折射自上而下的衰减位置。比体内高光更快消失，才能读成一条细边而不是一层雾。 */
private const val RIM_FADE_STOP = 0.45f

/**
 * 内缘折射的描边宽度倍数。
 *
 * 描边以路径为中心，取两倍宽再裁到路径内，外侧那一半被裁掉，留下的正好是一条贴着内缘的细线——
 * 这正是玻璃边缘把背景光「折射」进来的观感来源。直接描一条等宽边只会得到一个轮廓框。
 */
private const val RIM_STROKE_SPAN = 2f

/**
 * 液态玻璃。
 *
 * 绘制顺序就是光穿过一块玻璃的顺序，[spec] 的各字段含义见 `LiquidGlassSpec`：
 * 1. **背景取样**——把 [LiquidGlassSpec.backdrop] 记录的内容以模糊方式重画一遍，裁在路径内；
 * 2. **玻璃底**——半透明底色，决定整体明暗；
 * 3. **体内高光**——自上而下衰减，模拟光在体内散射；
 * 4. **内缘折射**——贴着内缘的一条渐变细线，上缘最亮；
 * 5. **轮廓细边**——让玻璃在任何背景上都有可辨认的边界。
 *
 * 外缘柔光与体内底阴影已移除：本工程 UI 不使用阴影，玻璃的浮起感仅由取样模糊与边缘线条承担。
 */
fun Modifier.liquidGlass(spec: LiquidGlassSpec): Modifier = this
    // 上报自身坐标：背景取样要按「内容层与玻璃层的实际位置差」对齐，而不是按尺寸令牌推算。
    .onGloballyPositioned { coordinates -> spec.backdrop?.reportConsumerPosition(coordinates) }
    .drawWithCache {
        val path = Path().apply {
            addOutline(spec.shape.createOutline(size, layoutDirection, this@drawWithCache))
        }
        val edgeWidthPx = spec.edgeWidth.toPx()
        val blurRadiusPx = spec.blurRadius.toPx()
        val fadeEnd = size.height
        onDrawBehind {
            clipPath(path) { spec.backdrop?.drawBlurred(scope = this, blurRadiusPx = blurRadiusPx) }
            drawPath(path = path, color = spec.colors.fill)
            clipPath(path) {
                drawRect(
                    brush = shearGradient(
                        from = spec.colors.highlight,
                        fadeStop = SHEEN_FADE_STOP,
                        endY = fadeEnd,
                    ),
                )
            }
            drawPath(path = path, color = spec.colors.edge, style = Stroke(width = edgeWidthPx))
            clipPath(path) {
                drawPath(
                    path = path,
                    brush = shearGradient(from = spec.colors.highlight, fadeStop = RIM_FADE_STOP, endY = fadeEnd),
                    style = Stroke(width = edgeWidthPx * RIM_STROKE_SPAN),
                )
            }
        }
    }

/** 自上而下由 [from] 渐隐到透明的竖向渐变。玻璃的高光与折射都是这个形状，只是衰减位置不同。 */
private fun shearGradient(from: Color, fadeStop: Float, endY: Float): Brush = Brush.verticalGradient(
    0f to from,
    fadeStop to Color.Transparent,
    startY = 0f,
    endY = endY,
)

/**
 * 本机能否真正做背景模糊。
 *
 * 取决于 `RenderEffect`，它在 API 31（Android 12）才出现。低于此版本时玻璃必须换用更不透明的
 * 底色，而不是假装模糊——假装的结果是文字压在滚动中的背景上，谁也读不清。
 * 降级逻辑集中在 `rememberLiquidGlassSpec` 里，调用点不需要自己判断版本。
 */
fun supportsLiquidGlassBlur(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
