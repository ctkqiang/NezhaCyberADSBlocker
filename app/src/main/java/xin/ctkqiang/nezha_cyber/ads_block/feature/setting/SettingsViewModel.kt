package xin.ctkqiang.nezha_cyber.ads_block.feature.setting

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
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.PrivacyPolicyStore
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleStore

/**
 * 设置 ViewModel。
 *
 * 两个来源各改各的字段：规则存储只提供规模信息（只读），隐私策略提供用户可改的三项。
 * 应用版本号由界面从平台取：那是平台资源，不属于领域状态（工程规则第 41.8 节）。
 *
 * 写入不在这里做乐观更新：状态始终来自 [PrivacyPolicyStore.policy] 的回流，
 * 因此界面上显示的一定是**已经落盘**的那一份值，而不是「点了之后本以为是那样」。
 */
class SettingsViewModel(ruleStore: RuleStore, private val privacyPolicyStore: PrivacyPolicyStore) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SettingsUiState())

    val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()

    init {
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
            privacyPolicyStore.policy.collect { policy ->
                mutableUiState.update { current ->
                    current.copy(
                        isObservationLoggingEnabled = policy.isObservationLoggingEnabled,
                        observationRetention = policy.observationRetention,
                        blockedResponseMode = policy.blockedResponseMode,
                    )
                }
            }
        }
    }

    fun dispatch(intent: SettingsUiIntent) {
        when (intent) {
            is SettingsUiIntent.SetObservationLoggingEnabled -> viewModelScope.launch {
                privacyPolicyStore.setObservationLoggingEnabled(intent.enabled)
            }

            is SettingsUiIntent.SetObservationRetention -> viewModelScope.launch {
                privacyPolicyStore.setObservationRetention(intent.retention)
            }

            is SettingsUiIntent.SetBlockedResponseMode -> viewModelScope.launch {
                privacyPolicyStore.setBlockedResponseMode(intent.mode)
            }
        }
    }

    companion object {
        fun factory(ruleStore: RuleStore, privacyPolicyStore: PrivacyPolicyStore): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { SettingsViewModel(ruleStore = ruleStore, privacyPolicyStore = privacyPolicyStore) }
            }
    }
}
