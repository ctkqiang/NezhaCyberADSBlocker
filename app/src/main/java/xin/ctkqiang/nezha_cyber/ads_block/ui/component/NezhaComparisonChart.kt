package xin.ctkqiang.nezha_cyber.ads_block.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import xin.ctkqiang.nezha_cyber.ads_block.R
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaThemePreview

/**
 * 横向对比条：每一行一个主体，条长按**各行总量的最大值**归一。
 *
 * 归一化基准取当前这一组里的最大值，而不是固定的历史最大值：这张图的用途是「在同一批里
 * 谁更突出」，用组内最大值才能让差异始终可见。代价是同一份数据在不同批次里条长不同，
 * 因此图必须配一行说明当前的口径，不能让读者把条长当绝对值。
 *
 * 只用品牌色与中性色两档，不引入第三种颜色：本工程的视觉基调是极简（规则第 0.2 节），
 * 需要第三种颜色时说明这张图想表达的东西太多了。
 */
@Composable
fun NezhaComparisonChart(bars: List<NezhaComparisonBar>, modifier: Modifier = Modifier) {
    val maxTotal = bars.maxOfOrNull { bar -> bar.highlighted + bar.rest }?.coerceAtLeast(1L) ?: 1L
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NezhaDimens.chartRowGap),
    ) {
        bars.forEach { bar -> ComparisonBarRow(bar = bar, maxTotal = maxTotal) }
    }
}

@Composable
private fun ComparisonBarRow(bar: NezhaComparisonBar, maxTotal: Long) {
    val palette = NezhaTheme.palette
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = bar.label,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = NezhaTheme.typography.body.copy(color = palette.textPrimary),
            )
            Spacer(modifier = Modifier.width(NezhaDimens.blockGap))
            BasicText(
                text = bar.trailing,
                // 有被强调的量才用品牌色：全为 0 的行不该抢走同样多的注意力（与读数行同一规则）。
                style = NezhaTheme.typography.label.copy(
                    color = if (bar.highlighted > 0) palette.brand else palette.textSecondary,
                ),
            )
        }
        Spacer(modifier = Modifier.height(NezhaDimens.tightGap))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(NezhaDimens.chartBarHeight),
        ) {
            val corner = CornerRadius(NezhaDimens.chartBarCornerRadius.toPx())
            val filledWidth = (bar.highlighted + bar.rest).toFloat() / maxTotal * size.width
            val highlightedWidth = bar.highlighted.toFloat() / maxTotal * size.width
            if (filledWidth > 0f) {
                drawRoundRect(
                    color = palette.surfaceElevated,
                    size = Size(width = filledWidth, height = size.height),
                    cornerRadius = corner,
                )
            }
            if (highlightedWidth > 0f) {
                drawRoundRect(
                    color = palette.brand,
                    size = Size(width = highlightedWidth, height = size.height),
                    cornerRadius = corner,
                )
            }
        }
        bar.caption?.let { caption ->
            Spacer(modifier = Modifier.height(NezhaDimens.tightGap))
            BasicText(
                text = caption,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
            )
        }
    }
}

@Preview(name = "对比条 · 明暗对照", showBackground = true, widthDp = 420, heightDp = 360)
@Composable
private fun NezhaComparisonChartPreview() {
    NezhaThemePreview {
        NezhaSurfaceCard {
            NezhaComparisonChart(
                bars = listOf(
                    NezhaComparisonBar("示例浏览器", highlighted = 18, rest = 24, trailing = "18 / 42"),
                    NezhaComparisonBar("示例视频", highlighted = 4, rest = 17, trailing = "4 / 21"),
                    NezhaComparisonBar("示例购物", highlighted = 0, rest = 9, trailing = "0 / 9"),
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
