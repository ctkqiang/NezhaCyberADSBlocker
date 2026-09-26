package xin.ctkqiang.nezha_cyber.ads_block.feature.statistic

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import xin.ctkqiang.nezha_cyber.ads_block.R
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaComparisonBar
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaComparisonChart
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaSurfaceCard
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme

/** 高频域名的分隔方式。域名之间用中点，避免与域名自身的点混淆。 */
private const val HOST_SEPARATOR = " · "

/**
 * 按应用读数（工程规则第 24 节的第二个统计维度）。
 *
 * 单独成一个文件而不是塞进统计页：这一块是「应用 → 它请求了什么」的下钻视图，
 * 与页面上其余的整体读数不是同一个概念，混在一起会让两者的边界在阅读时消失。
 *
 * 用对比条而不是纯文本行：这一块要回答的是「**哪个**应用在大量联网、其中多少被拦下来」，
 * 而三列数字需要用户自己在脑子里换算大小；条长把这件事直接给出来。排序仍由 ViewModel 决定，
 * 界面不再排一次——两处排序迟早会出现一处改了另一处没改。
 */
@Composable
internal fun ApplicationTrafficCard(summaries: List<ApplicationTrafficSummary>) {
    val palette = NezhaTheme.palette
    NezhaSurfaceCard {
        BasicText(
            text = stringResource(R.string.statistics_applications_title),
            style = NezhaTheme.typography.title.copy(color = palette.textPrimary),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        if (summaries.isEmpty()) {
            BasicText(
                text = stringResource(R.string.statistics_applications_empty),
                style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
            )
        } else {
            TrafficLegend()
            Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
            NezhaComparisonChart(bars = summaries.map { summary -> summary.toComparisonBar() })
            Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
            BasicText(
                text = stringResource(R.string.statistics_traffic_note),
                style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
            )
            Spacer(modifier = Modifier.height(NezhaDimens.tightGap))
            BasicText(
                text = stringResource(R.string.statistics_applications_note),
                style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
            )
        }
    }
}

/**
 * 转成对比条。
 *
 * 未归属到应用的组显示为「未知来源」而不是包名的空串，理由与观测列表一致：
 * 空标签在界面上看起来像是渲染坏了，而它其实是一个有确切含义的分组。
 */
@Composable
private fun ApplicationTrafficSummary.toComparisonBar(): NezhaComparisonBar = NezhaComparisonBar(
    label = label ?: stringResource(R.string.statistics_app_unknown),
    highlighted = blocked.toLong(),
    rest = relayed.toLong(),
    trailing = stringResource(R.string.statistics_app_bar_trailing, blocked, observed),
    caption = topHosts.takeIf { hosts -> hosts.isNotEmpty() }
        ?.let { hosts -> stringResource(R.string.statistics_app_hosts, hosts.joinToString(HOST_SEPARATOR)) },
)
