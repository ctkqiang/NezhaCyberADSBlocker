package xin.ctkqiang.nezha_cyber.ads_block.feature.home

import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.DomainObservation
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.FilteringStatistics
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.ObservationStore
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleAction
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleSource
import xin.ctkqiang.nezha_cyber.ads_block.domain.vpn.VpnController
import xin.ctkqiang.nezha_cyber.ads_block.domain.vpn.VpnSessionState
import xin.ctkqiang.nezha_cyber.ads_block.domain.vpn.VpnStartResult

private const val BROWSER = "com.example.browser"

private const val BROWSER_LABEL = "示例浏览器"

/**
 * 首页 ViewModel 的行为测试。
 *
 * 锁住的是几类会被后续改动误伤的约定：授权被拒时下发一次性授权效果、授权通过后自动续跑启动、
 * 运行中点按主按钮走停止分支，以及「最近拦截」取的是**最新的那次拦截**而不是最近一条观测。
 * 前三条在真机上不好复现（需要反复拒绝系统授权），最后一条只会在有转发流量时才看错，
 * 因此都必须由单元测试保证。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
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
    fun `初始状态为未启动且不需要授权`() {
        val viewModel = homeViewModel(FakeVpnController())

        assertEquals(VpnSessionState.Stopped, viewModel.uiState.value.session)
        assertFalse(viewModel.uiState.value.authorizationRequired)
        assertNull(viewModel.uiState.value.latestBlocked)
    }

    @Test
    fun `授权被拒时标记需要授权并下发一次性效果`() = runTest(dispatcher) {
        val controller = FakeVpnController(VpnStartResult.PermissionDenied)
        val viewModel = homeViewModel(controller)
        val effects = mutableListOf<HomeUiEffect>()
        val collector = launch { viewModel.effect.collect { effects += it } }

        viewModel.dispatch(HomeUiIntent.ToggleProtection)

        assertTrue(viewModel.uiState.value.authorizationRequired)
        assertEquals(listOf(HomeUiEffect.RequestVpnAuthorization), effects)
        assertEquals(1, controller.startCount)
        collector.cancel()
    }

    @Test
    fun `授权通过后自动续跑启动并清除授权标记`() = runTest(dispatcher) {
        val controller = FakeVpnController(VpnStartResult.PermissionDenied, VpnStartResult.Started)
        val viewModel = homeViewModel(controller)

        viewModel.dispatch(HomeUiIntent.ToggleProtection)
        assertTrue(viewModel.uiState.value.authorizationRequired)

        viewModel.dispatch(HomeUiIntent.AuthorizationResult(granted = true))

        assertFalse(viewModel.uiState.value.authorizationRequired)
        assertEquals(2, controller.startCount)
        assertTrue(viewModel.uiState.value.session is VpnSessionState.Running)
    }

    @Test
    fun `授权被拒时不再重复请求启动`() {
        val controller = FakeVpnController(VpnStartResult.PermissionDenied)
        val viewModel = homeViewModel(controller)

        viewModel.dispatch(HomeUiIntent.ToggleProtection)
        viewModel.dispatch(HomeUiIntent.AuthorizationResult(granted = false))

        assertTrue(viewModel.uiState.value.authorizationRequired)
        assertEquals(1, controller.startCount)
    }

    @Test
    fun `运行中点按主按钮走停止分支`() {
        val controller = FakeVpnController(initialState = VpnSessionState.Running(Instant.EPOCH))
        val viewModel = homeViewModel(controller)

        viewModel.dispatch(HomeUiIntent.ToggleProtection)

        assertEquals(1, controller.stopCount)
        assertEquals(0, controller.startCount)
        assertEquals(VpnSessionState.Stopped, viewModel.uiState.value.session)
    }

    /**
     * 最近观测里最新的一条是**转发**，最新的一条**拦截**在它后面。取的是后者：
     * 「最近拦截」要的是最近一次被拦下的，不是最近一次查询。
     */
    @Test
    fun `最近拦截取的是最新的那次拦截而不是最新那条观测`() {
        val viewModel = homeViewModel(
            controller = FakeVpnController(),
            recent = listOf(
                observation(host = "cdn.example.net", blocked = false),
                observation(host = "ads.example.com", blocked = true),
                observation(host = "tracker.example.net", blocked = true),
            ),
        )

        val latest = viewModel.uiState.value.latestBlocked

        assertEquals("ads.example.com", latest?.host)
        assertEquals(BROWSER_LABEL, latest?.appLabel)
    }

    @Test
    fun `没有任何拦截时最近拦截为空`() {
        val viewModel = homeViewModel(
            controller = FakeVpnController(),
            recent = listOf(observation(host = "cdn.example.net", blocked = false)),
        )

        assertNull(viewModel.uiState.value.latestBlocked)
    }

    /** 归属不到应用时不编一个名字，留给界面显示「未知来源」。 */
    @Test
    fun `无法归属时只给域名不给应用名`() {
        val viewModel = homeViewModel(
            controller = FakeVpnController(),
            recent = listOf(observation(host = "ads.example.com", blocked = true, packageName = null)),
        )

        val latest = viewModel.uiState.value.latestBlocked

        assertEquals("ads.example.com", latest?.host)
        assertNull(latest?.appLabel)
    }

    /** 两个新端口对绝大多数用例都无关紧要，统一在这里给替身，避免每个用例重复构造。 */
    private fun homeViewModel(controller: VpnController, recent: List<DomainObservation> = emptyList()) = HomeViewModel(
        vpnController = controller,
        observationStore = FakeObservationStore(recent),
        installedApplicationSource = FakeApplicationSource(),
    )

    private fun observation(host: String, blocked: Boolean, packageName: String? = BROWSER) = DomainObservation(
        at = Instant.parse("2026-09-24T10:15:30Z"),
        host = host,
        action = if (blocked) RuleAction.BLOCK else RuleAction.ALLOW,
        matchedRule = if (blocked) host else null,
        source = if (blocked) RuleSource.BUILTIN else null,
        packageName = packageName,
    )
}

/**
 * [VpnController] 的测试替身。
 *
 * 按传入顺序逐个返回启动结果，队列耗尽后一律返回成功，这样同一个替身既能表达
 * 「先被拒再通过」，也能表达普通的成功路径。
 */
private class FakeVpnController(
    vararg startResults: VpnStartResult,
    initialState: VpnSessionState = VpnSessionState.Stopped,
) : VpnController {
    private val pendingResults = ArrayDeque(startResults.toList())

    private val mutableSession = MutableStateFlow(initialState)

    override val session: StateFlow<VpnSessionState> = mutableSession

    var startCount: Int = 0
        private set

    var stopCount: Int = 0
        private set

    override suspend fun isAuthorized(): Boolean = true

    override suspend fun start(): VpnStartResult {
        startCount += 1
        val result = pendingResults.removeFirstOrNull() ?: VpnStartResult.Started
        if (result is VpnStartResult.Started) {
            mutableSession.value = VpnSessionState.Running(Instant.now())
        }
        return result
    }

    override suspend fun stop() {
        stopCount += 1
        mutableSession.value = VpnSessionState.Stopped
    }
}

/**
 * 观测存储的测试替身。
 *
 * 只实现首页用得到的两项：`recent` 与 `statistics`。首页不消费统计，给默认值即可——
 * 让替身只做被用到的事，比实现整套接口更能暴露「谁在依赖什么」。
 */
private class FakeObservationStore(recent: List<DomainObservation>) : ObservationStore {
    private val mutableRecent = MutableStateFlow(recent)

    override val statistics: StateFlow<FilteringStatistics> = MutableStateFlow(FilteringStatistics())

    override val recent: StateFlow<List<DomainObservation>> = mutableRecent

    override suspend fun load() = Unit

    override fun record(observation: DomainObservation) = Unit

    override suspend fun clear() {
        mutableRecent.value = emptyList()
    }
}

private class FakeApplicationSource : InstalledApplicationSource {
    override suspend fun listInstalledApplications(): List<InstalledApplication> = emptyList()

    override suspend fun displayNames(packageNames: Set<String>): Map<String, String> =
        if (BROWSER in packageNames) mapOf(BROWSER to BROWSER_LABEL) else emptyMap()

    override suspend fun hasPackageVisibility(): Boolean = true
}
