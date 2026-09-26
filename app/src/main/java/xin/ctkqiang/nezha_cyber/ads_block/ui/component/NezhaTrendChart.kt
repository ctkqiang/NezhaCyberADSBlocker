package xin.ctkqiang.nezha_cyber.ads_block.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import xin.ctkqiang.nezha_cyber.ads_block.R
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaThemePreview

/**
 * 趋势柱状图：按时间分桶的查询量，每根柱子内部再分成被强调与其余两部分。
 *
 * 柱高按**本组最大值**归一，而不是按固定的历史峰值：最近窗口里的流量量级随时在变，
 * 用固定峰值会让大多数时候整张图贴地、什么也看不出来。代价是不同时刻的图不能直接比高度，
 * 因此调用方必须在说明里写清横轴与口径（哪一段、怎么分桶）。
 *
 * 图上不画坐标轴刻度。分桶的时间宽度取决于窗口跨度，写死刻度只会给出错误的信息；
 * 读者真正需要的是「哪一段更密」，那是柱高的相对关系，不是绝对值。
 */
@Composable
fun NezhaTrendChart(buckets: List<NezhaTrendBucket>, modifier: Modifier = Modifier) {
    val palette = NezhaTheme.palette
    val maxTotal = buckets.maxOfOrNull { bucket -> bucket.total }?.coerceAtLeast(1) ?: 1
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(NezhaDimens.trendChartHeight),
    ) {
        val count = buckets.size
        if (count == 0) return@Canvas
        val gap = NezhaDimens.trendBarGap.toPx()
        val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(1f)
        val corner = CornerRadius(NezhaDimens.trendBarCornerRadius.toPx())
        buckets.forEachIndexed { index, bucket ->
            val left = index * (barWidth + gap)
            val totalHeight = bucket.total.toFloat() / maxTotal * size.height
            val highlightedHeight = bucket.highlighted.toFloat() / maxTotal * size.height
            if (totalHeight > 0f) {
                drawRoundRect(
                    color = palette.surfaceElevated,
                    topLeft = Offset(x = left, y = size.height - totalHeight),
                    size = Size(width = barWidth, height = totalHeight),
                    cornerRadius = corner,
                )
            }
            if (highlightedHeight > 0f) {
                // 被强调的一段贴底：柱子的顶是总量，底是拦截量，扫一眼就能比出两行的高低。
                drawRoundRect(
                    color = palette.brand,
                    topLeft = Offset(x = left, y = size.height - highlightedHeight),
                    size = Size(width = barWidth, height = highlightedHeight),
                    cornerRadius = corner,
                )
            }
        }
    }
}

@Preview(name = "趋势柱 · 明暗对照", showBackground = true, widthDp = 420, heightDp = 240)
@Composable
private fun NezhaTrendChartPreview() {
    NezhaThemePreview {
        NezhaSurfaceCard {
            NezhaTrendChart(
                buckets = listOf(
                    NezhaTrendBucket(highlighted = 2, rest = 6),
                    NezhaTrendBucket(highlighted = 5, rest = 9),
                    NezhaTrendBucket(highlighted = 1, rest = 3),
                    NezhaTrendBucket(highlighted = 8, rest = 14),
                    NezhaTrendBucket(highlighted = 3, rest = 11),
                    NezhaTrendBucket(highlighted = 6, rest = 7),
                    NezhaTrendBucket(highlighted = 0, rest = 2),
                    NezhaTrendBucket(highlighted = 4, rest = 10),
                    NezhaTrendBucket(highlighted = 9, rest = 5),
                    NezhaTrendBucket(highlighted = 2, rest = 8),
                    NezhaTrendBucket(highlighted = 7, rest = 12),
                    NezhaTrendBucket(highlighted = 3, rest = 6),
                ),
            )
            Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
            Row(horizontalArrangement = Arrangement.spacedBy(NezhaDimens.blockGap)) {
                NezhaChartLegendEntry(
                    color = NezhaTheme.palette.brand,
                    label = stringResource(R.string.statistics_legend_blocked),
                )
                NezhaChartLegendEntry(
                    color = NezhaTheme.palette.surfaceElevated,
                    label = stringResource(R.string.statistics_legend_relayed),
                )
            }
        }
    }
}
