package xin.ctkqiang.nezha_cyber.ads_block.ui.component

/**
 * 对比条的一条。
 *
 * [highlighted] 与 [rest] 是同一总量的两个部分，条长由两者之和决定：这样跨行比较的是**总量**，
 * 而每行内部又能看出被强调那部分占多少。只按 [highlighted] 排行长会把总量差异藏起来——
 * 一个拦了 3 条的应用与一个拦了 3 条但总共请求了 300 次的应用，看起来会一模一样。
 *
 * [trailing] 是行右侧的读数文本，由调用方格式化：数量口径（观测 / 拦截 / 放行）在不同图里
 * 不一样，组件不猜。
 *
 * [caption] 是可选的补充说明（例如该应用的高频域名），[trailing] 放不下或放进去会打断
 * 对比节奏的信息都放这里。为 null 时整行不留空位。
 */
data class NezhaComparisonBar(
    val label: String,
    val highlighted: Long,
    val rest: Long,
    val trailing: String,
    val caption: String? = null,
)
