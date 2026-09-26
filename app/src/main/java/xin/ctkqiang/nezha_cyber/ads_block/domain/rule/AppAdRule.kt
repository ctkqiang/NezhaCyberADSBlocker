package xin.ctkqiang.nezha_cyber.ads_block.domain.rule

/**
 * 一条「按应用生效」的内置广告域名规则。
 *
 * [host] 与 [DomainRule.host] 同构：小写、不含结尾点、通配符只允许 `*.` 前缀。
 * [packageName] 是 Android 包名，规则只在发起查询的应用等于它时参与匹配。
 *
 * 单独建模而不是给 [DomainRule] 补一个可空的包名字段：绝大多数域名规则是全局的，
 * 让它们都挂一个恒为 null 的字段，只会让「这条规则到底对谁生效」在阅读时变得含糊
 * （工程规则第 37.2 节：按语义选择数据结构）。
 */
data class AppAdRule(val packageName: String, val host: String)
