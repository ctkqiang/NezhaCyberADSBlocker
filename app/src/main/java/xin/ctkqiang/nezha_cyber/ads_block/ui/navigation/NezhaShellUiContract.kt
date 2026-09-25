package xin.ctkqiang.nezha_cyber.ads_block.ui.navigation

import androidx.compose.runtime.Immutable

/**
 * 外壳状态。
 *
 * 当前展示哪一个页面是渲染的直接依据，属于状态而不是一次性动作，因此不需要 UiEffect：
 * 本骨架里没有任何「发一次就结束」的副作用。等真正出现（例如跳转系统 VPN 授权页）
 * 再引入 NezhaShellUiEffect，不提前占位。
 *
 * [selectedPackageName] 是应用详情页的目标应用。它由外壳持有而不是由详情页自己保存：
 * 页面本身没有「选择」这个动作，是应用列表把用户的选择转达给外壳的。
 * 为 null 表示还没选过，详情页据此显示引导，而不是转圈或显示上一个应用。
 */
@Immutable
data class NezhaShellUiState(
    val selectedTab: NezhaTab,
    val selectedSection: NezhaSection,
    val selectedPackageName: String? = null,
)

/**
 * 外壳层面接收的用户意图。
 *
 * 切换标签页时会重置到该标签页的默认页面，等价于 iOS 标签栏「再次点击回到根页面」的行为。
 */
sealed interface NezhaShellUiIntent {
    data class SelectTab(val tab: NezhaTab) : NezhaShellUiIntent

    data class SelectSection(val section: NezhaSection) : NezhaShellUiIntent

    /** 从应用列表点开某个应用的详情。 */
    data class ShowApplicationDetail(val packageName: String) : NezhaShellUiIntent
}
