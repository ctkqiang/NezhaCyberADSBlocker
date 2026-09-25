package xin.ctkqiang.nezha_cyber.ads_block.feature.analysis

import xin.ctkqiang.nezha_cyber.ads_block.domain.analysis.ApkSource

/**
 * APK 分析页状态与意图。
 *
 * [candidates] 里带 [ApkCandidateRow.observed]，是把静态候选与运行时观测对照的结果
 * （工程规则第 16 节）：静态出现 ≠ 运行时真的请求过，界面上必须能看出这个差别。
 */
data class ApkAnalysisUiState(
    val isAnalyzing: Boolean = false,
    val displayName: String? = null,
    val packageName: String? = null,
    val versionName: String? = null,
    val dexFileCount: Int = 0,
    val candidates: List<ApkCandidateRow> = emptyList(),
    val isTruncated: Boolean = false,
    val hasFailed: Boolean = false,
)

data class ApkCandidateRow(val host: String, val occurrences: Int, val observed: Boolean)

sealed interface ApkAnalysisUiIntent {
    data object PickFile : ApkAnalysisUiIntent

    data class FilePicked(val source: ApkSource?) : ApkAnalysisUiIntent
}

/**
 * 一次性效果。
 *
 * 拉起系统文件选择器属于交互动作，不能放进状态：放进状态后每次重组都会再拉起一次。
 */
sealed interface ApkAnalysisUiEffect {
    data object RequestFile : ApkAnalysisUiEffect
}
