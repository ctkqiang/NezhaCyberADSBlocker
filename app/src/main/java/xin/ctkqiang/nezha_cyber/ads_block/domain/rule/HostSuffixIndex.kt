package xin.ctkqiang.nezha_cyber.ads_block.domain.rule

/**
 * 域名后缀索引。
 *
 * 把一组域名规则编译成「按标签数」次哈希查找，单次查询开销与规则条数无关（工程规则第 22 节）：
 * 精确规则进一个集合，通配规则只留下父域进另一个集合，查询时从最长后缀逐级剥离、命中即返回，
 * 因此第一次命中天然就是最具体的那条规则。10 万条规则与 10 条规则的查询开销相同。
 *
 * 通配规则只匹配**严格子域**：`*.ads.example.com` 命中 `a.ads.example.com`，但不命中
 * `ads.example.com` 本身。裸域往往正是该服务的正式入口，误拦的代价远大于漏拦一条广告子域。
 *
 * 抽成独立类型而不是让每个调用点各写一份：域名匹配逻辑一旦复制就会各自漂移，而漂移在
 * 广告拦截里表现为「有时拦、有时不拦」，是最难复现的一类缺陷（工程规则第 28 节）。
 */
class HostSuffixIndex(hosts: Collection<String>) {
    private val exactHosts: Set<String> = hosts.asSequence()
        .filterNot { host -> host.startsWith(WILDCARD_PREFIX) }
        .toHashSet()

    private val wildcardParents: Set<String> = hosts.asSequence()
        .filter { host -> host.startsWith(WILDCARD_PREFIX) }
        .map { host -> host.removePrefix(WILDCARD_PREFIX) }
        .toHashSet()

    /** 命中返回规则原文（通配规则带 `*.` 前缀）；未命中返回 null。 */
    fun match(host: String): String? {
        var suffix = host
        var isHostItself = true
        while (true) {
            val matched = when {
                suffix in exactHosts -> suffix
                !isHostItself && suffix in wildcardParents -> "$WILDCARD_PREFIX$suffix"
                else -> null
            }
            if (matched != null) {
                return matched
            }
            val separator = suffix.indexOf('.')
            if (separator < 0) {
                return null
            }
            suffix = suffix.substring(separator + 1)
            isHostItself = false
        }
    }
}
