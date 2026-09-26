package xin.ctkqiang.nezha_cyber.ads_block.feature.statistic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.time.Duration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.ApplicationLabelCache
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.InstalledApplicationSource
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.ProtectedApplicationStore
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.DomainObservation
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.ObservationStore
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.PrivacyPolicyStore
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleStore

/** 榜单长度。再多就超出「一眼看清最近拦了什么」的用途。 */
private const val TOP_DOMAIN_LIMIT = 5

/** 每个应用列出多少个高频域名。手机上一屏放不下更多，也超出「这个应用在跟谁通信」的用量。 */
private const val TOP_HOST_LIMIT_PER_APPLICATION = 3

/** 趋势图分几段。段数太少看不出起伏，太多则在手机宽度上每根柱子不足一个发丝宽。 */
private const val TREND_BUCKET_COUNT = 12

/** 少于这个条数就不画趋势：一两个点连不成趋势，硬画只会给出「有曲线」的错觉。 */
private const val MINIMUM_TREND_OBSERVATIONS = 4

/**
 * 统计 ViewModel。
 *
 * 两个数字来源必须分清，否则页面会自相矛盾：
 * - **累计计数**来自观测存储（跨会话保存）；
 * - **排行榜与按应用读数**来自最近观测窗口，只覆盖内存中保留的那一小段，因此界面必须标注范围。
 *
 * 统计口径按工程规则第 24 节：已观测 = 全部查询，已拦截 = 被规则拦下的数量，
 * 已放行 = 两者之差。「放行」不等于「用户放行」，界面措辞不能混用。
 *
 * 四个数据源各自独立订阅、各改各的字段。合并成一条流只会让任一方的更新都触发其余三方的重算。
 */
class StatisticsViewModel(
    private val observationStore: ObservationStore,
    ruleStore: RuleStore,
    installedApplicationSource: InstalledApplicationSource,
    protectedApplicationStore: ProtectedApplicationStore,
    privacyPolicyStore: PrivacyPolicyStore,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(StatisticsUiState())

    private val labelCache = ApplicationLabelCache(installedApplicationSource)

    val uiState: StateFlow<StatisticsUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            observationStore.statistics.collect { statistics ->
                mutableUiState.update { current -> current.copy(statistics = statistics, isLoading = false) }
            }
        }
        viewModelScope.launch {
            observationStore.recent.collect { observations ->
                val packageNames = observations.mapNotNull { observation -> observation.packageName }.toSet()
                val labels = labelCache.resolve(packageNames)
                mutableUiState.update { current ->
                    current.copy(
                        topBlockedDomains = observations.topBlockedDomains(),
                        applicationTraffic = observations.toApplicationTraffic(labels),
                        trafficTrend = observations.toTrafficTrend(),
                        windowObservationCount = observations.size,
                    )
                }
            }
        }
        viewModelScope.launch {
            ruleStore.snapshot.collect { snapshot ->
                mutableUiState.update { current ->
                    current.copy(
                        builtinRuleCount = snapshot.builtinRuleCount,
                        userRuleCount = snapshot.userRuleCount,
                        builtinVersion = snapshot.builtinVersion,
                    )
                }
            }
        }
        viewModelScope.launch {
            protectedApplicationStore.protectedPackages.collect { packages ->
                mutableUiState.update { current ->
                    current.copy(
                        protectedScope = if (packages.isEmpty()) {
                            ProtectedScope.AllApplications
                        } else {
                            ProtectedScope.Selected(packages.size)
                        },
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

    fun dispatch(intent: StatisticsUiIntent) {
        when (intent) {
            StatisticsUiIntent.RequestClear -> mutableUiState.update { current ->
                current.copy(isConfirmingClear = true)
            }

            StatisticsUiIntent.CancelClear -> mutableUiState.update { current ->
                current.copy(isConfirmingClear = false)
            }

            StatisticsUiIntent.ConfirmClear -> clearStatistics()
        }
    }

    /**
     * 清空后不额外发提示。
     *
     * 页面上的读数同时归零，这本身就是结果；再叠一层「已清除」的横条只会在同一次交互里
     * 用两种方式说同一件事。清空是挂起操作，因此必须回到 `viewModelScope` 上执行。
     */
    private fun clearStatistics() {
        viewModelScope.launch {
            observationStore.clear()
            mutableUiState.update { current -> current.copy(isConfirmingClear = false) }
        }
    }

    private fun List<DomainObservation>.topBlockedDomains(): List<BlockedDomainCount> =
        filter { observation -> observation.isBlocked }
            .groupingBy { observation -> observation.host }
            .eachCount()
            .entries
            .sortedByDescending { entry -> entry.value }
            .take(TOP_DOMAIN_LIMIT)
            .map { entry -> BlockedDomainCount(host = entry.key, count = entry.value) }

    private fun List<DomainObservation>.toApplicationTraffic(
        labels: Map<String, String>,
    ): List<ApplicationTrafficSummary> = groupBy { observation -> observation.packageName }
        .map { (packageName, items) ->
            ApplicationTrafficSummary(
                packageName = packageName,
                label = packageName?.let { name -> labels[name] ?: name },
                observed = items.size,
                blocked = items.count { observation -> observation.isBlocked },
                topHosts = items.topHosts(),
            )
        }
        .sortedWith(
            compareByDescending<ApplicationTrafficSummary> { summary -> summary.blocked }
                .thenByDescending { summary -> summary.observed },
        )

    /**
     * 把最近窗口按时间等分成若干段。
     *
     * 分桶边界取窗口自身的首尾时刻，而不是绝对时钟的整点：窗口只有几十条观测、跨度往往只有
     * 几分钟，按整点分桶会让绝大多数桶是空的，图上只剩孤零零一根柱子。代价是**桶宽随窗口
     * 长度变化**，因此界面必须写明这是等分，不能让读者把横轴当成固定时长。
     *
     * 样本不足时返回空列表而不是一张贴地的图：全零的图会被读成「最近没有流量」，
     * 而事实只是「样本太少」——两者对用户是相反的含义。
     */
    private fun List<DomainObservation>.toTrafficTrend(): List<TrafficBucket> {
        val ordered = sortedBy { observation -> observation.at }
        val start = ordered.firstOrNull()?.at
        val span = if (start == null || ordered.size < MINIMUM_TREND_OBSERVATIONS) {
            0L
        } else {
            Duration.between(start, ordered.last().at).toMillis()
        }
        return if (start == null || span <= 0L) {
            emptyList()
        } else {
            val buckets = MutableList(TREND_BUCKET_COUNT) { TrafficBucket(blocked = 0, relayed = 0) }
            for (observation in ordered) {
                val offset = Duration.between(start, observation.at).toMillis()
                val index = (offset * TREND_BUCKET_COUNT / span).toInt().coerceIn(0, TREND_BUCKET_COUNT - 1)
                val current = buckets[index]
                buckets[index] = if (observation.isBlocked) {
                    TrafficBucket(blocked = current.blocked + 1, relayed = current.relayed)
                } else {
                    TrafficBucket(blocked = current.blocked, relayed = current.relayed + 1)
                }
            }
            buckets
        }
    }

    private fun List<DomainObservation>.topHosts(): List<String> = groupingBy { observation -> observation.host }
        .eachCount()
        .entries
        .sortedByDescending { entry -> entry.value }
        .take(TOP_HOST_LIMIT_PER_APPLICATION)
        .map { entry -> entry.key }

    companion object {
        fun factory(
            observationStore: ObservationStore,
            ruleStore: RuleStore,
            installedApplicationSource: InstalledApplicationSource,
            protectedApplicationStore: ProtectedApplicationStore,
            privacyPolicyStore: PrivacyPolicyStore,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                StatisticsViewModel(
                    observationStore = observationStore,
                    ruleStore = ruleStore,
                    installedApplicationSource = installedApplicationSource,
                    protectedApplicationStore = protectedApplicationStore,
                    privacyPolicyStore = privacyPolicyStore,
                )
            }
        }
    }
}
