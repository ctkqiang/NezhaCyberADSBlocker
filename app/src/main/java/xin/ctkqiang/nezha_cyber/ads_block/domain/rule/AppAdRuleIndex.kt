package xin.ctkqiang.nezha_cyber.ads_block.domain.rule

/**
 * 应用专属广告规则的索引。
 *
 * 按包名分组，每组编译成一个 [HostSuffixIndex]，因此一次查询只多出「按包名的一次哈希查找」，
 * 与规则条数无关（工程规则第 22 节）。
 *
 * 包名未知时不命中任何规则，这是刻意选择的失败方向：归属查询在 Android 10 以下不可用，
 * 在那种设备上把应用专属规则套用到身份不明的流量上，等于让规则的作用域失控——
 * 宁可漏拦，也不越界。
 */
class AppAdRuleIndex(rules: List<AppAdRule>) {
    private val indexesByPackage: Map<String, HostSuffixIndex> = rules
        .groupBy { rule -> rule.packageName }
        .mapValues { (_, packageRules) -> HostSuffixIndex(packageRules.map { rule -> rule.host }) }

    /** 命中返回规则原文；包名未知或该包没有对应规则时返回 null。 */
    fun match(host: String, packageName: String?): String? =
        packageName?.let { target -> indexesByPackage[target] }?.match(host)
}
