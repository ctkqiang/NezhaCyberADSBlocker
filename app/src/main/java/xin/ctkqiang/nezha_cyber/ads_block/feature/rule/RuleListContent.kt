package xin.ctkqiang.nezha_cyber.ads_block.feature.rule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import xin.ctkqiang.nezha_cyber.ads_block.R
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.KeywordPolicyStore
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleAction
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleSource
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleStore
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaListScaffold
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaPillButton
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaSurfaceCard
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaSwitch
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaTextField
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.LocalKeywordPolicyStore
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.LocalRuleStore
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme

private const val BUILTIN_MATCH_NOTE_LIMIT = 50

private const val USER_SECTION_KEY = "rule-user-section"

private const val USER_EMPTY_KEY = "rule-user-empty"

private const val BUILTIN_SECTION_KEY = "rule-builtin-section"

private const val BUILTIN_SEARCH_KEY = "rule-builtin-search"

private const val BUILTIN_EMPTY_KEY = "rule-builtin-empty"

private const val BUILTIN_LIMIT_KEY = "rule-builtin-limit"

private const val BUILTIN_NOTE_KEY = "rule-builtin-note"

private const val ALLOW_NOTE_KEY = "rule-allow-note"

/**
 * 规则页内容。黑名单与白名单共用它，只有目标动作不同。
 *
 * `viewModel` 必须带 key：两个页面同属一个 Activity，默认按 ViewModel 类型取实例，
 * 不带 key 时白名单页会拿到黑名单页的同一个 ViewModel，于是两页显示同一份规则。
 *
 * 列表分段拆成 [LazyListScope] 的私有扩展，而不是全部堆在这一个函数里：
 * 每段各自独立、键也各自独立，分开之后每段的意图一眼可读。
 *
 * 关键词兜底只出现在黑名单页：它只用于阻断，放到白名单页会暗示它也能放行。
 */
@Composable
internal fun RuleListContent(
    action: RuleAction,
    modifier: Modifier = Modifier,
    ruleStore: RuleStore = LocalRuleStore.current,
    keywordPolicyStore: KeywordPolicyStore = LocalKeywordPolicyStore.current,
) {
    val viewModel: RuleListViewModel = viewModel(
        key = action.name,
        factory = RuleListViewModel.factory(action, ruleStore, keywordPolicyStore),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NezhaListScaffold(
        modifier = modifier,
        header = { RuleDraftSection(uiState = uiState, onIntent = viewModel::dispatch) },
    ) {
        userRulesSection(action = action, uiState = uiState, onIntent = viewModel::dispatch)
        if (action == RuleAction.ALLOW) {
            item(key = ALLOW_NOTE_KEY) {
                Note(text = stringResource(R.string.rules_allow_note))
            }
        } else {
            keywordRuleSection(uiState = uiState, onIntent = viewModel::dispatch)
            builtinSection(uiState = uiState, onIntent = viewModel::dispatch)
        }
    }
}

private fun LazyListScope.userRulesSection(
    action: RuleAction,
    uiState: RuleListUiState,
    onIntent: (RuleListUiIntent) -> Unit,
) {
    item(key = USER_SECTION_KEY) {
        SectionTitle(
            title = stringResource(
                if (action == RuleAction.BLOCK) {
                    R.string.rules_user_block_section
                } else {
                    R.string.rules_user_allow_section
                },
                uiState.userRules.size,
            ),
        )
    }
    if (uiState.userRules.isEmpty()) {
        item(key = USER_EMPTY_KEY) {
            Note(
                text = stringResource(
                    if (action == RuleAction.BLOCK) {
                        R.string.rules_user_block_empty
                    } else {
                        R.string.rules_user_allow_empty
                    },
                ),
            )
        }
    } else {
        items(items = uiState.userRules, key = { row -> "user-${row.host}" }) { row ->
            RuleRowCard(row = row, onIntent = onIntent, deletable = true)
        }
    }
}

private fun LazyListScope.builtinSection(uiState: RuleListUiState, onIntent: (RuleListUiIntent) -> Unit) {
    item(key = BUILTIN_SECTION_KEY) {
        SectionTitle(
            title = stringResource(R.string.rules_builtin_section),
            subtitle = stringResource(
                R.string.statistics_builtin_rules_value,
                uiState.builtinVersion,
                uiState.builtinTotalCount,
            ),
        )
    }
    item(key = BUILTIN_SEARCH_KEY) {
        NezhaTextField(
            value = uiState.query,
            onValueChange = { query -> onIntent(RuleListUiIntent.QueryChanged(query)) },
            placeholder = stringResource(R.string.rules_builtin_search_hint),
        )
    }
    if (uiState.query.isNotBlank()) {
        if (uiState.builtinMatches.isEmpty()) {
            item(key = BUILTIN_EMPTY_KEY) {
                Note(text = stringResource(R.string.rules_builtin_search_empty))
            }
        } else {
            items(items = uiState.builtinMatches, key = { row -> "builtin-${row.host}" }) { row ->
                RuleRowCard(row = row, onIntent = onIntent, deletable = false)
            }
            if (uiState.builtinMatches.size >= BUILTIN_MATCH_NOTE_LIMIT) {
                item(key = BUILTIN_LIMIT_KEY) {
                    Note(text = stringResource(R.string.rules_builtin_limit_note, BUILTIN_MATCH_NOTE_LIMIT))
                }
            }
        }
    }
    item(key = BUILTIN_NOTE_KEY) {
        Note(text = stringResource(R.string.rules_builtin_override_note))
    }
}

@Composable
private fun RuleDraftSection(uiState: RuleListUiState, onIntent: (RuleListUiIntent) -> Unit) {
    val palette = NezhaTheme.palette
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NezhaDimens.blockGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NezhaTextField(
            value = uiState.draft,
            onValueChange = { draft -> onIntent(RuleListUiIntent.DraftChanged(draft)) },
            placeholder = stringResource(R.string.rules_add_hint),
            onSubmit = { onIntent(RuleListUiIntent.SubmitDraft) },
            modifier = Modifier.weight(1f),
        )
        NezhaPillButton(
            text = stringResource(R.string.rules_add_action),
            enabled = uiState.draft.isNotBlank(),
            onClick = { onIntent(RuleListUiIntent.SubmitDraft) },
        )
    }
    uiState.message?.let { message ->
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        BasicText(
            text = stringResource(message.textRes()),
            style = NezhaTheme.typography.caption.copy(
                color = if (message == RuleListMessage.Added) palette.brand else palette.textPrimary,
            ),
        )
    }
}

/** 提交结果的文案。关键词区段也要用它，因此是同包内可见而不是文件私有。 */
internal fun RuleListMessage.textRes(): Int = when (this) {
    RuleListMessage.Added -> R.string.rules_message_added
    RuleListMessage.InvalidHost -> R.string.rules_message_invalid
    RuleListMessage.InvalidKeyword -> R.string.rules_message_invalid_keyword
    RuleListMessage.AlreadyExists -> R.string.rules_message_exists
    RuleListMessage.NotFound -> R.string.rules_message_not_found
}

@Composable
private fun RuleRowCard(row: RuleRow, onIntent: (RuleListUiIntent) -> Unit, deletable: Boolean) {
    val palette = NezhaTheme.palette
    NezhaSurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = row.host,
                    style = NezhaTheme.typography.body.copy(
                        color = if (row.enabled) palette.textPrimary else palette.textSecondary,
                    ),
                )
                Spacer(modifier = Modifier.height(NezhaDimens.tightGap))
                BasicText(
                    text = stringResource(
                        if (row.source == RuleSource.BUILTIN) {
                            R.string.rules_source_builtin
                        } else {
                            R.string.rules_source_user
                        },
                    ),
                    style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
                )
            }
            if (deletable) {
                NezhaPillButton(
                    text = stringResource(R.string.rules_delete_action),
                    onClick = { onIntent(RuleListUiIntent.DeleteRule(row.host)) },
                )
            }
            NezhaSwitch(
                checked = row.enabled,
                onCheckedChange = { enabled ->
                    onIntent(RuleListUiIntent.ToggleRule(row.host, row.source, enabled))
                },
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String? = null) {
    val palette = NezhaTheme.palette
    Column(modifier = Modifier.fillMaxWidth()) {
        BasicText(
            text = title,
            style = NezhaTheme.typography.title.copy(color = palette.textPrimary),
        )
        subtitle?.let { text ->
            Spacer(modifier = Modifier.height(NezhaDimens.tightGap))
            BasicText(
                text = text,
                style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
            )
        }
    }
}

/** 区段下方的一行说明。关键词区段与内置清单区段共用它。 */
@Composable
internal fun Note(text: String) {
    BasicText(
        text = text,
        style = NezhaTheme.typography.caption.copy(color = NezhaTheme.palette.textSecondary),
    )
}
