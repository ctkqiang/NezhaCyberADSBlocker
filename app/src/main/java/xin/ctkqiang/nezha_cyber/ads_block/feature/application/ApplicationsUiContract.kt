package xin.ctkqiang.nezha_cyber.ads_block.feature.application

/**
 * 应用列表页状态与意图。
 *
 * 统计数字来自最近观测窗口，因此界面文案带上「最近」的口径说明：
 * 跨会话的按应用累计需要长期保存每应用的计数，当前阶段没有做，界面不能假装有。
 */
data class ApplicationsUiState(
    val rows: List<ApplicationRow> = emptyList(),
    val protectedCount: Int = 0,
    val isLoading: Boolean = true,
    val isRestarting: Boolean = false,
    val needsPackageVisibility: Boolean = false,
)

/**
 * 列表里的一行。
 *
 * [permissions] 为 null 表示**读不到**该包的权限信息（已卸载或对本应用不可见）。
 * 这与「读到了，但没有运行时权限」是两件不同的事，因此用可空类型区分而不是用 0 表示，
 * 界面也必须分开显示——把读不到显示成「0 项」是在编造一个审计结论（第 32 节）。
 */
data class ApplicationRow(
    val packageName: String,
    val label: String,
    val isProtected: Boolean,
    val observedCount: Int,
    val blockedCount: Int,
    val permissions: DangerousPermissionSummary?,
)

/**
 * 危险权限摘要。
 *
 * 只统计运行时权限（平台概念 `PROTECTION_DANGEROUS`）：普通权限安装即授予、用户关不掉，
 * 计入只会稀释这个数字的意义。列表页用它给出一眼可扫的侵入程度，完整清单在应用详情页。
 */
data class DangerousPermissionSummary(val granted: Int, val total: Int)

sealed interface ApplicationsUiIntent {
    data class ToggleProtection(val packageName: String) : ApplicationsUiIntent

    data object ClearSelection : ApplicationsUiIntent

    data class AuthorizationResult(val granted: Boolean) : ApplicationsUiIntent

    data object RetryLoad : ApplicationsUiIntent

    data object OpenSettings : ApplicationsUiIntent
}

/**
 * 一次性效果。
 *
 * 修改选择后若隧道正在运行必须重建，而重建可能需要系统授权——这类动作不能塞进状态里，
 * 否则每次重组都会重新触发一次（工程规则第 40.1 节）。
 */
sealed interface ApplicationsUiEffect {
    data object RequestVpnAuthorization : ApplicationsUiEffect

    data object OpenApplicationSettings : ApplicationsUiEffect
}
