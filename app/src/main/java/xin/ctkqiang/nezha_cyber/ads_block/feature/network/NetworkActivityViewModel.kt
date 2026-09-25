package xin.ctkqiang.nezha_cyber.ads_block.feature.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.ApplicationLabelCache
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.InstalledApplicationSource
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.DomainObservation
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.ObservationStore
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.PrivacyPolicyStore

/**
 * 实时活动 ViewModel。
 *
 * 界面要回答的是「刚才发生了什么、是哪个应用做的」，这两件事分别来自观测记录与包名解析。
 * 筛选条件（动作、应用、域名）与是否跟随最新记录都由界面持有。
 *
 * 暂停的实现要点：暂停期间**不停止收集**，只是不把新行放进界面。若连收集也停掉，
 * 恢复后拿到的会是暂停前的旧快照，而不是「此刻的窗口」。
 *
 * [latestRows] 在收集协程里写、在 [dispatch] 里读，两者都在 `viewModelScope` 的主线程上，
 * 因此不需要同步。
 */
class NetworkActivityViewModel(
    private val observationStore: ObservationStore,
    installedApplicationSource: InstalledApplicationSource,
    privacyPolicyStore: PrivacyPolicyStore,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(NetworkActivityUiState())

    private val labelCache = ApplicationLabelCache(installedApplicationSource)

    private var latestRows: List<NetworkActivityRow> = emptyList()

    val uiState: StateFlow<NetworkActivityUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            observationStore.recent.collect { observations ->
                val packageNames = observations.mapNotNull { observation -> observation.packageName }.toSet()
                val labels = labelCache.resolve(packageNames)
                val rows = observations.mapIndexed { index, observation -> observation.toRow(index, labels) }
                val options = observations.toApplicationOptions(labels)
                latestRows = rows
                mutableUiState.update { current ->
                    current.copy(
                        rows = if (current.isLive) rows else current.rows,
                        applicationOptions = options,
                        applicationFilter = current.applicationFilter.coerceToAvailable(options),
                        isLoading = false,
                    )
                }
            }
        }
        viewModelScope.launch {
            privacyPolicyStore.policy.collect { policy ->
                mutableUiState.update { current ->
                    current.copy(isObservationLoggingEnabled = policy.isObservationLoggingEnabled)
                }
            }
        }
    }

    fun dispatch(intent: NetworkActivityUiIntent) {
        when (intent) {
            is NetworkActivityUiIntent.SetBlockedOnly -> mutableUiState.update { current ->
                current.copy(blockedOnly = intent.blockedOnly)
            }

            is NetworkActivityUiIntent.SetApplicationFilter -> mutableUiState.update { current ->
                current.copy(applicationFilter = intent.filter)
            }

            is NetworkActivityUiIntent.QueryChanged -> mutableUiState.update { current ->
                current.copy(query = intent.query)
            }

            is NetworkActivityUiIntent.SetLive -> setLive(intent.isLive)
        }
    }

    /**
     * 切换实时与暂停。
     *
     * 恢复实时时一次性换上最新的一份，因此用户看到的始终是此刻的窗口，
     * 而不是他按下暂停那一刻的残影。
     */
    private fun setLive(isLive: Boolean) {
        mutableUiState.update { current ->
            current.copy(isLive = isLive, rows = if (isLive) latestRows else current.rows)
        }
    }

    /** 出现过的应用按观测次数降序，次数相同则保持首次出现顺序。 */
    private fun List<DomainObservation>.toApplicationOptions(labels: Map<String, String>): List<ApplicationOption> =
        groupingBy { observation -> observation.packageName }
            .eachCount()
            .entries
            .sortedByDescending { entry -> entry.value }
            .map { entry ->
                ApplicationOption(
                    packageName = entry.key,
                    label = entry.key?.let { name -> labels[name] ?: name },
                )
            }

    /**
     * 选中的应用可能因为观测窗口滚动而消失。此时保留筛选会让页面永远空着，
     * 而顶部又没有对应的可选项可以取消，因此退回「全部」。
     */
    private fun ApplicationFilter.coerceToAvailable(options: List<ApplicationOption>): ApplicationFilter =
        if (this is ApplicationFilter.Source && options.none { option -> option.packageName == packageName }) {
            ApplicationFilter.All
        } else {
            this
        }

    private fun DomainObservation.toRow(index: Int, labels: Map<String, String>): NetworkActivityRow =
        NetworkActivityRow(
            // 同一毫秒内可能有同一应用对同一域名的两次查询，因此把序号并入 key，
            // 保证 LazyColumn 的键在整份列表里唯一。
            id = "$index-${at.toEpochMilli()}-$host-${packageName.orEmpty()}",
            at = at,
            host = host,
            appLabel = packageName?.let { name -> labels[name] ?: name },
            blocked = isBlocked,
            matchedRule = matchedRule,
            source = source,
            packageName = packageName,
        )

    companion object {
        fun factory(
            observationStore: ObservationStore,
            installedApplicationSource: InstalledApplicationSource,
            privacyPolicyStore: PrivacyPolicyStore,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                NetworkActivityViewModel(
                    observationStore = observationStore,
                    installedApplicationSource = installedApplicationSource,
                    privacyPolicyStore = privacyPolicyStore,
                )
            }
        }
    }
}
