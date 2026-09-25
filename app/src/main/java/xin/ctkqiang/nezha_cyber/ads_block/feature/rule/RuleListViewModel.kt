package xin.ctkqiang.nezha_cyber.ads_block.feature.rule

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
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.DomainRule
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.KeywordBlockingPolicy
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.KeywordPolicyStore
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleAction
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleEditResult
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleSnapshot
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleSource
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleStore

/** 内置清单的搜索结果上限。清单有数万条，全量渲染既无意义也不可用。 */
private const val BUILTIN_MATCH_LIMIT = 50

/**
 * 规则页 ViewModel。
 *
 * [action] 决定这一页是黑名单还是白名单，其余逻辑完全相同。
 *
 * 内置清单只做「搜索 + 启停」而不做「编辑」：清单随应用分发、运行时只读（第 39.1 节），
 * 用户能改的只有覆写状态，因此这里不提供删除入口——删掉一条内置规则既做不到，也会误导用户。
 *
 * 关键词兜底规则是第三档来源，它同样只用于阻断，因此只在黑名单页订阅与展示。
 * 规则引擎的优先级阶梯（第 10 节）有四档，页面只呈现两档会让用户无法回答
 * 「这个域名到底是被什么拦下的」（第 32 节要求对拦截原因保持透明）。
 */
class RuleListViewModel(
    private val action: RuleAction,
    private val ruleStore: RuleStore,
    private val keywordPolicyStore: KeywordPolicyStore,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(RuleListUiState())

    private var snapshot: RuleSnapshot = RuleSnapshot.Empty

    val uiState: StateFlow<RuleListUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            ruleStore.snapshot.collect { updated ->
                snapshot = updated
                publish()
            }
        }
        if (action == RuleAction.BLOCK) {
            viewModelScope.launch {
                keywordPolicyStore.policy.collect { policy -> publishKeywords(policy) }
            }
        }
    }

    fun dispatch(intent: RuleListUiIntent) {
        when (intent) {
            is RuleListUiIntent.DraftChanged -> mutableUiState.update {
                // 输入一变，上一次的提交结果就不再是「当前这次输入的结果」，必须清掉。
                it.copy(draft = intent.draft, message = null)
            }
            RuleListUiIntent.SubmitDraft -> submit()
            is RuleListUiIntent.QueryChanged -> {
                mutableUiState.update { it.copy(query = intent.query) }
                publish()
            }
            is RuleListUiIntent.ToggleRule -> toggle(intent)
            is RuleListUiIntent.DeleteRule -> delete(intent.host)
            is RuleListUiIntent.KeywordDraftChanged -> mutableUiState.update {
                it.copy(keywordDraft = intent.draft, keywordMessage = null)
            }
            RuleListUiIntent.SubmitKeywordDraft -> submitKeyword()
            is RuleListUiIntent.DeleteKeyword -> viewModelScope.launch {
                keywordPolicyStore.removeKeyword(intent.keyword)
            }
            is RuleListUiIntent.ToggleKeywordBlocking -> viewModelScope.launch {
                keywordPolicyStore.setEnabled(intent.enabled)
            }
        }
    }

    private fun submit() {
        val draft = uiState.value.draft
        viewModelScope.launch {
            when (ruleStore.upsertUserRule(draft, action)) {
                RuleEditResult.Applied -> mutableUiState.update {
                    it.copy(draft = "", message = RuleListMessage.Added)
                }
                RuleEditResult.InvalidHost -> mutableUiState.update {
                    it.copy(message = RuleListMessage.InvalidHost)
                }
                RuleEditResult.AlreadyExists -> mutableUiState.update {
                    it.copy(message = RuleListMessage.AlreadyExists)
                }
                RuleEditResult.NotFound -> mutableUiState.update {
                    it.copy(message = RuleListMessage.NotFound)
                }
            }
        }
    }

    private fun submitKeyword() {
        val draft = uiState.value.keywordDraft
        viewModelScope.launch {
            when (keywordPolicyStore.addKeyword(draft)) {
                RuleEditResult.Applied -> mutableUiState.update {
                    it.copy(keywordDraft = "", keywordMessage = RuleListMessage.Added)
                }
                RuleEditResult.InvalidHost -> mutableUiState.update {
                    it.copy(keywordMessage = RuleListMessage.InvalidKeyword)
                }
                RuleEditResult.AlreadyExists -> mutableUiState.update {
                    it.copy(keywordMessage = RuleListMessage.AlreadyExists)
                }
                RuleEditResult.NotFound -> mutableUiState.update {
                    it.copy(keywordMessage = RuleListMessage.NotFound)
                }
            }
        }
    }

    private fun toggle(intent: RuleListUiIntent.ToggleRule) {
        viewModelScope.launch {
            ruleStore.setRuleEnabled(intent.host, action, intent.source, intent.enabled)
        }
    }

    private fun delete(host: String) {
        viewModelScope.launch {
            ruleStore.removeUserRule(host, action)
        }
    }

    private fun publish() {
        val userRules = snapshot.rules.asSequence()
            .filter { rule -> rule.source == RuleSource.USER && rule.action == action }
            .sortedBy { rule -> rule.host }
            .map { rule -> rule.toRow() }
            .toList()
        val query = uiState.value.query.trim()
        val builtinMatches = if (action != RuleAction.BLOCK || query.isEmpty()) {
            emptyList()
        } else {
            snapshot.rules.asSequence()
                .filter { rule -> rule.source == RuleSource.BUILTIN && rule.host.contains(query, ignoreCase = true) }
                .take(BUILTIN_MATCH_LIMIT)
                .map { rule -> rule.toRow() }
                .toList()
        }
        mutableUiState.update { current ->
            current.copy(
                userRules = userRules,
                builtinMatches = builtinMatches,
                builtinTotalCount = snapshot.builtinRuleCount,
                builtinVersion = snapshot.builtinVersion,
                isLoading = false,
            )
        }
    }

    private fun publishKeywords(policy: KeywordBlockingPolicy) {
        mutableUiState.update { current ->
            current.copy(
                keywordRules = KeywordRules(enabled = policy.enabled, keywords = policy.keywords.sorted()),
            )
        }
    }

    private fun DomainRule.toRow(): RuleRow = RuleRow(host = host, enabled = enabled, source = source)

    companion object {
        fun factory(
            action: RuleAction,
            ruleStore: RuleStore,
            keywordPolicyStore: KeywordPolicyStore,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { RuleListViewModel(action, ruleStore, keywordPolicyStore) }
        }
    }
}
