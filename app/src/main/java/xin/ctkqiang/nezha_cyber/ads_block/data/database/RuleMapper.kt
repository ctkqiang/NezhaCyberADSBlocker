package xin.ctkqiang.nezha_cyber.ads_block.data.database

import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.DomainRule
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleAction
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleSource

/**
 * 规则在数据库行与领域模型之间的映射。
 *
 * [RuleEntity] 的动作与来源都解析不出来时返回 null：一条既不知道要放行还是要阻断、
 * 也不知道来自哪里的规则没有任何可用语义，交给调用方跳过并记录，好过让引擎拿到一条
 * 语义不明的规则后做出无法解释的决策。
 */
internal fun DomainRule.toEntity(): RuleEntity = RuleEntity(
    host = host,
    action = action.name,
    source = source.name,
    enabled = enabled,
)

internal fun RuleEntity.toDomain(): DomainRule? {
    val parsedAction = action.toEnumOrNull<RuleAction>() ?: return null
    val parsedSource = source.toEnumOrNull<RuleSource>() ?: return null
    return DomainRule(
        host = host,
        action = parsedAction,
        source = parsedSource,
        enabled = enabled,
    )
}

private inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? =
    enumValues<T>().firstOrNull { value -> value.name.equals(this, ignoreCase = true) }
