package xin.ctkqiang.nezha_cyber.ads_block.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme

/**
 * 一行占满的占比条：总长恒等于整行宽度，内部按比例分成被强调与其余两段。
 *
 * 与 [NezhaComparisonChart] 的区别是归一化基准：那个按**组内最大值**归一，用来横向比较谁更大；
 * 这个按**自身总量**归一，用来在单行里看内部比例。两者不能互换——把单行的占比条放进对比图，
 * 所有条会一样长，比较就没了；反过来则会让每一行的总长随数据变化，阅读时找不到基准。
 *
 * 总量为 0 时画一条空轨道而不是不画：整列保持对齐，读者不会以为是渲染漏了一行。
 */
@Composable
fun NezhaRatioBar(highlighted: Long, rest: Long, modifier: Modifier = Modifier) {
    val palette = NezhaTheme.palette
    val total = highlighted + rest
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(NezhaDimens.ratioBarHeight),
    ) {
        val corner = CornerRadius(NezhaDimens.ratioBarCornerRadius.toPx())
        drawRoundRect(
            color = palette.surfaceElevated,
            size = Size(width = size.width, height = size.height),
            cornerRadius = corner,
        )
        if (total > 0L) {
            val highlightedWidth = highlighted.toFloat() / total.toFloat() * size.width
            if (highlightedWidth > 0f) {
                drawRoundRect(
                    color = palette.brand,
                    size = Size(width = highlightedWidth, height = size.height),
                    cornerRadius = corner,
                )
            }
        }
    }
}
