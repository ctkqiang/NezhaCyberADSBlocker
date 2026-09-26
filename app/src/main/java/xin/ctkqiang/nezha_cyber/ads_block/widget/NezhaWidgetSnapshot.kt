package xin.ctkqiang.nezha_cyber.ads_block.widget

/**
 * 一次小组件刷新所需的全部数据。
 *
 * 刻意做成一个**快照**而不是让渲染层直接读各个端口：小组件的刷新发生在广播里，允许的时间很短，
 * 数据必须一次性取齐。边渲染边读，一旦某个端口慢了，用户看到的就是「一半新一半旧」的卡片。
 *
 * 这里存的是已经算好的展示值，不是原因：占比、排名、趋势分桶都在加载阶段完成，
 * 渲染阶段只做「把值塞进 RemoteViews」。RemoteViews 的每次 set 都是一次跨进程调用，
 * 塞进去之前不该还有计算。
 */
internal data class NezhaWidgetSnapshot(
    val isRunning: Boolean = false,
    val isTransitioning: Boolean = false,
    val hasFailed: Boolean = false,
    val observed: Long = 0,
    val blocked: Long = 0,
    val relayed: Long = 0,
    val distinctBlockedHosts: Int = 0,
    val latestBlockedHost: String? = null,
    val latestBlockedApp: String? = null,
    val protectedApplicationCount: Int = 0,
    val builtinRuleCount: Int = 0,
    val userRuleCount: Int = 0,
    val topBlockedApplications: List<TopApplication> = emptyList(),
    val trendHasBlock: List<Boolean> = emptyList(),
) {
    /** 拦截占已观测的比例，取值 0..1。没有观测时不返回 1，避免把「没数据」画成「全都拦了」。 */
    val blockedFraction: Float
        get() = if (observed <= 0) 0f else (blocked.toDouble() / observed.toDouble()).toFloat()

    /**
     * 被拦最多的应用。
     *
     * 嵌套在这里而不是另起一个顶层声明：它只在 [topBlockedApplications] 里有意义，
     * 出去以后没有任何地方会单独用它（工程规则第 37.1 节对封闭层次的例外）。
     */
    internal data class TopApplication(val label: String, val blocked: Long)

    internal companion object {
        val Empty = NezhaWidgetSnapshot()
    }
}
