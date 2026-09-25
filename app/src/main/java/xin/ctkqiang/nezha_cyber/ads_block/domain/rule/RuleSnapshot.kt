package xin.ctkqiang.nezha_cyber.ads_block.domain.rule

/**
 * 规则快照。
 *
 * 规则引擎与界面都只读快照，不读可变集合：规则是低频变更、高频读取的数据，
 * 用不可变快照可以把「读到一半被改掉」这类问题从源头去掉。
 *
 * 刻意不加 Compose 的 `@Immutable`：domain 不得依赖 Compose（工程规则第 38.3 节）。
 * 不可变性由 `val` 与只读集合本身保证，与注解无关。
 *
 * 两个计数在构造时算一次并存下来，而不是写成 `get()`：清单有数万条，
 * 每次重组都遍历一遍是不可接受的开销。
 *
 * [builtinVersion] 是 assets/domains.version 的当前值，用于判断是否需要重新导入（第 39.3 节）。
 */
data class RuleSnapshot(val rules: List<DomainRule>, val builtinVersion: Int) {
    val userRuleCount: Int = rules.count { rule -> rule.source == RuleSource.USER }

    val builtinRuleCount: Int = rules.count { rule -> rule.source == RuleSource.BUILTIN }

    companion object {
        val Empty = RuleSnapshot(rules = emptyList(), builtinVersion = 0)
    }
}
