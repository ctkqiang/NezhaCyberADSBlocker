package xin.ctkqiang.nezha_cyber.ads_block.feature.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xin.ctkqiang.nezha_cyber.ads_block.domain.analysis.ApkAnalysisOutcome
import xin.ctkqiang.nezha_cyber.ads_block.domain.analysis.ApkAnalysisResult
import xin.ctkqiang.nezha_cyber.ads_block.domain.analysis.ApkAnalyzer
import xin.ctkqiang.nezha_cyber.ads_block.domain.analysis.ApkSource
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.ObservationStore

private const val EFFECT_BUFFER_CAPACITY = 1

/**
 * APK 分析 ViewModel。
 *
 * 分析结果与「最近观测中出现过」的标记分两步：观测窗口随时在变，标记必须跟着重算，
 * 而分析结果只在用户选文件时更新一次。把两者放在一起重算，才能保证标记不会停留在旧窗口上。
 */
class ApkAnalysisViewModel(private val apkAnalyzer: ApkAnalyzer, observationStore: ObservationStore) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ApkAnalysisUiState())

    private val mutableEffect = MutableSharedFlow<ApkAnalysisUiEffect>(extraBufferCapacity = EFFECT_BUFFER_CAPACITY)

    private var observedHosts: Set<String> = emptySet()

    private var analysisResult: ApkAnalysisResult? = null

    val uiState: StateFlow<ApkAnalysisUiState> = mutableUiState.asStateFlow()

    val effect: SharedFlow<ApkAnalysisUiEffect> = mutableEffect.asSharedFlow()

    init {
        viewModelScope.launch {
            observationStore.recent.collect { observations ->
                observedHosts = observations.map { observation -> observation.host }.toSet()
                publish()
            }
        }
    }

    fun dispatch(intent: ApkAnalysisUiIntent) {
        when (intent) {
            ApkAnalysisUiIntent.PickFile -> mutableEffect.tryEmit(ApkAnalysisUiEffect.RequestFile)
            is ApkAnalysisUiIntent.FilePicked -> analyze(intent.source)
        }
    }

    private fun analyze(source: ApkSource?) {
        // 用户取消选择是正常路径：保留上一次的结果，不进入失败态也不清空界面。
        if (source == null) return
        viewModelScope.launch {
            mutableUiState.update { state -> state.copy(isAnalyzing = true, hasFailed = false) }
            val outcome = apkAnalyzer.analyze(source)
            analysisResult = (outcome as? ApkAnalysisOutcome.Analyzed)?.result
            mutableUiState.update { state ->
                state.copy(isAnalyzing = false, hasFailed = outcome is ApkAnalysisOutcome.Unreadable)
            }
            publish()
        }
    }

    private fun publish() {
        val result = analysisResult
        mutableUiState.update { state ->
            state.copy(
                displayName = result?.displayName,
                packageName = result?.packageName,
                versionName = result?.versionName,
                dexFileCount = result?.dexFileCount ?: 0,
                isTruncated = result?.isTruncated ?: false,
                candidates = result?.candidates.orEmpty().map { candidate ->
                    ApkCandidateRow(
                        host = candidate.host,
                        occurrences = candidate.occurrences,
                        observed = candidate.host in observedHosts,
                    )
                },
            )
        }
    }

    companion object {
        fun factory(apkAnalyzer: ApkAnalyzer, observationStore: ObservationStore): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { ApkAnalysisViewModel(apkAnalyzer, observationStore) }
            }
    }
}
