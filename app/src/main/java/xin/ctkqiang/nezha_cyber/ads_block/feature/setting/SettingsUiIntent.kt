package xin.ctkqiang.nezha_cyber.ads_block.feature.setting

import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.BlockedResponseMode
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.ObservationRetention

/**
 * 设置页接收的用户意图。
 *
 * 每一项都对应一次落盘写入，没有「发一次就结束」的副作用，因此本页不需要 UiEffect。
 *
 * 单独成文件而不是与 [SettingsUiState] 合并：工程规则第 40.2 节允许同一个封闭层次的
 * 状态与意图共用一个文件，但第 37.1 节的默认是「一个文件一个顶层声明」，
 * 而这里合并并没有带来任何可读性收益——设置页的状态与意图都很小，分开放反而更好找。
 */
sealed interface SettingsUiIntent {
    data class SetObservationLoggingEnabled(val enabled: Boolean) : SettingsUiIntent

    data class SetObservationRetention(val retention: ObservationRetention) : SettingsUiIntent

    data class SetBlockedResponseMode(val mode: BlockedResponseMode) : SettingsUiIntent
}
