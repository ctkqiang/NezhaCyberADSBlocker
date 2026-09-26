package xin.ctkqiang.nezha_cyber.ads_block.feature.statistic

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import xin.ctkqiang.nezha_cyber.ads_block.R
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.FilteringStatistics
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaProportion
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaProportionRing
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaSurfaceCard
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme

/** 百分比换算用的比例基数。写成具名常量而不是裸 100（工程规则第 42.4 节）。 */
private const val PERCENT_SCALE = 100L

/**
 * 拦截占比环。
 *
 * 单独成文件而不是留在统计页里：统计页已经承载了整体读数、规则数、排行榜、按应用与清除
 * 五块内容，再往里塞图表会让那个文件同时承担「页面编排」与「单块渲染」两件事。
 *
 * 口径是**跨会话累计**，与另外两张只覆盖最近窗口的图不同，因此说明文字必须写出自己的口径——
 * 三张图在同一页，读者一定会横向比较，而口径不同的图放在一起比较正是最容易得出错误结论的地方。
 */
@Composable
internal fun ProportionCard(statistics: FilteringStatistics) {
    val palette = NezhaTheme.palette
    NezhaSurfaceCard {
        BasicText(
            text = stringResource(R.string.statistics_proportion_title),
            style = NezhaTheme.typography.title.copy(color = palette.textPrimary),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        NezhaProportionRing(
            proportion = NezhaProportion(
                highlighted = statistics.blocked,
                rest = statistics.relayed,
                centerValue = stringResource(R.string.statistics_percent_value, blockedPercent(statistics)),
                centerCaption = stringResource(R.string.statistics_proportion_center_caption),
                highlightedLabel = stringResource(R.string.statistics_legend_blocked),
                restLabel = stringResource(R.string.statistics_legend_relayed),
            ),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        BasicText(
            text = stringResource(R.string.statistics_proportion_note),
            style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
        )
    }
}

/** 拦截率的整数百分比。观测数为 0 时给 0：不显示 NaN，也不对空数据给出任何结论。 */
private fun blockedPercent(statistics: FilteringStatistics): Int =
    if (statistics.observed <= 0L) 0 else (statistics.blocked * PERCENT_SCALE / statistics.observed).toInt()
