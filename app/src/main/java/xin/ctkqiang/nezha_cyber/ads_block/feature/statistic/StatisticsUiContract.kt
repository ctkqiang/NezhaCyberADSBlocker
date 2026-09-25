package xin.ctkqiang.nezha_cyber.ads_block.feature.statistic

import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.FilteringStatistics

/**
 * 统计页状态与意图。
 *
 * 三者构成同一个封闭层次，因此共用一个文件（工程规则第 40.2 节）。
 *
 * 第 24 节要求统计同时覆盖两个维度：整体读数与按应用读数。两者都来自最近观测窗口，
 * 与跨会话累计的计数器口径不同，因此界面上必须分别标注范围。
 */
data class StatisticsUiState(
    val statistics: FilteringStatistics = FilteringStatistics(),
    val protectedScope: ProtectedScope = ProtectedScope.AllApplications,
    val builtinRuleCount: Int = 0,
    val userRuleCount: Int = 0,
    val builtinVersion: Int = 0,
    val topBlockedDomains: List<BlockedDomainCount> = emptyList(),
    val applicationTraffic: List<ApplicationTrafficSummary> = emptyList(),
    val isLoading: Boolean = true,
    /**
     * 观测记录是否开着。
     *
     * 关掉时读数会停住不动，而「停住」与「没有流量」看起来一样，因此页面必须说明原因。
     */
    val isObservationLoggingEnabled: Boolean = true,
    /** 是否已进入「确认清除」这一态。清空不可撤销，因此用两次点按代替一个对话框。 */
    val isConfirmingClear: Boolean = false,
) {
    /** 有没有东西可以清除。全为零时按钮不可点：可点却什么也不发生的按钮比不可点更让人困惑。 */
    val hasStatistics: Boolean
        get() = statistics.observed > 0 || applicationTraffic.isNotEmpty()
}

/**
 * 逐应用保护的范围。
 *
 * 空集合**不是**「保护了 0 个应用」，而是「没有限定应用，全部应用都被接管」。
 * 这个区别必须显式建模，否则统计页会在用户处于全量接管时显示「受保护应用 0」
 * （工程规则第 6 节与 `ProtectedApplicationStore` 的语义）。
 */
sealed interface ProtectedScope {
    data object AllApplications : ProtectedScope

    data class Selected(val count: Int) : ProtectedScope
}

/** 被拦截域名的出现次数。[count] 来自最近窗口，界面必须标注这一点。 */
data class BlockedDomainCount(val host: String, val count: Int)

/**
 * 一个应用的流量读数（第 24 节的按应用维度）。
 *
 * [packageName] 为 null 表示归属不到应用的那一组，此时 [label] 也是 null，界面显示为未知来源。
 * [relayed] 是派生值，理由与 [FilteringStatistics] 相同：单独存一份只会多一个不一致的机会。
 */
data class ApplicationTrafficSummary(
    val packageName: String?,
    val label: String?,
    val observed: Int,
    val blocked: Int,
    val topHosts: List<String>,
) {
    val relayed: Int
        get() = observed - blocked
}

/**
 * 统计页唯一的写操作。
 *
 * 清空不可撤销，因此拆成「请求」与「确认」两步，而不是直接执行。
 */
sealed interface StatisticsUiIntent {
    data object RequestClear : StatisticsUiIntent

    data object ConfirmClear : StatisticsUiIntent

    data object CancelClear : StatisticsUiIntent
}
