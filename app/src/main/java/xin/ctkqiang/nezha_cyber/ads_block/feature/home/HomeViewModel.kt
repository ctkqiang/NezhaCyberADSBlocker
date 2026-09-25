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
import xin.ctkqiang.nezha_cyber.ads_block.domain.vpn.VpnController
import xin.ctkqiang.nezha_cyber.ads_block.domain.vpn.VpnStartResult

/**
 * 首页 ViewModel。
 *
 * 只依赖领域端口 [VpnController]，因此拿不到 Context、Intent，也无法直接拉起系统对话框——
 * 需要授权时发出一次性效果，由界面与组合根完成（工程规则第 40.1、40.3 节）。
 *
 * 会话状态不在这里保存副本：它由服务写入、经端口透传，ViewModel 只做映射，
 * 避免出现两处状态互相打架。
 */
class HomeViewModel(private val vpnController: VpnController) : ViewModel() {
    private val mutableUiState = MutableStateFlow(HomeUiState())

    private val mutableEffect = MutableSharedFlow<HomeUiEffect>(extraBufferCapacity = EFFECT_BUFFER_CAPACITY)

    val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()

    val effect: SharedFlow<HomeUiEffect> = mutableEffect.asSharedFlow()

    init {
        viewModelScope.launch {
            vpnController.session.collect { session ->
                mutableUiState.update { current -> current.copy(session = session) }
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

    companion object {
        private const val EFFECT_BUFFER_CAPACITY = 1

        fun factory(vpnController: VpnController): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(vpnController) }
        }
    }
}
