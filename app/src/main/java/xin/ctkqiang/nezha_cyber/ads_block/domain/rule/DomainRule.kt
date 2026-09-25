package xin.ctkqiang.nezha_cyber.ads_block.domain.rule

/**
 * 一条域名规则。
 *
 * [host] 一律小写、不含结尾点、不含协议与路径；通配符只允许 `*.` 前缀（第 39.2 节）。
 * 归一化发生在构造之前的边界处，引擎内部因此不必反复处理大小写与尾点。
 *
 * [enabled] 是用户对某条规则的启停覆写。内置清单里的条目可以被用户停用，
 * 但停用不等于删除：观测记录与统计仍要能关联回这条规则（第 39.4 节）。
 */
data class DomainRule(val host: String, val action: RuleAction, val source: RuleSource, val enabled: Boolean = true)
