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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import xin.ctkqiang.nezha_cyber.ads_block.domain.vpn.VpnController
import xin.ctkqiang.nezha_cyber.ads_block.domain.vpn.VpnSessionState
import xin.ctkqiang.nezha_cyber.ads_block.domain.vpn.VpnStartResult

/**
 * 首页 ViewModel 的行为测试。
 *
 * 锁住的是三条会被后续改动误伤的约定：授权被拒时下发一次性授权效果、授权通过后自动续跑启动、
 * 运行中点按主按钮走停止分支。这些分支在真机上不好复现（需要反复拒绝系统授权），
 * 因此必须由单元测试保证。
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
        val viewModel = HomeViewModel(FakeVpnController())

        assertEquals(VpnSessionState.Stopped, viewModel.uiState.value.session)
        assertFalse(viewModel.uiState.value.authorizationRequired)
    }

    @Test
    fun `授权被拒时标记需要授权并下发一次性效果`() = runTest(dispatcher) {
        val controller = FakeVpnController(VpnStartResult.PermissionDenied)
        val viewModel = HomeViewModel(controller)
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
        val viewModel = HomeViewModel(controller)

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
        val viewModel = HomeViewModel(controller)

        viewModel.dispatch(HomeUiIntent.ToggleProtection)
        viewModel.dispatch(HomeUiIntent.AuthorizationResult(granted = false))

        assertTrue(viewModel.uiState.value.authorizationRequired)
        assertEquals(1, controller.startCount)
    }

    @Test
    fun `运行中点按主按钮走停止分支`() {
        val controller = FakeVpnController(initialState = VpnSessionState.Running(Instant.EPOCH))
        val viewModel = HomeViewModel(controller)

        viewModel.dispatch(HomeUiIntent.ToggleProtection)

        assertEquals(1, controller.stopCount)
        assertEquals(0, controller.startCount)
        assertEquals(VpnSessionState.Stopped, viewModel.uiState.value.session)
    }
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
