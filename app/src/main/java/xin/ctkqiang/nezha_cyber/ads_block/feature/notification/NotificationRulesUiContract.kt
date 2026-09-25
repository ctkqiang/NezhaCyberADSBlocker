package xin.ctkqiang.nezha_cyber.ads_block.feature.notification

import xin.ctkqiang.nezha_cyber.ads_block.domain.application.InstalledApplication
import xin.ctkqiang.nezha_cyber.ads_block.domain.notification.NotificationRuleId

/**
 * 通知拦截页的状态、意图与一次性效果。
 *
 * 三者构成同一个封闭层次，因此共用一个文件（工程规则第 40.2 节）。
 *
 * [installedApplications] 由既有的应用发现端口提供，界面**不重复实现**一遍应用扫描。
 * [editor] 为 null 表示编辑器未打开；非 null 时它就是正在编辑的那份草稿——草稿放在状态里而不是
 * 界面的 `remember` 里，是因为「点了保存之后草稿要清掉」这件事发生在 ViewModel，
 * 状态若留在界面就会出现两处各改一半。
 */
data class NotificationRulesUiState(
    val rules: List<NotificationRuleRow> = emptyList(),
    val installedApplications: List<InstalledApplication> = emptyList(),
    val isAccessGranted: Boolean = false,
    val isLoading: Boolean = true,
    val editor: NotificationRuleEditorState? = null,
    val isApplicationPickerOpen: Boolean = false,
)

/** 列表里的一行。[appLabel] 已经在 ViewModel 侧解析好，界面不再去查包管理器。 */
data class NotificationRuleRow(
    val id: NotificationRuleId,
    val packageName: String,
    val appLabel: String,
    val matchText: String,
    val enabled: Boolean,
)

/**
 * 正在编辑的规则草稿。
 *
 * [editingRuleId] 为 null 表示新建。[packageName] 为 null 表示还没选应用——这一步不能省，
 * 因为规则的作用范围就是包名，没有它这条规则无处生效。
 */
data class NotificationRuleEditorState(
    val editingRuleId: NotificationRuleId?,
    val packageName: String?,
    val matchText: String,
) {
    val isEditing: Boolean get() = editingRuleId != null
}

sealed interface NotificationRulesUiIntent {
    /** 用户从系统设置回到应用，权限状态可能已经变了，需要重新查询。 */
    data object RefreshAccess : NotificationRulesUiIntent

    data object OpenEditor : NotificationRulesUiIntent

    data class EditRule(val id: NotificationRuleId) : NotificationRulesUiIntent

    data object DismissEditor : NotificationRulesUiIntent

    data object OpenApplicationPicker : NotificationRulesUiIntent

    data object DismissApplicationPicker : NotificationRulesUiIntent

    data class SelectApplication(val packageName: String) : NotificationRulesUiIntent

    data class MatchTextChanged(val matchText: String) : NotificationRulesUiIntent

    data object SubmitEditor : NotificationRulesUiIntent

    data class SetEnabled(val id: NotificationRuleId, val enabled: Boolean) : NotificationRulesUiIntent

    data class DeleteRule(val id: NotificationRuleId) : NotificationRulesUiIntent
}

/**
 * 一次性效果。
 *
 * 这些都必须**发一次就结束**：把它们塞进状态会让每次重组都重新弹一次提示，
 * 或者出现「提示已经显示了但没人把它关掉」。界面据此显示一条短提示并在片刻后淡出。
 */
sealed interface NotificationRulesUiEffect {
    data object RuleCreated : NotificationRulesUiEffect

    data object RuleUpdated : NotificationRulesUiEffect

    data object RuleDeleted : NotificationRulesUiEffect

    data class ShowError(val error: NotificationRulesError) : NotificationRulesUiEffect
}

/** 规则编辑可能失败的三种原因。与 `NotificationRuleEditResult` 一一对应，但措辞属于界面层。 */
enum class NotificationRulesError {
    EmptyMatchText,
    Duplicate,
    RuleMissing,
}
