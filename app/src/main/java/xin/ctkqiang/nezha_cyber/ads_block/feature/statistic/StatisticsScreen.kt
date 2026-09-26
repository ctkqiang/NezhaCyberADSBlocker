package xin.ctkqiang.nezha_cyber.ads_block.feature.statistic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import xin.ctkqiang.nezha_cyber.ads_block.R
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaChartLegendEntry
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaComparisonBar
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaComparisonChart
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaMetricRow
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaPillButton
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaScreenScaffold
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaSurfaceCard
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.LocalInstalledApplicationSource
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.LocalObservationStore
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.LocalPrivacyPolicyStore
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.LocalProtectedApplicationStore
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.LocalRuleStore
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.NezhaDataPreviewHost
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaThemePreview

/**
 * 统计：过滤结果的聚合读数。
 *
 * 读数的来源与范围必须写在页面上，否则「已拦截 96」会被当成全部历史：累计计数来自持久化，
 * 排行榜与按应用读数只覆盖内存里保留的最近一段观测（工程规则第 24 节要求区分已观测、已拦截、已放行，
 * 并要求统计能按应用维度下钻）。
 *
 * 三张图各自绑定一种口径，不允许混用：
 * - 占比环 = 累计（跨会话）；
 * - 趋势柱 = 最近观测窗口；
 * - 对比条 = 最近观测窗口。
 * 每张图下方都写明自己的口径，因为读者会把同一页里的图放在一起比较，而口径不同的图放在
 * 一起比较正是最容易得出错误结论的地方。
 */
@Composable
fun StatisticsScreen(modifier: Modifier = Modifier) {
    val observationStore = LocalObservationStore.current
    val ruleStore = LocalRuleStore.current
    val installedApplicationSource = LocalInstalledApplicationSource.current
    val protectedApplicationStore = LocalProtectedApplicationStore.current
    val privacyPolicyStore = LocalPrivacyPolicyStore.current
    val viewModel: StatisticsViewModel = viewModel(
        factory = StatisticsViewModel.factory(
            observationStore = observationStore,
            ruleStore = ruleStore,
            installedApplicationSource = installedApplicationSource,
            protectedApplicationStore = protectedApplicationStore,
            privacyPolicyStore = privacyPolicyStore,
        ),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StatisticsContent(uiState = uiState, onIntent = viewModel::dispatch, modifier = modifier)
}

@Composable
private fun StatisticsContent(
    uiState: StatisticsUiState,
    onIntent: (StatisticsUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = NezhaTheme.palette
    NezhaScreenScaffold(modifier = modifier) {
        StatisticsHero(blocked = uiState.statistics.blocked)
        Spacer(modifier = Modifier.height(NezhaDimens.sectionGap))
        ProportionCard(statistics = uiState.statistics)
        Spacer(modifier = Modifier.height(NezhaDimens.sectionGap))
        MetricsCard(uiState = uiState)
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        BasicText(
            text = stringResource(R.string.statistics_scope_note),
            style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
        )
        if (!uiState.isObservationLoggingEnabled) {
            Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
            BasicText(
                text = stringResource(R.string.statistics_logging_disabled_note),
                style = NezhaTheme.typography.caption.copy(color = palette.brand),
            )
        }
        Spacer(modifier = Modifier.height(NezhaDimens.sectionGap))
        TrendCard(uiState = uiState)
        Spacer(modifier = Modifier.height(NezhaDimens.sectionGap))
        ApplicationTrafficCard(summaries = uiState.applicationTraffic)
        Spacer(modifier = Modifier.height(NezhaDimens.sectionGap))
        RulesCard(uiState = uiState)
        Spacer(modifier = Modifier.height(NezhaDimens.sectionGap))
        TopBlockedCard(domains = uiState.topBlockedDomains)
        Spacer(modifier = Modifier.height(NezhaDimens.sectionGap))
        ClearStatisticsCard(uiState = uiState, onIntent = onIntent)
    }
}

@Composable
private fun StatisticsHero(blocked: Long) {
    val palette = NezhaTheme.palette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = NezhaDimens.sectionGap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(
            text = blocked.toString(),
            style = NezhaTheme.typography.hero.copy(color = palette.brand),
        )
        BasicText(
            text = stringResource(R.string.statistics_blocked_caption),
            style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
        )
    }
}

/** 整体读数（第 24 节的第一个维度）。 */
@Composable
private fun MetricsCard(uiState: StatisticsUiState) {
    val statistics = uiState.statistics
    NezhaSurfaceCard {
        NezhaMetricRow(
            label = stringResource(R.string.statistics_observed),
            value = statistics.observed.toString(),
            highlight = false,
        )
        NezhaMetricRow(
            label = stringResource(R.string.statistics_blocked),
            value = statistics.blocked.toString(),
        )
        NezhaMetricRow(
            label = stringResource(R.string.statistics_relayed),
            value = statistics.relayed.toString(),
            highlight = false,
        )
        NezhaMetricRow(
            label = stringResource(R.string.statistics_session_blocked_hosts),
            value = statistics.sessionDistinctBlockedHosts.toString(),
        )
        NezhaMetricRow(
            label = stringResource(R.string.statistics_protected_applications),
            value = when (val scope = uiState.protectedScope) {
                ProtectedScope.AllApplications -> stringResource(R.string.statistics_protected_all)
                is ProtectedScope.Selected -> stringResource(R.string.statistics_count_value, scope.count)
            },
            highlight = false,
        )
    }
}

/** 生效规则数（第 24 节）。两个来源分开列，因为它们的可靠程度不同。 */
@Composable
private fun RulesCard(uiState: StatisticsUiState) {
    val palette = NezhaTheme.palette
    NezhaSurfaceCard {
        BasicText(
            text = stringResource(R.string.statistics_rules_title),
            style = NezhaTheme.typography.title.copy(color = palette.textPrimary),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        NezhaMetricRow(
            label = stringResource(R.string.statistics_builtin_rules),
            value = stringResource(
                R.string.statistics_builtin_rules_value,
                uiState.builtinVersion,
                uiState.builtinRuleCount,
            ),
            highlight = false,
        )
        NezhaMetricRow(
            label = stringResource(R.string.statistics_user_rules),
            value = stringResource(R.string.statistics_count_value, uiState.userRuleCount),
            highlight = false,
        )
    }
}

/**
 * 最近拦截最多的域名。
 *
 * 这一张只有一条序列（全部是拦截计数），因此 `rest` 恒为 0、条长就是拦截次数。
 * 只放「已拦截」一个图例而不是照抄两档：给一条序列配一个恒为空图例，会让读者去找那条
 * 根本不存在的灰色柱。
 */
@Composable
private fun TopBlockedCard(domains: List<BlockedDomainCount>) {
    val palette = NezhaTheme.palette
    NezhaSurfaceCard {
        BasicText(
            text = stringResource(R.string.statistics_top_title),
            style = NezhaTheme.typography.title.copy(color = palette.textPrimary),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        if (domains.isEmpty()) {
            BasicText(
                text = stringResource(R.string.statistics_top_empty),
                style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
            )
        } else {
            NezhaChartLegendEntry(
                color = palette.brand,
                label = stringResource(R.string.statistics_legend_blocked),
            )
            Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
            NezhaComparisonChart(
                bars = domains.map { domain ->
                    NezhaComparisonBar(
                        label = domain.host,
                        highlighted = domain.count.toLong(),
                        rest = 0L,
                        trailing = stringResource(R.string.statistics_count_value, domain.count),
                    )
                },
            )
            Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
            BasicText(
                text = stringResource(R.string.statistics_top_note),
                style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
            )
        }
    }
}

/**
 * 清除统计。
 *
 * 用两次点按代替确认对话框：清空不可撤销，但代价只是本机的一份诊断数据，为一个对话框
 * 引入一整套浮层组件并不划算。第一次点按之后按钮换成「确认清除 / 取消」，用户随时可以退回。
 */
@Composable
private fun ClearStatisticsCard(uiState: StatisticsUiState, onIntent: (StatisticsUiIntent) -> Unit) {
    val palette = NezhaTheme.palette
    NezhaSurfaceCard {
        BasicText(
            text = stringResource(R.string.statistics_clear_title),
            style = NezhaTheme.typography.title.copy(color = palette.textPrimary),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        BasicText(
            text = stringResource(R.string.statistics_clear_note),
            style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
        )
        if (uiState.isConfirmingClear) {
            Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
            BasicText(
                text = stringResource(R.string.statistics_clear_confirm_note),
                style = NezhaTheme.typography.caption.copy(color = palette.brand),
            )
        }
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        Row(
            horizontalArrangement = Arrangement.spacedBy(NezhaDimens.blockGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (uiState.isConfirmingClear) {
                NezhaPillButton(
                    text = stringResource(R.string.statistics_clear_confirm_action),
                    onClick = { onIntent(StatisticsUiIntent.ConfirmClear) },
                )
                NezhaPillButton(
                    text = stringResource(R.string.statistics_clear_cancel_action),
                    onClick = { onIntent(StatisticsUiIntent.CancelClear) },
                )
            } else {
                NezhaPillButton(
                    text = stringResource(R.string.statistics_clear_action),
                    enabled = uiState.hasStatistics,
                    onClick = { onIntent(StatisticsUiIntent.RequestClear) },
                )
            }
        }
    }
}

@Preview(name = "统计 · 明暗对照", showBackground = true, widthDp = 800, heightDp = 1280)
@Composable
private fun StatisticsScreenPreview() {
    NezhaThemePreview {
        NezhaDataPreviewHost {
            StatisticsScreen()
        }
    }
}

@Preview(name = "统计 · 空数据", showBackground = true, widthDp = 412, heightDp = 900)
@Composable
private fun StatisticsEmptyPreview() {
    NezhaThemePreview {
        StatisticsContent(uiState = StatisticsUiState(isLoading = false), onIntent = {})
    }
}

@Preview(name = "统计 · 待确认清除", showBackground = true, widthDp = 412, heightDp = 900)
@Composable
private fun StatisticsConfirmClearPreview() {
    NezhaThemePreview {
        StatisticsContent(
            uiState = StatisticsUiState(
                protectedScope = ProtectedScope.Selected(count = 3),
                applicationTraffic = PREVIEW_TRAFFIC,
                isLoading = false,
                isConfirmingClear = true,
            ),
            onIntent = {},
        )
    }
}

private val PREVIEW_TRAFFIC = listOf(
    ApplicationTrafficSummary(
        packageName = "com.example.browser",
        label = "示例浏览器",
        observed = 42,
        blocked = 18,
        topHosts = listOf("ads.example.com", "doubleclick.net", "metrics.example.io"),
    ),
    ApplicationTrafficSummary(
        packageName = "com.example.video",
        label = "示例视频",
        observed = 21,
        blocked = 4,
        topHosts = listOf("adservice.example.net", "cdn.example.net"),
    ),
    ApplicationTrafficSummary(
        packageName = null,
        label = null,
        observed = 7,
        blocked = 0,
        topHosts = listOf("api.example.org"),
    ),
)
