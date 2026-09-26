package xin.ctkqiang.nezha_cyber.ads_block.feature.home

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
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.ApplicationLabelCache
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.InstalledApplicationSource
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.DomainObservation
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.ObservationStore
import xin.ctkqiang.nezha_cyber.ads_block.domain.vpn.VpnController
import xin.ctkqiang.nezha_cyber.ads_block.domain.vpn.VpnStartResult

/**
 * 首页 ViewModel。
 *
 * 只依赖领域端口，因此拿不到 Context、Intent，也无法直接拉起系统对话框——
 * 需要授权时发出一次性效果，由界面与组合根完成（工程规则第 40.1、40.3 节）。
 *
 * 会话状态不在这里保存副本：它由服务写入、经端口透传，ViewModel 只做映射，
 * 避免出现两处状态互相打架。
 *
 * 观测流只用来取「最近一条被拦下的记录」；统计口径与聚合全部留在统计页，
 * 首页不重复计算——两处各算一遍，迟早会出现两个数字对不上。
 */
class HomeViewModel(
    private val vpnController: VpnController,
    observationStore: ObservationStore,
    installedApplicationSource: InstalledApplicationSource,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(HomeUiState())

    private val mutableEffect = MutableSharedFlow<HomeUiEffect>(extraBufferCapacity = EFFECT_BUFFER_CAPACITY)

    private val labelCache = ApplicationLabelCache(installedApplicationSource)

    val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()

    val effect: SharedFlow<HomeUiEffect> = mutableEffect.asSharedFlow()

    init {
        viewModelScope.launch {
            vpnController.session.collect { session ->
                mutableUiState.update { current -> current.copy(session = session) }
            }
        }
        viewModelScope.launch {
            observationStore.recent.collect { observations ->
                mutableUiState.update { current -> current.copy(latestBlocked = observations.latestBlocked()) }
            }
        }
    }

    fun dispatch(intent: HomeUiIntent) {
        when (intent) {
            HomeUiIntent.ToggleProtection -> toggleProtection()
            is HomeUiIntent.AuthorizationResult -> onAuthorizationResult(intent.granted)
        }
    }

    private fun toggleProtection() {
        viewModelScope.launch {
            if (uiState.value.session.isActive) {
                vpnController.stop()
                return@launch
            }
            if (vpnController.start() is VpnStartResult.PermissionDenied) {
                // 授权被拒不是缺陷：标记需要授权并请界面拉起系统对话框，用户授权后会自动续跑。
                mutableUiState.update { it.copy(authorizationRequired = true) }
                mutableEffect.tryEmit(HomeUiEffect.RequestVpnAuthorization)
            } else {
                mutableUiState.update { it.copy(authorizationRequired = false) }
            }
        }
    }

    private fun onAuthorizationResult(granted: Boolean) {
        if (!granted) {
            mutableUiState.update { it.copy(authorizationRequired = true) }
            return
        }
        mutableUiState.update { it.copy(authorizationRequired = false) }
        toggleProtection()
    }

    /**
     * 取最近一条被拦下的记录。
     *
     * [ObservationStore.recent] 最新的在前，因此第一条被拦下的就是最近的那一次。
     * 归属解析走既有的名字缓存，与网络活动页共用同一份实现：同一个应用在两处必须叫同一个名字。
     *
     * 解析不到名字时留空由界面显示「未知来源」，不退回包名——包名在那张卡片上是噪音，
     * 而「未知」是如实结论。
     */
    private suspend fun List<DomainObservation>.latestBlocked(): LatestBlocked? {
        val latest = firstOrNull { observation -> observation.isBlocked }
        return if (latest == null) {
            null
        } else {
            val packageName = latest.packageName
            val label = packageName?.let { name -> labelCache.resolve(setOf(name))[name] }
            LatestBlocked(host = latest.host, appLabel = label)
        }
    }

    companion object {
        private const val EFFECT_BUFFER_CAPACITY = 1

        fun factory(
            vpnController: VpnController,
            observationStore: ObservationStore,
            installedApplicationSource: InstalledApplicationSource,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    vpnController = vpnController,
                    observationStore = observationStore,
                    installedApplicationSource = installedApplicationSource,
                )
            }
        }
    }
}
