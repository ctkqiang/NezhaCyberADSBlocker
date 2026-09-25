package xin.ctkqiang.nezha_cyber.ads_block.feature.rule

import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleAction
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleSource

/**
 * 规则页状态与意图。
 *
 * 黑名单与白名单共用这一套契约，区别只在于目标动作 [RuleAction]：两页需要的能力完全一致
 * （查看、新增、启停、删除、搜索内置清单），分成两套只会产生两份会各自漂移的实现。
 *
 * [keywordRules] 只在黑名单页有意义（关键词只用于阻断），白名单页保持默认值且不渲染它。
 */
data class RuleListUiState(
    val userRules: List<RuleRow> = emptyList(),
    val builtinMatches: List<RuleRow> = emptyList(),
    val builtinTotalCount: Int = 0,
    val builtinVersion: Int = 0,
    val draft: String = "",
    val query: String = "",
    val keywordRules: KeywordRules = KeywordRules(),
    val keywordDraft: String = "",
    val message: RuleListMessage? = null,
    val keywordMessage: RuleListMessage? = null,
    val isLoading: Boolean = true,
)

data class RuleRow(val host: String, val enabled: Boolean, val source: RuleSource)

/**
 * 关键词兜底规则的展示形态。
 *
 * [enabled] 为 false 时规则引擎完全不执行这一档，但 [keywords] 仍然保留：关掉不等于清空，
 * 再打开时应当原样恢复。排序在 ViewModel 里做一次，界面每次重组不必再排。
 */
data class KeywordRules(val enabled: Boolean = true, val keywords: List<String> = emptyList())

/**
 * 上一次提交的结果。
 *
 * 用状态而不是一次性提示承载：它描述的是「当前这次输入的结果」，会随输入变化而被清除，
 * 因此属于状态；弹出式提示才需要一次性效果，而这一页不必打断用户。
 *
 * 域名输入与关键词输入各有各的消息字段，因此消息类型合并成一套：
 * 两条输入同时出错时，各自显示各自的，不会互相覆盖。
 */
sealed interface RuleListMessage {
    data object Added : RuleListMessage

    data object InvalidHost : RuleListMessage

    /** 关键词必须是单个 DNS 标签，与域名规则的要求不同，因此单独一档。 */
    data object InvalidKeyword : RuleListMessage

    data object AlreadyExists : RuleListMessage

    data object NotFound : RuleListMessage
}

sealed interface RuleListUiIntent {
    data class DraftChanged(val draft: String) : RuleListUiIntent

    data object SubmitDraft : RuleListUiIntent

    data class QueryChanged(val query: String) : RuleListUiIntent

    data class ToggleRule(val host: String, val source: RuleSource, val enabled: Boolean) : RuleListUiIntent

    data class DeleteRule(val host: String) : RuleListUiIntent

    data class KeywordDraftChanged(val draft: String) : RuleListUiIntent

    data object SubmitKeywordDraft : RuleListUiIntent

    data class DeleteKeyword(val keyword: String) : RuleListUiIntent

    data class ToggleKeywordBlocking(val enabled: Boolean) : RuleListUiIntent
}
