package xin.ctkqiang.nezha_cyber.ads_block.feature.statistic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import xin.ctkqiang.nezha_cyber.ads_block.R
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaChartLegendEntry
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme

/**
 * 统计页三张图共用的图例。
 *
 * 单独成文件而不是各自内联一份：三张图用的是同一组含义（品牌色 = 已拦截，灰色 = 已放行），
 * 各写一份的话，改一处颜色或措辞就会让同一页里的图例互相矛盾——而图例自相矛盾的图表，
 * 比没有图例更糟。
 */
@Composable
internal fun TrafficLegend() {
    val palette = NezhaTheme.palette
    Row(horizontalArrangement = Arrangement.spacedBy(NezhaDimens.blockGap)) {
        NezhaChartLegendEntry(
            color = palette.brand,
            label = stringResource(R.string.statistics_legend_blocked),
        )
        NezhaChartLegendEntry(
            color = palette.surfaceElevated,
            label = stringResource(R.string.statistics_legend_relayed),
        )
    }
}
