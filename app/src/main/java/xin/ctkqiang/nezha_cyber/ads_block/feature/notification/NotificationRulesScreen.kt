package xin.ctkqiang.nezha_cyber.ads_block.feature.notification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import xin.ctkqiang.nezha_cyber.ads_block.R
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.InstalledApplication
import xin.ctkqiang.nezha_cyber.ads_block.ui.application.rememberApplicationIcon
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaAppIcon
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaEmptyState
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaListScaffold
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaPillButton
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaSurfaceCard
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaSwitch
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaTextField
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.LocalInstalledApplicationSource
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.LocalNotificationAccessSource
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.LocalNotificationRuleStore
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.NezhaDataPreviewHost
import xin.ctkqiang.nezha_cyber.ads_block.ui.notification.LocalNotificationAccessLauncher
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaThemePreview

/** 一次性提示的停留时长。足够读完一句话，又不会长期占住页头。 */
private const val MESSAGE_VISIBLE_MILLIS = 2600L

private const val EMPTY_ITEM_KEY = "notification-rules-empty"

/**
 * 通知拦截规则页。
 *
 * 页面只渲染状态、派发意图：规则判定的逻辑一行都不在这里，界面也不直接接触数据库
 * （工程规则第 40.3 节）。跳转系统设置由注入的 [NotificationAccessLauncher] 完成，
 * 因此本文件里没有 Android 的 `Intent`。
 *
 * 页头里有一段平台限制说明，是刻意放在这里的：用户最可能以为「拦截」发生在通知出现之前，
 * 而事实上系统是在通知**已经发布**之后才回调本应用。不写清楚，用户会以为功能坏了。
 */
@Composable
fun NotificationRulesScreen(modifier: Modifier = Modifier) {
    val ruleStore = LocalNotificationRuleStore.current
    val accessSource = LocalNotificationAccessSource.current
    val installedApplicationSource = LocalInstalledApplicationSource.current
    val accessLauncher = LocalNotificationAccessLauncher.current
    val viewModel: NotificationRulesViewModel = viewModel(
        factory = NotificationRulesViewModel.factory(
            ruleStore = ruleStore,
            accessSource = accessSource,
            installedApplicationSource = installedApplicationSource,
        ),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // 用户是去**别的界面**开权限的，授权完成不会有任何回调落到本进程。回到本页时若不重新查询，
    // 页面会一直停在「权限未开启」，用户只能靠杀进程再打开才看到变化——那看起来就是功能坏了。
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.dispatch(NotificationRulesUiIntent.RefreshAccess)
    }
    var latestEffect by remember { mutableStateOf<NotificationRulesUiEffect?>(null) }
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect -> latestEffect = effect }
    }
    LaunchedEffect(latestEffect) {
        if (latestEffect != null) {
            delay(MESSAGE_VISIBLE_MILLIS)
            latestEffect = null
        }
    }
    NotificationRulesContent(
        uiState = uiState,
        message = latestEffect?.let { effect -> effectMessage(effect) },
        onIntent = viewModel::dispatch,
        onOpenAccessSettings = { accessLauncher.launch() },
        modifier = modifier,
    )
}

@Composable
private fun NotificationRulesContent(
    uiState: NotificationRulesUiState,
    message: String?,
    onIntent: (NotificationRulesUiIntent) -> Unit,
    onOpenAccessSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        NezhaListScaffold(
            header = {
                if (!uiState.isAccessGranted) {
                    NotificationAccessCard(onOpenAccessSettings = onOpenAccessSettings)
                    Spacer(modifier = Modifier.height(NezhaDimens.sectionGap))
                }
                uiState.editor?.let { editor ->
                    NotificationRuleEditorCard(
                        editor = editor,
                        applications = uiState.installedApplications,
                        onIntent = onIntent,
                    )
                    Spacer(modifier = Modifier.height(NezhaDimens.sectionGap))
                }
                NotificationRulesHeader(message = message, onIntent = onIntent)
            },
        ) {
            if (uiState.rules.isEmpty()) {
                item(key = EMPTY_ITEM_KEY) {
                    NezhaEmptyState(
                        description = stringResource(R.string.notification_rules_empty),
                        tag = stringResource(R.string.notification_rules_empty_tag),
                    )
                }
            } else {
                items(items = uiState.rules, key = { row -> row.id.value }) { row ->
                    NotificationRuleRowCard(row = row, onIntent = onIntent)
                }
            }
        }
        if (uiState.isApplicationPickerOpen) {
            NotificationApplicationPicker(
                applications = uiState.installedApplications,
                onSelect = { packageName -> onIntent(NotificationRulesUiIntent.SelectApplication(packageName)) },
                onDismiss = { onIntent(NotificationRulesUiIntent.DismissApplicationPicker) },
            )
        }
    }
}

@Composable
private fun NotificationRulesHeader(message: String?, onIntent: (NotificationRulesUiIntent) -> Unit) {
    val palette = NezhaTheme.palette
    BasicText(
        text = stringResource(R.string.notification_rules_title),
        style = NezhaTheme.typography.title.copy(color = palette.textPrimary),
    )
    message?.let { text ->
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        BasicText(
            text = text,
            style = NezhaTheme.typography.label.copy(color = palette.brand),
        )
    }
    Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
    BasicText(
        text = stringResource(R.string.notification_rules_platform_note),
        style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
    )
    Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
    NezhaPillButton(
        text = stringResource(R.string.notification_rules_add_action),
        onClick = { onIntent(NotificationRulesUiIntent.OpenEditor) },
    )
}

/** 权限未开启时的引导卡片。通知使用权只能由用户在系统设置里开启，因此这里只提供跳转。 */
@Composable
private fun NotificationAccessCard(onOpenAccessSettings: () -> Unit) {
    val palette = NezhaTheme.palette
    NezhaSurfaceCard {
        BasicText(
            text = stringResource(R.string.notification_access_required_title),
            style = NezhaTheme.typography.title.copy(color = palette.brand),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        BasicText(
            text = stringResource(R.string.notification_access_required_message),
            style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        NezhaPillButton(
            text = stringResource(R.string.notification_access_required_action),
            onClick = onOpenAccessSettings,
        )
    }
}

@Composable
private fun NotificationRuleEditorCard(
    editor: NotificationRuleEditorState,
    applications: List<InstalledApplication>,
    onIntent: (NotificationRulesUiIntent) -> Unit,
) {
    val palette = NezhaTheme.palette
    val selectedLabel = editor.packageName?.let { packageName ->
        applications.firstOrNull { application -> application.packageName == packageName }?.label ?: packageName
    }
    NezhaSurfaceCard {
        BasicText(
            text = stringResource(
                if (editor.isEditing) {
                    R.string.notification_rule_editor_edit_title
                } else {
                    R.string.notification_rule_editor_new_title
                },
            ),
            style = NezhaTheme.typography.title.copy(color = palette.textPrimary),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        BasicText(
            text = stringResource(R.string.notification_rule_editor_application),
            style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.tightGap))
        NezhaPillButton(
            text = selectedLabel ?: stringResource(R.string.notification_rule_editor_application_placeholder),
            onClick = { onIntent(NotificationRulesUiIntent.OpenApplicationPicker) },
        )
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        BasicText(
            text = stringResource(R.string.notification_rule_editor_match_text),
            style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.tightGap))
        NezhaTextField(
            value = editor.matchText,
            onValueChange = { matchText -> onIntent(NotificationRulesUiIntent.MatchTextChanged(matchText)) },
            placeholder = stringResource(R.string.notification_rule_editor_match_text_placeholder),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        BasicText(
            text = stringResource(R.string.notification_rule_editor_note),
            style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        Row(horizontalArrangement = Arrangement.spacedBy(NezhaDimens.blockGap)) {
            NezhaPillButton(
                text = stringResource(R.string.notification_rule_editor_save),
                onClick = { onIntent(NotificationRulesUiIntent.SubmitEditor) },
            )
            NezhaPillButton(
                text = stringResource(R.string.notification_rule_editor_cancel),
                onClick = { onIntent(NotificationRulesUiIntent.DismissEditor) },
            )
        }
    }
}

@Composable
private fun NotificationRuleRowCard(row: NotificationRuleRow, onIntent: (NotificationRulesUiIntent) -> Unit) {
    val palette = NezhaTheme.palette
    val icon = rememberApplicationIcon(packageName = row.packageName)
    NezhaSurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NezhaAppIcon(icon = icon, label = row.appLabel, size = NezhaDimens.appIconCompactSize)
            Spacer(modifier = Modifier.width(NezhaDimens.blockGap))
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = row.appLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = NezhaTheme.typography.title.copy(color = palette.textPrimary),
                )
                Spacer(modifier = Modifier.height(NezhaDimens.tightGap))
                BasicText(
                    text = stringResource(R.string.notification_rules_row_contains, row.matchText),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
                )
            }
            Spacer(modifier = Modifier.width(NezhaDimens.blockGap))
            NezhaSwitch(
                checked = row.enabled,
                onCheckedChange = { enabled ->
                    onIntent(NotificationRulesUiIntent.SetEnabled(id = row.id, enabled = enabled))
                },
            )
        }
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        Row(horizontalArrangement = Arrangement.spacedBy(NezhaDimens.blockGap)) {
            NezhaPillButton(
                text = stringResource(R.string.notification_rules_edit_action),
                onClick = { onIntent(NotificationRulesUiIntent.EditRule(row.id)) },
            )
            NezhaPillButton(
                text = stringResource(R.string.notification_rules_delete_action),
                onClick = { onIntent(NotificationRulesUiIntent.DeleteRule(row.id)) },
            )
        }
    }
}

@Composable
private fun effectMessage(effect: NotificationRulesUiEffect): String = when (effect) {
    NotificationRulesUiEffect.RuleCreated -> stringResource(R.string.notification_rules_created)

    NotificationRulesUiEffect.RuleUpdated -> stringResource(R.string.notification_rules_updated)

    NotificationRulesUiEffect.RuleDeleted -> stringResource(R.string.notification_rules_deleted)

    is NotificationRulesUiEffect.ShowError -> when (effect.error) {
        NotificationRulesError.EmptyMatchText -> stringResource(R.string.notification_error_empty_match_text)
        NotificationRulesError.Duplicate -> stringResource(R.string.notification_error_duplicate)
        NotificationRulesError.RuleMissing -> stringResource(R.string.notification_error_rule_missing)
    }
}

/**
 * 权限未开启态。
 *
 * 这一态比已授权态更需要预览：它多一张引导卡片，而卡片措辞、按钮位置与后面规则列表之间的
 * 间距都只在竖屏窄宽度下才看得出问题。
 */
@Preview(name = "通知拦截规则 · 权限未开启", showBackground = true, widthDp = 820, heightDp = 1400)
@Composable
private fun NotificationRulesScreenWithoutAccessPreview() {
    NezhaThemePreview {
        NezhaDataPreviewHost {
            NotificationRulesScreen()
        }
    }
}

/** 权限已开启态。此时引导卡片消失，页头直接接上规则列表。 */
@Preview(name = "通知拦截规则 · 权限已开启", showBackground = true, widthDp = 820, heightDp = 1400)
@Composable
private fun NotificationRulesScreenWithAccessPreview() {
    NezhaThemePreview {
        NezhaDataPreviewHost(isNotificationAccessGranted = true) {
            NotificationRulesScreen()
        }
    }
}
