package xin.ctkqiang.nezha_cyber.ads_block.domain.observation

/**
 * 过滤统计。
 *
 * 口径按工程规则第 24 节固定为「已观测 / 已拦截 / 已放行」：
 * [observed] 是全部查询数，[blocked] 是被规则拦下的数量，[relayed] 是其余被转发出去的数量。
 *
 * 两类指标刻意分开标注，因为它们的时间范围不同，混在一起会让人误读：
 * - [observed] 与 [blocked] 跨会话累计，持久化在本机；
 * - [sessionDistinctBlockedHosts] 只统计本次会话，重启归零——去重要靠全量域名集合，
 *   长期持有它会随使用时间无界增长，因此不做跨会话累计。
 *
 * [relayed] 是派生值：它恒等于 observed - blocked，单独存一份只会多一个不一致的机会。
 * 「放行」也不等于「用户放行」：绝大多数转发出去的查询只是没有命中任何规则（第 32 节）。
 */
data class FilteringStatistics(
    val observed: Long = 0,
    val blocked: Long = 0,
    val sessionDistinctBlockedHosts: Int = 0,
) {
    val relayed: Long
        get() = observed - blocked
}
