package xin.ctkqiang.nezha_cyber.ads_block.feature.notification

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
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.InstalledApplication
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.InstalledApplicationSource
import xin.ctkqiang.nezha_cyber.ads_block.domain.notification.NotificationAccessSource
import xin.ctkqiang.nezha_cyber.ads_block.domain.notification.NotificationRule
import xin.ctkqiang.nezha_cyber.ads_block.domain.notification.NotificationRuleEditResult
import xin.ctkqiang.nezha_cyber.ads_block.domain.notification.NotificationRuleId
import xin.ctkqiang.nezha_cyber.ads_block.domain.notification.NotificationRuleStore

/** 一次性效果的缓冲容量。满了之后 `tryEmit` 会失败并丢掉新效果，因此给足余量。 */
private const val EFFECT_BUFFER_CAPACITY = 8

/**
 * 通知拦截规则的 ViewModel。
 *
 * 三个上游（规则、权限状态、已安装应用）各自是独立的流，状态统一在 [publish] 里重建。
 * 比在每个流里各改一部分状态更难出错：通知规则页同时显示规则与权限，
 * 分成两处改很容易出现「换了应用列表但行里的应用名没跟着更新」。
 *
 * 已安装应用清单**复用既有的应用发现端口**，不另写一份扫描逻辑（第 20 节的要求）。
 */
class NotificationRulesViewModel(
    private val ruleStore: NotificationRuleStore,
    private val accessSource: NotificationAccessSource,
    installedApplicationSource: InstalledApplicationSource,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(NotificationRulesUiState())

    private val mutableEffect = MutableSharedFlow<NotificationRulesUiEffect>(
        extraBufferCapacity = EFFECT_BUFFER_CAPACITY,
    )

    private val labelCache = ApplicationLabelCache(installedApplicationSource)

    private var installedApplications: List<InstalledApplication> = emptyList()

    private var labels: Map<String, String> = emptyMap()

    private var rules: List<NotificationRule> = emptyList()

    val uiState: StateFlow<NotificationRulesUiState> = mutableUiState.asStateFlow()

    val effect: SharedFlow<NotificationRulesUiEffect> = mutableEffect.asSharedFlow()

    init {
        viewModelScope.launch {
            installedApplications = installedApplicationSource.listInstalledApplications()
            labels = labelCache.resolve(installedApplications.map { application -> application.packageName }.toSet())
            publish()
        }
        viewModelScope.launch {
            ruleStore.rules.collect { snapshot ->
                rules = snapshot
                publish()
            }
        }
        viewModelScope.launch {
            accessSource.isGranted.collect { granted ->
                mutableUiState.update { current -> current.copy(isAccessGranted = granted, isLoading = false) }
            }
        }
    }

    fun dispatch(intent: NotificationRulesUiIntent) {
        when (intent) {
            NotificationRulesUiIntent.RefreshAccess -> accessSource.refresh()

            NotificationRulesUiIntent.OpenEditor -> mutableUiState.update { current ->
                current.copy(
                    editor = NotificationRuleEditorState(
                        editingRuleId = null,
                        packageName = null,
                        matchText = "",
                    ),
                )
            }

            is NotificationRulesUiIntent.EditRule -> startEditing(intent.id)

            NotificationRulesUiIntent.DismissEditor -> mutableUiState.update { current ->
                current.copy(editor = null, isApplicationPickerOpen = false)
            }

            NotificationRulesUiIntent.OpenApplicationPicker -> mutableUiState.update { current ->
                current.copy(isApplicationPickerOpen = true)
            }

            NotificationRulesUiIntent.DismissApplicationPicker -> mutableUiState.update { current ->
                current.copy(isApplicationPickerOpen = false)
            }

            is NotificationRulesUiIntent.SelectApplication -> mutableUiState.update { current ->
                val editor = current.editor ?: return@update current
                current.copy(
                    editor = editor.copy(packageName = intent.packageName),
                    isApplicationPickerOpen = false,
                )
            }

            is NotificationRulesUiIntent.MatchTextChanged -> mutableUiState.update { current ->
                val editor = current.editor ?: return@update current
                current.copy(editor = editor.copy(matchText = intent.matchText))
            }

            NotificationRulesUiIntent.SubmitEditor -> submit()

            is NotificationRulesUiIntent.SetEnabled -> viewModelScope.launch {
                report(
                    result = ruleStore.setEnabled(id = intent.id, enabled = intent.enabled),
                    success = null,
                )
            }

            is NotificationRulesUiIntent.DeleteRule -> viewModelScope.launch {
                report(result = ruleStore.delete(intent.id), success = NotificationRulesUiEffect.RuleDeleted)
            }
        }
    }

    /**
     * 打开编辑草稿。
     *
     * 规则可能在界面持有列表之后被删除（例如另一处入口删掉了它），因此这里先按 id 找一遍，
     * 找不到就如实报「规则已不存在」，而不是开一个指向不存在规则的编辑器。
     */
    private fun startEditing(id: NotificationRuleId) {
        val rule = rules.firstOrNull { candidate -> candidate.id == id }
        if (rule == null) {
            emitEffect(NotificationRulesUiEffect.ShowError(error = NotificationRulesError.RuleMissing))
            return
        }
        mutableUiState.update { current ->
            current.copy(
                editor = NotificationRuleEditorState(
                    editingRuleId = rule.id,
                    packageName = rule.packageName,
                    matchText = rule.matchText,
                ),
            )
        }
    }

    /**
     * 提交编辑草稿。
     *
     * [NotificationRuleEditorState.editingRuleId] 先绑定到局部变量再判空：直接把 `== null` 的结论
     * 存成布尔、另一处又写 `?: NotificationRuleId(0L)` 的话，那个 0 是一个**编出来的 id**，
     * 一旦判断与取值两条路径分叉，它就会安静地去更新一条不存在的规则。
     */
    private fun submit() {
        val editor = mutableUiState.value.editor ?: return
        val packageName = editor.packageName ?: return
        viewModelScope.launch {
            val editingRuleId = editor.editingRuleId
            val result = if (editingRuleId == null) {
                ruleStore.create(packageName = packageName, matchText = editor.matchText)
            } else {
                ruleStore.update(id = editingRuleId, packageName = packageName, matchText = editor.matchText)
            }
            val success = if (editingRuleId == null) {
                NotificationRulesUiEffect.RuleCreated
            } else {
                NotificationRulesUiEffect.RuleUpdated
            }
            report(result = result, success = success)
            if (result == NotificationRuleEditResult.Applied) {
                mutableUiState.update { current -> current.copy(editor = null) }
            }
        }
    }

    /**
     * 把领域结果翻译成一次性效果。
     *
     * 成功时发哪一条由调用方给出（[success]），失败统一映射成 [NotificationRulesUiEffect.ShowError]：
     * 领域层用四种结果区分失败原因，界面只需要「出错了、原因是什么」。
     */
    private fun report(result: NotificationRuleEditResult, success: NotificationRulesUiEffect?) {
        when (result) {
            NotificationRuleEditResult.Applied -> success?.let { effect -> emitEffect(effect) }

            NotificationRuleEditResult.EmptyMatchText -> emitEffect(
                NotificationRulesUiEffect.ShowError(error = NotificationRulesError.EmptyMatchText),
            )

            NotificationRuleEditResult.Duplicate -> emitEffect(
                NotificationRulesUiEffect.ShowError(error = NotificationRulesError.Duplicate),
            )

            NotificationRuleEditResult.NotFound -> emitEffect(
                NotificationRulesUiEffect.ShowError(error = NotificationRulesError.RuleMissing),
            )
        }
    }

    private fun emitEffect(effect: NotificationRulesUiEffect) {
        mutableEffect.tryEmit(effect)
    }

    private fun publish() {
        mutableUiState.update { current ->
            current.copy(
                rules = rules.map { rule -> rule.toRow() },
                installedApplications = installedApplications,
            )
        }
    }

    /** 解析不到应用名时显示包名：包名仍然能定位到它，而「未知应用」会把两个来源说成同一个。 */
    private fun NotificationRule.toRow(): NotificationRuleRow = NotificationRuleRow(
        id = id,
        packageName = packageName,
        appLabel = labels[packageName] ?: packageName,
        matchText = matchText,
        enabled = enabled,
    )

    companion object {
        fun factory(
            ruleStore: NotificationRuleStore,
            accessSource: NotificationAccessSource,
            installedApplicationSource: InstalledApplicationSource,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                NotificationRulesViewModel(
                    ruleStore = ruleStore,
                    accessSource = accessSource,
                    installedApplicationSource = installedApplicationSource,
                )
            }
        }
    }
}
