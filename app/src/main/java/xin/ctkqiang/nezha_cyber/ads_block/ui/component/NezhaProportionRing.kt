package xin.ctkqiang.nezha_cyber.ads_block.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import xin.ctkqiang.nezha_cyber.ads_block.R
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaThemePreview

/** 从十二点方向开始画，符合读表习惯：先看到的是正上方那一小段。 */
private const val RING_START_ANGLE = -90f

private const val FULL_CIRCLE_DEGREES = 360f

/**
 * 占比环：用一段弧表示被强调部分占总量的比例，环心给出读数。
 *
 * 弧的两端用平头而不是圆头：圆头会让弧比真实比例长出约一个线宽，在一段只有 5% 的弧上
 * 那是接近一倍的高估。图表可以不精确，但不能系统性地夸大——尤其在这一页夸大的正是
 * 「拦了多少」这个用户最在意的数字。
 *
 * 底色环画满整圈，因此「空」的部分也有形，读者能一眼看出分母是整圆。
 */
@Composable
fun NezhaProportionRing(proportion: NezhaProportion, modifier: Modifier = Modifier) {
    val palette = NezhaTheme.palette
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(NezhaDimens.proportionRingSize),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = NezhaDimens.proportionRingStroke.toPx()
                val inset = strokeWidth / 2f
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val stroke = Stroke(width = strokeWidth)
                drawArc(
                    color = palette.surfaceElevated,
                    startAngle = RING_START_ANGLE,
                    sweepAngle = FULL_CIRCLE_DEGREES,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = stroke,
                )
                val sweep = proportion.highlightedFraction * FULL_CIRCLE_DEGREES
                if (sweep > 0f) {
                    drawArc(
                        color = palette.brand,
                        startAngle = RING_START_ANGLE,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = stroke,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BasicText(
                    text = proportion.centerValue,
                    style = NezhaTheme.typography.display.copy(color = palette.brand),
                )
                BasicText(
                    text = proportion.centerCaption,
                    style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
                )
            }
        }
        Spacer(modifier = Modifier.width(NezhaDimens.proportionRingGap))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NezhaDimens.blockGap),
        ) {
            NezhaChartLegendEntry(color = palette.brand, label = proportion.highlightedLabel)
            NezhaChartLegendEntry(color = palette.surfaceElevated, label = proportion.restLabel)
        }
    }
}

@Preview(name = "占比环 · 明暗对照", showBackground = true, widthDp = 420, heightDp = 260)
@Composable
private fun NezhaProportionRingPreview() {
    NezhaThemePreview {
        NezhaSurfaceCard {
            NezhaProportionRing(
                proportion = NezhaProportion(
                    highlighted = 96,
                    rest = 1_188,
                    centerValue = "7%",
                    centerCaption = "累计拦截占比",
                    highlightedLabel = "已拦截",
                    restLabel = "已放行",
                ),
            )
            Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
            BasicText(
                text = stringResource(R.string.statistics_proportion_note),
                style = NezhaTheme.typography.caption.copy(color = NezhaTheme.palette.textSecondary),
            )
        }
    }
}
