package xin.ctkqiang.nezha_cyber.ads_block.widget

import kotlin.math.ceil
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.ApplicationLabelCache
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.InstalledApplicationSource
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.ProtectedApplicationStore
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.DomainObservation
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.ObservationStore
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleStore
import xin.ctkqiang.nezha_cyber.ads_block.domain.vpn.VpnController
import xin.ctkqiang.nezha_cyber.ads_block.domain.vpn.VpnSessionState

/** 趋势条分成几段。八段是在 1×2 到 4×2 的宽度下都还看得清的密度上限。 */
private const val TREND_SLICES = 8

/** 排名卡显示几个应用。三行是小尺寸卡片放得下、又不必滚动的上限。 */
private const val TOP_APPLICATION_LIMIT = 3

/**
 * 把各个端口读成一个 [NezhaWidgetSnapshot]。
 *
 * 只读端口的 `StateFlow.value`，不做任何 I/O 与订阅：小组件刷新发生在广播里，允许的时间很短，
 * 而 `value` 是内存读，微秒级。这里也**不主动 load()**——载入是应用启动时的事，
 * 小组件不该为了刷新一个数字去触发一次磁盘扫描。
 *
 * 代价是冷启动后立刻刷新时可能读到尚未载入的初值（0）。这是刻意的取舍：宁可短暂显示 0，
 * 也不要让一次桌面刷新去读盘。系统每 30 分钟一次的周期刷新很快就会补上真实值。
 *
 * 全部计算都在这里完成，渲染层只负责把结果塞进 RemoteViews——RemoteViews 的每次 set 都是
 * 一次跨进程调用，塞之前不该还有计算。
 */
internal class NezhaWidgetSnapshotLoader(
    private val observationStore: ObservationStore,
    private val protectedApplicationStore: ProtectedApplicationStore,
    private val ruleStore: RuleStore,
    private val vpnController: VpnController,
    private val unknownAppLabel: String,
    applicationSource: InstalledApplicationSource,
) {
    private val labelCache = ApplicationLabelCache(applicationSource)

    suspend fun load(): NezhaWidgetSnapshot {
        val session = vpnController.session.value
        val statistics = observationStore.statistics.value
        val rules = ruleStore.snapshot.value
        val blockedObservations = observationStore.recent.value.filter { observation -> observation.isBlocked }
        val labels = labelCache.resolve(
            blockedObservations.mapNotNull { observation -> observation.packageName }.toSet(),
        )
        val latest = blockedObservations.firstOrNull()
        return NezhaWidgetSnapshot(
            isRunning = session is VpnSessionState.Running,
            isTransitioning = session is VpnSessionState.Starting || session is VpnSessionState.Stopping,
            hasFailed = session is VpnSessionState.Failed,
            observed = statistics.observed,
            blocked = statistics.blocked,
            relayed = statistics.relayed,
            distinctBlockedHosts = statistics.sessionDistinctBlockedHosts,
            latestBlockedHost = latest?.host,
            latestBlockedApp = latest?.packageName?.let { name -> labels[name] ?: name },
            protectedApplicationCount = protectedApplicationStore.protectedPackages.value.size,
            builtinRuleCount = rules.builtinRuleCount,
            userRuleCount = rules.userRuleCount,
            topBlockedApplications = blockedObservations.toTopApplications(labels),
            trendHasBlock = observationStore.recent.value.toTrendSlices(),
        )
    }

    /** 按应用汇总拦截次数并取前几名。归属不到应用时如实显示「未知」，不猜一个名字。 */
    private fun List<DomainObservation>.toTopApplications(
        labels: Map<String, String>,
    ): List<NezhaWidgetSnapshot.TopApplication> = groupingBy { observation -> observation.packageName }
        .eachCount()
        .entries
        .sortedByDescending { entry -> entry.value }
        .take(TOP_APPLICATION_LIMIT)
        .map { entry ->
            val label = entry.key?.let { name -> labels[name] ?: name } ?: unknownAppLabel
            NezhaWidgetSnapshot.TopApplication(label = label, blocked = entry.value.toLong())
        }

    /**
     * 把最近窗口等分成若干段，标记每段里是否出现过拦截。
     *
     * 样本不足时不画：一段只有两三条观测的「趋势」看着像结论，其实只是噪音。
     * 这与统计页趋势图用的是同一条口径（工程规则第 45 节）。
     */
    private fun List<DomainObservation>.toTrendSlices(): List<Boolean> = if (size < TREND_SLICES) {
        emptyList()
    } else {
        val sliceSize = ceil(size.toDouble() / TREND_SLICES.toDouble()).toInt().coerceAtLeast(1)
        (0 until TREND_SLICES).map { index ->
            val start = index * sliceSize
            val end = minOf(start + sliceSize, size)
            start < end && subList(start, end).any { observation -> observation.isBlocked }
        }
    }
}
