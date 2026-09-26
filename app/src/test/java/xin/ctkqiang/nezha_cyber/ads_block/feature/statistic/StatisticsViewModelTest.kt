package xin.ctkqiang.nezha_cyber.ads_block.feature.statistic

import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.InstalledApplication
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.InstalledApplicationSource
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.ProtectedApplicationStore
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.DomainObservation
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.FilteringStatistics
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.ObservationStore
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.BlockedResponseMode
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.ObservationRetention
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.PrivacyPolicy
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.PrivacyPolicyStore
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleAction
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleEditResult
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleSnapshot
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleSource
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleStore

/**
 * 统计 ViewModel 的行为测试。
 *
 * 锁住三件后续改动容易破坏的事：按应用维度的口径（工程规则第 24 节要求分开已观测、已拦截、
 * 已放行）、无法归属的观测单独成组、以及清空必须是「请求 → 确认」两步。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `按应用聚合观测数与拦截数`() {
        val store = FakeObservationStore(
            recent = listOf(
                observation("ads.example.com", blocked = true, packageName = BROWSER),
                observation("ads.example.com", blocked = true, packageName = BROWSER),
                observation("cdn.example.net", blocked = false, packageName = BROWSER),
            ),
        )

        val browser = viewModel(store).uiState.value.applicationTraffic.single()

        assertEquals(BROWSER, browser.packageName)
        assertEquals("示例浏览器", browser.label)
        assertEquals(3, browser.observed)
        assertEquals(2, browser.blocked)
        assertEquals(1, browser.relayed)
    }

    @Test
    fun `每个应用列出的是它自己的高频域名`() {
        val store = FakeObservationStore(
            recent = listOf(
                observation("ads.example.com", blocked = true, packageName = BROWSER),
                observation("ads.example.com", blocked = true, packageName = BROWSER),
                observation("cdn.example.net", blocked = false, packageName = BROWSER),
                observation("metrics.example.io", blocked = false, packageName = VIDEO),
            ),
        )

        val summaries = viewModel(store).uiState.value.applicationTraffic

        assertEquals(listOf("ads.example.com", "cdn.example.net"), summaries.first().topHosts)
        assertEquals(listOf("metrics.example.io"), summaries.last().topHosts)
    }

    @Test
    fun `按拦截数降序排列`() {
        val store = FakeObservationStore(
            recent = listOf(
                observation("metrics.example.io", blocked = false, packageName = VIDEO),
                observation("ads.example.com", blocked = true, packageName = BROWSER),
            ),
        )

        val summaries = viewModel(store).uiState.value.applicationTraffic

        assertEquals(BROWSER, summaries.first().packageName)
        assertEquals(VIDEO, summaries.last().packageName)
    }

    @Test
    fun `无法归属到应用的观测单独成组且没有显示名`() {
        val store = FakeObservationStore(
            recent = listOf(observation("api.example.org", blocked = false, packageName = null)),
        )

        val summary = viewModel(store).uiState.value.applicationTraffic.single()

        assertNull(summary.packageName)
        assertNull(summary.label)
        assertEquals(1, summary.observed)
    }

    @Test
    fun `有包名但解析不到显示名时退回包名`() {
        val store = FakeObservationStore(
            recent = listOf(observation("ads.example.com", blocked = true, packageName = UNKNOWN)),
        )

        val summary = viewModel(store).uiState.value.applicationTraffic.single()

        assertEquals(UNKNOWN, summary.label)
    }

    @Test
    fun `受保护应用为空表示接管全部应用而不是零个`() {
        val viewModel = viewModel(FakeObservationStore())

        assertEquals(ProtectedScope.AllApplications, viewModel.uiState.value.protectedScope)
    }

    @Test
    fun `受保护应用非空时给出数量`() {
        val viewModel = viewModel(FakeObservationStore(), protectedPackages = setOf(BROWSER, VIDEO))

        assertEquals(ProtectedScope.Selected(count = 2), viewModel.uiState.value.protectedScope)
    }

    @Test
    fun `请求清除只进入确认态不清空数据`() {
        val store = FakeObservationStore(
            recent = listOf(observation("ads.example.com", blocked = true, packageName = BROWSER)),
        )
        val viewModel = viewModel(store)

        viewModel.dispatch(StatisticsUiIntent.RequestClear)

        assertTrue(viewModel.uiState.value.isConfirmingClear)
        assertEquals(0, store.clearCount)
        assertEquals(1, store.recent.value.size)
    }

    @Test
    fun `取消清除退出确认态且不动数据`() {
        val store = FakeObservationStore(
            recent = listOf(observation("ads.example.com", blocked = true, packageName = BROWSER)),
        )
        val viewModel = viewModel(store)

        viewModel.dispatch(StatisticsUiIntent.RequestClear)
        viewModel.dispatch(StatisticsUiIntent.CancelClear)

        assertFalse(viewModel.uiState.value.isConfirmingClear)
        assertEquals(0, store.clearCount)
    }

    @Test
    fun `确认清除会清空存储并退出确认态`() = runTest(dispatcher) {
        val store = FakeObservationStore(
            recent = listOf(observation("ads.example.com", blocked = true, packageName = BROWSER)),
        )
        val viewModel = viewModel(store)

        viewModel.dispatch(StatisticsUiIntent.RequestClear)
        viewModel.dispatch(StatisticsUiIntent.ConfirmClear)

        assertEquals(1, store.clearCount)
        assertFalse(viewModel.uiState.value.isConfirmingClear)
        assertTrue(viewModel.uiState.value.applicationTraffic.isEmpty())
        assertFalse(viewModel.uiState.value.hasStatistics)
    }

    @Test
    fun `没有任何数据时没有可清除的内容`() {
        assertFalse(viewModel(FakeObservationStore()).uiState.value.hasStatistics)
    }

    @Test
    fun `累计计数非零时可清除`() {
        val store = FakeObservationStore(statistics = FilteringStatistics(observed = 12, blocked = 3))

        assertTrue(viewModel(store).uiState.value.hasStatistics)
    }

    @Test
    fun `关闭记录时状态里如实反映`() {
        val viewModel = viewModel(
            observationStore = FakeObservationStore(),
            privacyPolicy = PrivacyPolicy(isObservationLoggingEnabled = false),
        )

        assertFalse(viewModel.uiState.value.isObservationLoggingEnabled)
    }

    @Test
    fun `观测太少时不画趋势`() {
        val store = FakeObservationStore(
            recent = listOf(
                observationAt(offsetSeconds = 0, blocked = true),
                observationAt(offsetSeconds = OBSERVATION_INTERVAL_SECONDS, blocked = false),
            ),
        )

        val state = viewModel(store).uiState.value

        assertTrue(state.trafficTrend.isEmpty())
        assertEquals(2, state.windowObservationCount)
    }

    /**
     * 全部落在同一时刻时不能画趋势：分桶跨度为零，任何分法都只是把点堆在同一个桶里，
     * 那不是趋势。此时必须返回空，由界面说明原因。
     */
    @Test
    fun `全部落在同一时刻时不画趋势`() {
        val store = FakeObservationStore(
            recent = List(TREND_SAMPLE_SIZE) { index -> observationAt(offsetSeconds = 0, blocked = index % 2 == 0) },
        )

        assertTrue(viewModel(store).uiState.value.trafficTrend.isEmpty())
    }

    /** 分桶只改变分布，不改变总数：丢计数会让趋势图与上面的累计读数互相矛盾。 */
    @Test
    fun `趋势按时间等分且不丢计数`() {
        val store = FakeObservationStore(
            recent = List(TREND_SAMPLE_SIZE) { index ->
                observationAt(
                    offsetSeconds = index * OBSERVATION_INTERVAL_SECONDS,
                    blocked = index % BLOCK_EVERY == 0,
                )
            },
        )

        val state = viewModel(store).uiState.value

        assertEquals(TREND_BUCKET_COUNT, state.trafficTrend.size)
        assertEquals(TREND_SAMPLE_SIZE, state.windowObservationCount)
        assertEquals(EXPECTED_BLOCKED_IN_SAMPLE, state.trafficTrend.sumOf { bucket -> bucket.blocked })
        assertEquals(
            TREND_SAMPLE_SIZE - EXPECTED_BLOCKED_IN_SAMPLE,
            state.trafficTrend.sumOf { bucket -> bucket.relayed },
        )
    }

    private fun viewModel(
        observationStore: FakeObservationStore,
        protectedPackages: Set<String> = emptySet(),
        privacyPolicy: PrivacyPolicy = PrivacyPolicy.Default,
    ): StatisticsViewModel = StatisticsViewModel(
        observationStore = observationStore,
        ruleStore = FakeRuleStore(),
        installedApplicationSource = FakeApplicationSource(),
        protectedApplicationStore = FakeProtectedApplicationStore(protectedPackages),
        privacyPolicyStore = FakePrivacyPolicyStore(privacyPolicy),
    )

    private fun observation(host: String, blocked: Boolean, packageName: String?) = DomainObservation(
        at = Instant.parse("2026-09-24T10:15:30Z"),
        host = host,
        action = if (blocked) RuleAction.BLOCK else RuleAction.ALLOW,
        matchedRule = if (blocked) host else null,
        source = if (blocked) RuleSource.BUILTIN else null,
        packageName = packageName,
    )

    /** 带时间偏移的观测，用于趋势分桶。偏移不同才会落进不同的桶。 */
    private fun observationAt(offsetSeconds: Long, blocked: Boolean) = DomainObservation(
        at = Instant.parse("2026-09-24T10:15:30Z").plusSeconds(offsetSeconds),
        host = "ads.example.com",
        action = if (blocked) RuleAction.BLOCK else RuleAction.ALLOW,
        matchedRule = if (blocked) "ads.example.com" else null,
        source = if (blocked) RuleSource.BUILTIN else null,
        packageName = BROWSER,
    )

    private companion object {
        const val BROWSER = "com.example.browser"

        const val VIDEO = "com.example.video"

        const val UNKNOWN = "com.example.unlisted"

        /** 分桶数必须与 ViewModel 里的常量一致，否则「不丢计数」这条断言会失去意义。 */
        const val TREND_BUCKET_COUNT = 12

        const val TREND_SAMPLE_SIZE = 12

        const val OBSERVATION_INTERVAL_SECONDS = 10L

        const val BLOCK_EVERY = 3

        const val EXPECTED_BLOCKED_IN_SAMPLE = 4
    }
}

private val LABELS = mapOf(
    "com.example.browser" to "示例浏览器",
    "com.example.video" to "示例视频",
)

private class FakeObservationStore(
    recent: List<DomainObservation> = emptyList(),
    statistics: FilteringStatistics = FilteringStatistics(),
) : ObservationStore {
    private val mutableStatistics = MutableStateFlow(statistics)

    private val mutableRecent = MutableStateFlow(recent)

    var clearCount: Int = 0
        private set

    override val statistics: StateFlow<FilteringStatistics> = mutableStatistics

    override val recent: StateFlow<List<DomainObservation>> = mutableRecent

    override suspend fun load() = Unit

    override fun record(observation: DomainObservation) = Unit

    override suspend fun clear() {
        clearCount += 1
        mutableStatistics.value = FilteringStatistics()
        mutableRecent.value = emptyList()
    }
}

private class FakeRuleStore : RuleStore {
    private val mutableSnapshot = MutableStateFlow(RuleSnapshot.Empty)

    override val snapshot: StateFlow<RuleSnapshot> = mutableSnapshot

    override suspend fun load() = Unit

    override suspend fun applyBuiltinCatalog(hosts: Set<String>, version: Int) = Unit

    override suspend fun upsertUserRule(host: String, action: RuleAction): RuleEditResult = RuleEditResult.Applied

    override suspend fun removeUserRule(host: String, action: RuleAction): RuleEditResult = RuleEditResult.Applied

    override suspend fun setRuleEnabled(
        host: String,
        action: RuleAction,
        source: RuleSource,
        enabled: Boolean,
    ): RuleEditResult = RuleEditResult.Applied
}

private class FakeApplicationSource : InstalledApplicationSource {
    override suspend fun listInstalledApplications(): List<InstalledApplication> = emptyList()

    override suspend fun displayNames(packageNames: Set<String>): Map<String, String> = LABELS.filterKeys { name ->
        name in packageNames
    }

    override suspend fun hasPackageVisibility(): Boolean = true
}

private class FakePrivacyPolicyStore(initial: PrivacyPolicy = PrivacyPolicy.Default) : PrivacyPolicyStore {
    override val policy: StateFlow<PrivacyPolicy> = MutableStateFlow(initial)

    override suspend fun load() = Unit

    override suspend fun setObservationLoggingEnabled(enabled: Boolean) = Unit

    override suspend fun setObservationRetention(retention: ObservationRetention) = Unit

    override suspend fun setBlockedResponseMode(mode: BlockedResponseMode) = Unit
}

private class FakeProtectedApplicationStore(initial: Set<String>) : ProtectedApplicationStore {
    private val mutableProtected = MutableStateFlow(initial)

    override val protectedPackages: StateFlow<Set<String>> = mutableProtected

    override suspend fun load() = Unit

    override suspend fun setProtected(packageName: String, isProtected: Boolean) = Unit

    override suspend fun clearSelection() {
        mutableProtected.value = emptySet()
    }
}
