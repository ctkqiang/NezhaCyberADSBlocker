package xin.ctkqiang.nezha_cyber.ads_block.domain.rule

/**
 * 端口：域名过滤决策。
 *
 * 工程规则第 9 节要求所有过滤决策都经过一个集中的规则引擎，不允许把 `if (domain == "...")`
 * 散落在代码库里，这个接口就是那个唯一入口。
 */
interface RuleEngine {
    /**
     * 对一次域名查询求决策。
     *
     * [packageName] 是发起查询的应用包名，由隧道层的连接归属反查得到；无法归属时为 null。
     * 它只被「按应用生效」的内置规则（[RuleSource.APP_ADS]）使用，其余档位与它无关，
     * 因此传 null 不会削弱全局规则，只会让应用专属规则不命中——这是刻意选择的失败方向。
     *
     * 必须是快速且无副作用的：它在每个 DNS 查询上被调用，内部只做内存索引查找（第 22 节）。
     */
    fun evaluate(host: String, packageName: String? = null): FilterDecision
}
