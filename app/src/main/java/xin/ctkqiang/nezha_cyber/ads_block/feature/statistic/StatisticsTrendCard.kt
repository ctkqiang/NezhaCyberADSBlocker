package xin.ctkqiang.nezha_cyber.ads_block.feature.statistic

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import xin.ctkqiang.nezha_cyber.ads_block.R
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaSurfaceCard
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaTrendBucket
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaTrendChart
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme

/**
 * 最近窗口的查询趋势。
 *
 * 样本不足时显示一句话而不是一张空图：全零的图会被读成「最近没有流量」，而事实是
 * 「样本太少」，这两个结论对用户恰好相反（工程规则第 32 节）。因此空态这句话不是兜底文案，
 * 而是这张图的一半语义。
 *
 * 说明文字里必须同时给出「覆盖多少条观测」与「分成多少段」：横轴是等分而非固定时长，
 * 不给这两个数，读者无从判断一根柱子代表多长一段时间。
 */
@Composable
internal fun TrendCard(uiState: StatisticsUiState) {
    val palette = NezhaTheme.palette
    NezhaSurfaceCard {
        BasicText(
            text = stringResource(R.string.statistics_trend_title),
            style = NezhaTheme.typography.title.copy(color = palette.textPrimary),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        if (uiState.trafficTrend.isEmpty()) {
            BasicText(
                text = stringResource(R.string.statistics_trend_empty),
                style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
            )
        } else {
            NezhaTrendChart(
                buckets = uiState.trafficTrend.map { bucket ->
                    NezhaTrendBucket(highlighted = bucket.blocked, rest = bucket.relayed)
                },
            )
            Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
            TrafficLegend()
            Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
            BasicText(
                text = stringResource(
                    R.string.statistics_trend_note,
                    uiState.windowObservationCount,
                    uiState.trafficTrend.size,
                ),
                style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
            )
        }
    }
}
