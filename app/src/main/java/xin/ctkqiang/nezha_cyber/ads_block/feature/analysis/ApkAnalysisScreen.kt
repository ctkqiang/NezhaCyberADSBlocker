package xin.ctkqiang.nezha_cyber.ads_block.feature.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import xin.ctkqiang.nezha_cyber.ads_block.R
import xin.ctkqiang.nezha_cyber.ads_block.ui.analysis.ApkPicker
import xin.ctkqiang.nezha_cyber.ads_block.ui.analysis.LocalApkPicker
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaEmptyState
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaListScaffold
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaMetricRow
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaPillButton
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaSurfaceCard
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.LocalApkAnalyzer
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.LocalObservationStore
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.NezhaDataPreviewHost
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaThemePreview

/**
 * APK 分析：解析所选 APK 的 DEX 字符串池，列出候选域名并标注运行时是否被观测到。
 *
 * 措辞必须严格：候选只说明字符串出现在 APK 里，不代表会联网、更不代表是广告
 * （工程规则第 12、16、32 节）。页面上把这条写清楚，比在报告里写一百行免责更有用。
 */
@Composable
fun ApkAnalysisScreen(modifier: Modifier = Modifier) {
    val apkAnalyzer = LocalApkAnalyzer.current
    val observationStore = LocalObservationStore.current
    val picker = LocalApkPicker.current
    val viewModel: ApkAnalysisViewModel = viewModel(
        factory = ApkAnalysisViewModel.factory(apkAnalyzer, observationStore),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ApkAnalysisUiEffect.RequestFile -> picker.pick { source ->
                    viewModel.dispatch(ApkAnalysisUiIntent.FilePicked(source))
                }
            }
        }
    }
    ApkAnalysisContent(uiState = uiState, onIntent = viewModel::dispatch, modifier = modifier)
}

@Composable
private fun ApkAnalysisContent(
    uiState: ApkAnalysisUiState,
    onIntent: (ApkAnalysisUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    NezhaListScaffold(
        modifier = modifier,
        header = { ApkAnalysisHeader(uiState = uiState, onIntent = onIntent) },
    ) {
        if (uiState.isAnalyzing) {
            item(key = ANALYZING_KEY) {
                Note(text = stringResource(R.string.apk_analyzing))
            }
        }
        if (uiState.hasFailed) {
            item(key = FAILED_KEY) {
                Note(text = stringResource(R.string.apk_failed))
            }
        }
        val result = uiState.displayName
        if (result == null && !uiState.isAnalyzing && !uiState.hasFailed) {
            item(key = EMPTY_KEY) {
                NezhaEmptyState(
                    description = stringResource(R.string.apk_empty),
                    tag = stringResource(R.string.apk_empty_tag),
                )
            }
        }
        // 汇总行必须在 item 的 @Composable 内容里计算：LazyListScope 的 content 本身不是
        // @Composable 上下文，在那里调用 stringResource 会直接编译失败。
        item(key = SUMMARY_KEY) {
            ApkSummaryCard(uiState = uiState)
        }
        if (uiState.candidates.isNotEmpty()) {
            item(key = CANDIDATE_TITLE_KEY) {
                BasicText(
                    text = stringResource(R.string.apk_candidates_title),
                    style = NezhaTheme.typography.title.copy(color = NezhaTheme.palette.textPrimary),
                )
            }
            items(items = uiState.candidates, key = { row -> row.host }) { row ->
                CandidateRow(row = row)
            }
            item(key = NOTE_KEY) {
                Note(text = stringResource(R.string.apk_note))
            }
        }
    }
}

@Composable
private fun ApkAnalysisHeader(uiState: ApkAnalysisUiState, onIntent: (ApkAnalysisUiIntent) -> Unit) {
    val palette = NezhaTheme.palette
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NezhaDimens.blockGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NezhaPillButton(
            text = stringResource(R.string.apk_pick_action),
            enabled = !uiState.isAnalyzing,
            onClick = { onIntent(ApkAnalysisUiIntent.PickFile) },
        )
        BasicText(
            text = uiState.displayName.orEmpty(),
            style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
        )
    }
    if (uiState.isTruncated) {
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        BasicText(
            text = stringResource(R.string.apk_truncated, uiState.candidates.size),
            style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
        )
    }
}

@Composable
private fun CandidateRow(row: ApkCandidateRow) {
    val palette = NezhaTheme.palette
    NezhaSurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = row.host,
                modifier = Modifier.weight(1f),
                style = NezhaTheme.typography.body.copy(
                    color = if (row.observed) palette.textPrimary else palette.textSecondary,
                ),
            )
            BasicText(
                text = stringResource(R.string.apk_candidate_occurrences, row.occurrences),
                style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
            )
        }
        if (row.observed) {
            Spacer(modifier = Modifier.height(NezhaDimens.tightGap))
            BasicText(
                text = stringResource(R.string.apk_candidate_observed),
                style = NezhaTheme.typography.caption.copy(color = palette.brand),
            )
        }
    }
}

@Composable
private fun Note(text: String) {
    BasicText(
        text = text,
        style = NezhaTheme.typography.caption.copy(color = NezhaTheme.palette.textSecondary),
    )
}

private data class SummaryRow(val label: String, val value: String)

@Composable
private fun ApkSummaryCard(uiState: ApkAnalysisUiState) {
    val rows = uiState.summaryRows()
    if (rows.isEmpty()) return
    NezhaSurfaceCard {
        rows.forEach { row ->
            NezhaMetricRow(label = row.label, value = row.value, highlight = false)
        }
    }
}

@Composable
private fun ApkAnalysisUiState.summaryRows(): List<SummaryRow> {
    val packageName = packageName ?: return emptyList()
    return listOf(
        SummaryRow(stringResource(R.string.apk_result_package), packageName),
        SummaryRow(
            label = stringResource(R.string.apk_result_version),
            value = versionName ?: stringResource(R.string.apk_result_unknown),
        ),
        SummaryRow(stringResource(R.string.apk_result_dex), stringResource(R.string.apk_dex_count, dexFileCount)),
        SummaryRow(stringResource(R.string.apk_result_candidates), candidates.size.toString()),
    )
}

private const val ANALYZING_KEY = "apk-analyzing"

private const val FAILED_KEY = "apk-failed"

private const val EMPTY_KEY = "apk-empty"

private const val SUMMARY_KEY = "apk-summary"

private const val CANDIDATE_TITLE_KEY = "apk-candidate-title"

private const val NOTE_KEY = "apk-note"

/**
 * 预览里没有真实文件选择器，替身直接回调「用户取消」，避免预览触发平台交互。
 */
@Preview(name = "APK 分析 · 明暗对照", showBackground = true, widthDp = 800, heightDp = 900)
@Composable
private fun ApkAnalysisScreenPreview() {
    NezhaThemePreview {
        NezhaDataPreviewHost {
            CompositionLocalProvider(
                LocalApkPicker provides ApkPicker { onPicked -> onPicked(null) },
            ) {
                ApkAnalysisScreen()
            }
        }
    }
}

@Preview(name = "APK 分析 · 分析结果", showBackground = true, widthDp = 412, heightDp = 900)
@Composable
private fun ApkAnalysisResultPreview() {
    NezhaThemePreview {
        ApkAnalysisContent(
            uiState = ApkAnalysisUiState(
                displayName = "sample.apk",
                packageName = "com.example.preview",
                versionName = "1.0.0",
                dexFileCount = 2,
                candidates = listOf(
                    ApkCandidateRow(host = "ads.example.com", occurrences = 7, observed = true),
                    ApkCandidateRow(host = "cdn.example.net", occurrences = 4, observed = true),
                    ApkCandidateRow(host = "com.example.preview", occurrences = 2, observed = false),
                ),
            ),
            onIntent = {},
        )
    }
}
