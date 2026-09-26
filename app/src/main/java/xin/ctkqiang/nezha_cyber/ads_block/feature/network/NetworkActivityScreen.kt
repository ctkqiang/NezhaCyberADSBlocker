package xin.ctkqiang.nezha_cyber.ads_block.feature.network

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import xin.ctkqiang.nezha_cyber.ads_block.R
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleSource
import xin.ctkqiang.nezha_cyber.ads_block.ui.application.rememberApplicationIcon
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaAppIcon
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaEmptyState
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaListScaffold
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaPillButton
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaSegmentedControl
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaTextField
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.LocalInstalledApplicationSource
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.LocalObservationStore
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.LocalPrivacyPolicyStore
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.NezhaDataPreviewHost
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaThemePreview

/**
 * 实时活动：本机应用正在解析哪些域名的流水。
 *
 * 每一行回答三个问题：「哪个应用」「请求了哪个域名」「结果如何」，并带上到毫秒的时间。
 * 呈现方式刻意接近抓包工具而不是常规列表：等宽的时间与域名让整列可以纵向扫读，
 * 紧凑留白配合发丝分隔线让一屏能看到十行左右，实时与暂停可切换以便定住某一瞬。
 *
 * 关于「这次请求用了哪个权限」：Android **没有**这个概念。网络请求只与 `INTERNET` 权限相关，
 * 系统不提供逐请求的权限归属，因此这里只能显示应用本身，权限清单在应用详情页里。
 * 不做任何假装能显示的样子（工程规则第 32 节）。
 *
 * 只展示元数据，不展示任何载荷内容（第 20、23 节）。
 */
@Composable
fun NetworkActivityScreen(modifier: Modifier = Modifier) {
    val observationStore = LocalObservationStore.current
    val installedApplicationSource = LocalInstalledApplicationSource.current
    val privacyPolicyStore = LocalPrivacyPolicyStore.current
    val viewModel: NetworkActivityViewModel = viewModel(
        factory = NetworkActivityViewModel.factory(
            observationStore = observationStore,
            installedApplicationSource = installedApplicationSource,
            privacyPolicyStore = privacyPolicyStore,
        ),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NetworkActivityContent(uiState = uiState, onIntent = viewModel::dispatch, modifier = modifier)
}

@Composable
private fun NetworkActivityContent(
    uiState: NetworkActivityUiState,
    onIntent: (NetworkActivityUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = NezhaTheme.palette
    val listState = rememberLazyListState()
    val newestId = uiState.visibleRows.firstOrNull()?.id
    LaunchedEffect(newestId, uiState.isLive) {
        // 只在视口本来就停在顶部时跟随：用户已经往下翻说明正在读旧记录，
        // 这时把他拽回顶部等于抢走滚动权。
        if (uiState.isLive && listState.firstVisibleItemIndex <= FOLLOW_TOP_THRESHOLD) {
            listState.animateScrollToItem(0)
        }
    }
    NezhaListScaffold(
        modifier = modifier,
        state = listState,
        verticalArrangement = Arrangement.spacedBy(NO_ROW_GAP),
        header = { NetworkActivityControls(uiState = uiState, onIntent = onIntent) },
    ) {
        if (uiState.visibleRows.isEmpty()) {
            item(key = EMPTY_ITEM_KEY) {
                NetworkActivityEmptyState(uiState = uiState)
            }
        } else {
            itemsIndexed(items = uiState.visibleRows, key = { _, row -> row.id }) { index, row ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(NezhaDimens.hairline)
                            .background(palette.outline),
                    )
                }
                TraceRow(row = row)
            }
        }
    }
}

@Composable
private fun NetworkActivityControls(uiState: NetworkActivityUiState, onIntent: (NetworkActivityUiIntent) -> Unit) {
    val palette = NezhaTheme.palette
    val visibleRows = uiState.visibleRows
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NezhaDimens.blockGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NezhaTextField(
            value = uiState.query,
            onValueChange = { query -> onIntent(NetworkActivityUiIntent.QueryChanged(query)) },
            placeholder = stringResource(R.string.network_search_hint),
            modifier = Modifier.weight(1f),
        )
        NezhaPillButton(
            text = stringResource(
                if (uiState.isLive) R.string.network_pause_action else R.string.network_resume_action,
            ),
            onClick = { onIntent(NetworkActivityUiIntent.SetLive(!uiState.isLive)) },
        )
    }
    Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
    NezhaSegmentedControl(
        items = FILTER_OPTIONS,
        selected = uiState.blockedOnly,
        label = { blockedOnly ->
            stringResource(if (blockedOnly) R.string.network_filter_blocked else R.string.network_filter_all)
        },
        onSelect = { blockedOnly -> onIntent(NetworkActivityUiIntent.SetBlockedOnly(blockedOnly)) },
    )
    if (uiState.applicationOptions.isNotEmpty()) {
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        ApplicationFilterRow(uiState = uiState, onIntent = onIntent)
    }
    Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
    BasicText(
        text = stringResource(
            if (uiState.isFiltered) R.string.network_summary_filtered else R.string.network_summary,
            visibleRows.size,
            visibleRows.count { row -> row.blocked },
        ),
        style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
    )
    if (!uiState.isLive) {
        Spacer(modifier = Modifier.height(NezhaDimens.tightGap))
        BasicText(
            text = stringResource(R.string.network_paused_note),
            style = NezhaTheme.typography.caption.copy(color = palette.brand),
        )
    }
    if (!uiState.isObservationLoggingEnabled) {
        Spacer(modifier = Modifier.height(NezhaDimens.tightGap))
        BasicText(
            text = stringResource(R.string.network_logging_disabled_note),
            style = NezhaTheme.typography.caption.copy(color = palette.brand),
        )
    }
}

/**
 * 应用筛选。
 *
 * 「全部应用」由界面补在最前面，它的文案是字符串资源，ViewModel 拿不到。
 * 横向滚动而不是换行：应用数没有上限，换行会把页头撑到吃掉半个屏幕。
 */
@Composable
private fun ApplicationFilterRow(uiState: NetworkActivityUiState, onIntent: (NetworkActivityUiIntent) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(NezhaDimens.tightGap)) {
        item(key = ALL_APPLICATIONS_KEY) {
            NezhaPillButton(
                text = stringResource(R.string.network_app_all),
                selected = uiState.applicationFilter == ApplicationFilter.All,
                onClick = { onIntent(NetworkActivityUiIntent.SetApplicationFilter(ApplicationFilter.All)) },
            )
        }
        items(
            items = uiState.applicationOptions,
            key = { option -> option.packageName ?: UNKNOWN_APPLICATION_KEY },
        ) { option ->
            NezhaPillButton(
                text = option.label ?: stringResource(R.string.network_app_unknown),
                selected = uiState.applicationFilter == ApplicationFilter.Source(option.packageName),
                onClick = {
                    onIntent(NetworkActivityUiIntent.SetApplicationFilter(ApplicationFilter.Source(option.packageName)))
                },
            )
        }
    }
}

/**
 * 一条实时记录。
 *
 * 三行固定分工：应用与时间、域名、结论。时间戳与域名用等宽字体，使整列纵向对齐——
 * 比例字体下冒号与小数点会让每一行错开，眼睛无法沿着一列往下扫。
 *
 * 图标只在这三行里的第一行出现，且尺寸比常规列表小：[NezhaDimens.appIconSize] 的 36dp
 * 会把行高抬到 110dp 以上，一屏就只剩五行。
 */
@Composable
private fun TraceRow(row: NetworkActivityRow) {
    val palette = NezhaTheme.palette
    val icon = rememberApplicationIcon(packageName = row.packageName)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = NezhaDimens.traceRowVerticalPadding),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NezhaAppIcon(icon = icon, label = row.appLabel, size = NezhaDimens.appIconCompactSize)
            Spacer(modifier = Modifier.width(NezhaDimens.blockGap))
            BasicText(
                text = row.appLabel ?: stringResource(R.string.network_app_unknown),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = NezhaTheme.typography.label.copy(color = palette.textPrimary),
            )
            Spacer(modifier = Modifier.width(NezhaDimens.blockGap))
            BasicText(
                text = TRACE_TIME_FORMATTER.format(row.at.atZone(ZoneId.systemDefault())),
                style = NezhaTheme.typography.mono.copy(color = palette.textSecondary),
            )
        }
        Spacer(modifier = Modifier.height(NezhaDimens.tightGap))
        BasicText(
            text = row.host,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = NezhaTheme.typography.mono.copy(color = palette.textPrimary),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.tightGap))
        BasicText(
            text = decisionLabel(row = row),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = NezhaTheme.typography.caption.copy(
                color = if (row.blocked) palette.brand else palette.textSecondary,
            ),
        )
    }
}

/**
 * 决策说明。
 *
 * 来源不同，措辞必须不同：内置清单与关键词规则的证据强度不一样，混成一句「已拦截」
 * 就等于把推断当成了事实（工程规则第 17、32 节）。
 */
@Composable
private fun decisionLabel(row: NetworkActivityRow): String {
    val action = stringResource(
        if (row.blocked) R.string.network_action_blocked else R.string.network_action_relayed,
    )
    val reason = when (row.source) {
        RuleSource.APP_ADS -> stringResource(R.string.network_reason_app_ads)
        RuleSource.BUILTIN -> stringResource(R.string.network_reason_builtin)
        RuleSource.KEYWORD -> stringResource(R.string.network_reason_keyword, row.matchedRule.orEmpty())
        RuleSource.USER -> stringResource(R.string.network_reason_user, row.matchedRule.orEmpty())
        null -> stringResource(R.string.network_reason_none)
    }
    return "$action · $reason"
}

/**
 * 空状态的三种含义必须分开。
 *
 * 「记录被关了」「还没观测到任何查询」「有观测但都不符合筛选」是三件不同的事，
 * 措辞相同会让用户以为隧道没工作。判据是**未经筛选**的行是否为空，而不是当前显示的行。
 */
@Composable
private fun NetworkActivityEmptyState(uiState: NetworkActivityUiState) {
    val hasNoObservation = uiState.rows.isEmpty()
    val isLoggingDisabled = !uiState.isObservationLoggingEnabled && hasNoObservation
    NezhaEmptyState(
        description = stringResource(
            when {
                uiState.isLoading -> R.string.network_loading
                isLoggingDisabled -> R.string.network_logging_disabled
                hasNoObservation -> R.string.network_empty
                else -> R.string.network_filter_empty
            },
        ),
        tag = stringResource(
            when {
                isLoggingDisabled -> R.string.network_logging_disabled_tag
                hasNoObservation -> R.string.network_empty_tag
                else -> R.string.network_filter_empty_tag
            },
        ),
    )
}

/**
 * 时间戳格式。含毫秒是刻意的：同一秒内往往有多次查询，只精确到秒会让几行看起来完全同时发生，
 * 而排查问题时「哪一条在前」正是关键。
 */
private val TRACE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

/** 流水行之间不留间距，间距由各行的内边距与一条发丝分隔线提供。 */
private val NO_ROW_GAP = 0.dp

/** 跟随最新记录时允许的最大偏移。0 表示视口必须停在最顶上的那一行。 */
private const val FOLLOW_TOP_THRESHOLD = 0

private const val EMPTY_ITEM_KEY = "network-activity-empty"

private const val ALL_APPLICATIONS_KEY = "network-application-all"

/** 「未知来源」那一组的键。包名不可能是空字符串，因此不会与真实包名冲突。 */
private const val UNKNOWN_APPLICATION_KEY = "network-application-unknown"

/** 筛选选项用布尔本身表达，不必为此再造一个枚举。 */
private val FILTER_OPTIONS = listOf(false, true)

@Preview(name = "实时活动 · 明暗对照", showBackground = true, widthDp = 800, heightDp = 1000)
@Composable
private fun NetworkActivityScreenPreview() {
    NezhaThemePreview {
        NezhaDataPreviewHost {
            NetworkActivityScreen()
        }
    }
}

@Preview(name = "实时活动 · 空状态", showBackground = true, widthDp = 412, heightDp = 720)
@Composable
private fun NetworkActivityEmptyPreview() {
    NezhaThemePreview {
        NetworkActivityContent(
            uiState = NetworkActivityUiState(rows = emptyList(), isLoading = false),
            onIntent = {},
        )
    }
}

@Preview(name = "实时活动 · 已暂停并按应用筛选", showBackground = true, widthDp = 412, heightDp = 720)
@Composable
private fun NetworkActivityFilteredPreview() {
    NezhaThemePreview {
        NezhaDataPreviewHost {
            NetworkActivityContent(
                uiState = NetworkActivityUiState(
                    rows = PREVIEW_ROWS,
                    applicationOptions = PREVIEW_OPTIONS,
                    blockedOnly = true,
                    applicationFilter = ApplicationFilter.Source(PREVIEW_BROWSER_PACKAGE),
                    query = "example",
                    isLive = false,
                    isLoading = false,
                ),
                onIntent = {},
            )
        }
    }
}

private const val PREVIEW_BROWSER_PACKAGE = "com.example.browser"

private const val PREVIEW_VIDEO_PACKAGE = "com.example.video"

private val PREVIEW_ROWS = listOf(
    NetworkActivityRow(
        id = "preview-1",
        at = Instant.parse("2026-09-24T10:15:30.128Z"),
        host = "ads.example.com",
        appLabel = "示例浏览器",
        blocked = true,
        matchedRule = "ads.example.com",
        source = RuleSource.BUILTIN,
        packageName = PREVIEW_BROWSER_PACKAGE,
    ),
    NetworkActivityRow(
        id = "preview-2",
        at = Instant.parse("2026-09-24T10:15:30.902Z"),
        host = "adservice.example.net",
        appLabel = "示例视频",
        blocked = true,
        matchedRule = "adservice",
        source = RuleSource.KEYWORD,
        packageName = PREVIEW_VIDEO_PACKAGE,
    ),
    NetworkActivityRow(
        id = "preview-3",
        at = Instant.parse("2026-09-24T10:15:31.014Z"),
        host = "metrics.example.io",
        appLabel = null,
        blocked = false,
        matchedRule = null,
        source = null,
        packageName = null,
    ),
)

private val PREVIEW_OPTIONS = listOf(
    ApplicationOption(packageName = PREVIEW_BROWSER_PACKAGE, label = "示例浏览器"),
    ApplicationOption(packageName = PREVIEW_VIDEO_PACKAGE, label = "示例视频"),
    ApplicationOption(packageName = null, label = null),
)
