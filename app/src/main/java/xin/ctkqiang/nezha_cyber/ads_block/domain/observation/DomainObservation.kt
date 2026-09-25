package xin.ctkqiang.nezha_cyber.ads_block.domain.observation

import java.time.Instant
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleAction
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleSource

/**
 * 一次域名观测。
 *
 * 只记录元数据：域名、动作、命中规则与来源、时间、以及（在系统允许时）发起查询的应用。
 * 绝不记录查询内容本身之外的任何载荷——这条边界由工程规则第 20 节固定。
 *
 * [packageName] 为 null 表示无法归属到具体应用：Android 10 以下没有可用的归属接口，
 * 此时界面必须如实显示「未知来源」，不能猜一个应用出来（第 32 节）。
 */
data class DomainObservation(
    val at: Instant,
    val host: String,
    val action: RuleAction,
    val matchedRule: String?,
    val source: RuleSource?,
    val packageName: String?,
) {
    /** 是否被拦截。未命中任何规则时动作是 ALLOW，但那不等于「用户放行」。 */
    val isBlocked: Boolean
        get() = action == RuleAction.BLOCK
}
