package xin.ctkqiang.nezha_cyber.ads_block.domain.analysis

/**
 * 静态分析得到的候选域名。
 *
 * 「候选」是刻意的措辞，也是这一层唯一允许的措辞：它只说明**APK 的字符串池里出现了这个域名**，
 * 既不等于它真的会联网，更不等于它是广告（工程规则第 12、32 节）。
 *
 * [occurrences] 是该域名在字符串池中出现的次数。次数高通常意味着它是被反复引用的常量，
 * 比只出现一次的字符串更可能是真实端点——但这只是排序依据，不是判定依据。
 */
data class DomainCandidate(val host: String, val occurrences: Int)
