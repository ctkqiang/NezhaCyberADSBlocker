package xin.ctkqiang.nezha_cyber.ads_block.feature.setting

import xin.ctkqiang.nezha_cyber.ads_block.domain.appearance.ThemePreference
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.BlockedResponseMode
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.ObservationRetention

/**
 * 设置页状态。
 *
 * 前三个字段是只读信息（规则规模），[themePreference] 与后三个是**用户可改**的偏好。
 * 偏好字段直接镜像各自的存储，不做二次加工：设置页要做的事就是让用户看到系统实际生效的
 * 那一份值，任何中间层都只会成为「界面显示的与生效的不一致」的来源。
 *
 * 文件名与声明同名而不是叫 `SettingsUiContract.kt`：意图类型在 `SettingsUiIntent.kt` 里，
 * 本文件只有一个顶层声明，按第 37.1 节的文件命名即可。
 */
data class SettingsUiState(
    val builtinRuleCount: Int = 0,
    val userRuleCount: Int = 0,
    val builtinVersion: Int = 0,
    val themePreference: ThemePreference = ThemePreference.System,
    val isObservationLoggingEnabled: Boolean = true,
    val observationRetention: ObservationRetention = ObservationRetention.Standard,
    val blockedResponseMode: BlockedResponseMode = BlockedResponseMode.NxDomain,
)
