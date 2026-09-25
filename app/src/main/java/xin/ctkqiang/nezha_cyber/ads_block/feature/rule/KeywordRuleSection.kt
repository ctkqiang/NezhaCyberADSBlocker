package xin.ctkqiang.nezha_cyber.ads_block.feature.rule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import xin.ctkqiang.nezha_cyber.ads_block.R
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaPillButton
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaSurfaceCard
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaSwitch
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaTextField
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme

private const val KEYWORD_SECTION_KEY = "rule-keyword-section"

private const val KEYWORD_EMPTY_KEY = "rule-keyword-empty"

private const val KEYWORD_NOTE_KEY = "rule-keyword-note"

/**
 * 关键词兜底规则区段。
 *
 * 它排在用户规则之后、内置清单之前，与规则引擎的判定顺序同序（工程规则第 10 节）：
 * 用户显式放行 → 用户显式阻断 → 内置清单 → 关键词。页面上「从上往下」的顺序就是实际的
 * 优先级顺序，用户才能据此理解某个域名为什么被拦（第 32 节要求拦截原因可解释）。
 *
 * 单独成一个文件而不是留在规则页内容里：这一段的开关、增删与列表都与域名规则无关，
 * 它们是两种数据模型，混在一个文件里只会让「哪一段属于哪种模型」在阅读时消失。
 */
internal fun LazyListScope.keywordRuleSection(uiState: RuleListUiState, onIntent: (RuleListUiIntent) -> Unit) {
    item(key = KEYWORD_SECTION_KEY) {
        KeywordHeader(uiState = uiState, onIntent = onIntent)
    }
    if (uiState.keywordRules.keywords.isEmpty()) {
        item(key = KEYWORD_EMPTY_KEY) {
            Note(text = stringResource(R.string.rules_keyword_empty))
        }
    } else {
        items(items = uiState.keywordRules.keywords, key = { keyword -> "keyword-$keyword" }) { keyword ->
            KeywordRow(keyword = keyword, onIntent = onIntent)
        }
    }
    item(key = KEYWORD_NOTE_KEY) {
        Note(text = stringResource(R.string.rules_keyword_note))
    }
}

/** 关键词区段的头部：标题与开关、新增输入、最近一次提交结果。 */
@Composable
private fun KeywordHeader(uiState: RuleListUiState, onIntent: (RuleListUiIntent) -> Unit) {
    val palette = NezhaTheme.palette
    NezhaSurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = stringResource(R.string.rules_keyword_section, uiState.keywordRules.keywords.size),
                modifier = Modifier.weight(1f),
                style = NezhaTheme.typography.title.copy(color = palette.textPrimary),
            )
            NezhaSwitch(
                checked = uiState.keywordRules.enabled,
                onCheckedChange = { enabled -> onIntent(RuleListUiIntent.ToggleKeywordBlocking(enabled)) },
            )
        }
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NezhaDimens.blockGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NezhaTextField(
                value = uiState.keywordDraft,
                onValueChange = { draft -> onIntent(RuleListUiIntent.KeywordDraftChanged(draft)) },
                placeholder = stringResource(R.string.rules_keyword_add_hint),
                onSubmit = { onIntent(RuleListUiIntent.SubmitKeywordDraft) },
                modifier = Modifier.weight(1f),
            )
            NezhaPillButton(
                text = stringResource(R.string.rules_add_action),
                enabled = uiState.keywordDraft.isNotBlank(),
                onClick = { onIntent(RuleListUiIntent.SubmitKeywordDraft) },
            )
        }
        uiState.keywordMessage?.let { message ->
            Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
            BasicText(
                text = stringResource(message.textRes()),
                style = NezhaTheme.typography.caption.copy(
                    color = if (message == RuleListMessage.Added) palette.brand else palette.textPrimary,
                ),
            )
        }
    }
}

/** 一个关键词。关键词没有启停覆写，只有存在与删除两种状态。 */
@Composable
private fun KeywordRow(keyword: String, onIntent: (RuleListUiIntent) -> Unit) {
    val palette = NezhaTheme.palette
    NezhaSurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = keyword,
                modifier = Modifier.weight(1f),
                style = NezhaTheme.typography.body.copy(color = palette.textPrimary),
            )
            NezhaPillButton(
                text = stringResource(R.string.rules_delete_action),
                onClick = { onIntent(RuleListUiIntent.DeleteKeyword(keyword)) },
            )
        }
    }
}
