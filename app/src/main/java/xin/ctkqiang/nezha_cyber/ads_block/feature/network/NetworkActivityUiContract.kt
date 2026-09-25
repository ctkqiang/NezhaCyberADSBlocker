package xin.ctkqiang.nezha_cyber.ads_block.feature.network

import java.time.Instant
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleSource

/**
 * 实时活动页的状态与意图。
 *
 * 三者构成同一个封闭层次，因此共用一个文件（工程规则第 40.2 节）。
 */
data class NetworkActivityUiState(
    val rows: List<NetworkActivityRow> = emptyList(),
    val applicationOptions: List<ApplicationOption> = emptyList(),
    val blockedOnly: Boolean = false,
    val applicationFilter: ApplicationFilter = ApplicationFilter.All,
    val query: String = "",
    /** 是否跟随最新的记录。为 false 时 [rows] 冻结在按下暂停那一刻。 */
    val isLive: Boolean = true,
    /**
     * 观测记录是否开着。
     *
     * 关掉时这一页会是空的，而「空」与「没有流量」看起来一模一样，因此界面必须
     * 明确说明是记录被关了，而不是隧道没工作。
     */
    val isObservationLoggingEnabled: Boolean = true,
    val isLoading: Boolean = true,
) {
    /**
     * 按当前三个筛选条件显示的行。
     *
     * 写成派生值而不是另存一份列表：两份列表迟早会出现一处忘了更新，
     * 而观测流水正是最容易漏更新的那种高频数据。条数上限为最近若干条，逐个过滤的开销可以忽略。
     */
    val visibleRows: List<NetworkActivityRow>
        get() {
            val trimmedQuery = query.trim()
            return rows.filter { row ->
                val matchesAction = !blockedOnly || row.blocked
                val matchesApplication = applicationFilter.matches(row.packageName)
                val matchesQuery = trimmedQuery.isEmpty() || row.host.contains(trimmedQuery, ignoreCase = true)
                matchesAction && matchesApplication && matchesQuery
            }
        }

    /** 是否有任何一个筛选条件在生效。界面据此决定摘要措辞。 */
    val isFiltered: Boolean
        get() = blockedOnly || applicationFilter != ApplicationFilter.All || query.isNotBlank()
}

/**
 * 应用筛选条件。
 *
 * 用封闭类型而不是可空字符串：`null` 无法同时表达「全部应用」与「归属不到应用的那一组」，
 * 而后者在 Android 10 以下或反查失败时是常态，必须能被单独选中。
 */
sealed interface ApplicationFilter {
    fun matches(packageName: String?): Boolean

    /** 不筛应用。 */
    data object All : ApplicationFilter {
        override fun matches(packageName: String?): Boolean = true
    }

    /** 只看某一个应用；[packageName] 为 null 表示只看「未知来源」那一组。 */
    data class Source(val packageName: String?) : ApplicationFilter {
        override fun matches(packageName: String?): Boolean = packageName == this.packageName
    }
}

/**
 * 应用筛选的一个可选项。
 *
 * [label] 只有在 [packageName] 也为 null 时才是 null（那一组归属不到任何应用）。
 * 有包名却解析不到显示名时退回包名本身：显示包名仍然能定位到具体应用，
 * 而显示「未知来源」会把两组不同的东西混成一组。
 *
 * 「全部应用」不出现在这里：那个选项的文案属于界面资源，由界面自己补在最前面。
 */
data class ApplicationOption(val packageName: String?, val label: String?)

/**
 * 一条观测的展示形态。
 *
 * [appLabel] 为 null 表示无法归属到应用（系统过低或反查失败），界面必须显示为未知来源，
 * 不能拿域名或包名顶替。[packageName] 同时供界面按需取该应用的图标。
 */
data class NetworkActivityRow(
    val id: String,
    val at: Instant,
    val host: String,
    val appLabel: String?,
    val blocked: Boolean,
    val matchedRule: String?,
    val source: RuleSource?,
    val packageName: String?,
)

sealed interface NetworkActivityUiIntent {
    data class SetBlockedOnly(val blockedOnly: Boolean) : NetworkActivityUiIntent

    data class SetApplicationFilter(val filter: ApplicationFilter) : NetworkActivityUiIntent

    data class QueryChanged(val query: String) : NetworkActivityUiIntent

    data class SetLive(val isLive: Boolean) : NetworkActivityUiIntent
}
