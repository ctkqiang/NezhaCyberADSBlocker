package xin.ctkqiang.nezha_cyber.ads_block.feature.statistic

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import xin.ctkqiang.nezha_cyber.ads_block.R
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaSurfaceCard
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme

/**
 * 按应用读数（工程规则第 24 节的第二个统计维度）。
 *
 * 单独成一个文件而不是塞进统计页：这一块是「应用 → 它请求了什么」的下钻视图，
 * 与页面上其余的整体读数不是同一个概念，混在一起会让两者的边界在阅读时消失。
 *
 * 排序由 [ApplicationTrafficSummary] 的产生方（ViewModel）决定，这里只渲染，
 * 不在界面里再排一次——两处排序迟早会出现一处改了另一处没改。
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
            summaries.forEachIndexed { index, summary ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
                }
                ApplicationTrafficRow(summary = summary)
            }
            Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
            BasicText(
                text = stringResource(R.string.statistics_applications_note),
                style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
            )
        }
    }
}

/**
 * 一个应用的读数。
 *
 * 拦截数大于零时用品牌色：这一列是用户打开本页真正想找的东西，
 * 全是 0 的行不该抢走同样多的注意力。
 */
@Composable
private fun ApplicationTrafficRow(summary: ApplicationTrafficSummary) {
    val palette = NezhaTheme.palette
    Column(modifier = Modifier.fillMaxWidth()) {
        BasicText(
            text = summary.label ?: stringResource(R.string.statistics_app_unknown),
            style = NezhaTheme.typography.body.copy(color = palette.textPrimary),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.tightGap))
        BasicText(
            text = stringResource(
                R.string.statistics_app_counts,
                summary.observed,
                summary.blocked,
                summary.relayed,
            ),
            style = NezhaTheme.typography.caption.copy(
                color = if (summary.blocked > 0) palette.brand else palette.textSecondary,
            ),
        )
        if (summary.topHosts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(NezhaDimens.tightGap))
            BasicText(
                text = summary.topHosts.joinToString(separator = " · "),
                style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
            )
        }
    }
}
