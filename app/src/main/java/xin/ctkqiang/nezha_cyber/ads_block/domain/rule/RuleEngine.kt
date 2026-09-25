package xin.ctkqiang.nezha_cyber.ads_block.domain.rule

/**
 * 端口：域名过滤决策。
 *
 * 工程规则第 9 节要求所有过滤决策都经过一个集中的规则引擎，不允许把 `if (domain == "...")`
 * 散落在代码库里，这个接口就是那个唯一入口。
 *
 * 与第 9 节示例的差异需要说明：示例里带 `packageName` 参数，这里没有。原因是当前规则模型
 * 不含「按应用区分」的维度——按应用限制由隧道层的路由（`addAllowedApplication`）完成。
 * 传一个被实现忽略的参数会制造「已经支持按应用规则」的假象，所以不传。
 */
interface RuleEngine {
    /**
     * 对一次域名查询求决策。
     *
     * 必须是快速且无副作用的：它在每个 DNS 查询上被调用，内部只做内存索引查找（第 22 节）。
     */
    fun evaluate(host: String): FilterDecision
}
