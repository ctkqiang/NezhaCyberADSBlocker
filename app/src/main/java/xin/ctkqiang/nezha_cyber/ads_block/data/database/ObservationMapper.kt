package xin.ctkqiang.nezha_cyber.ads_block.data.database

import java.time.Instant
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.DomainObservation
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleAction
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleSource

/**
 * 观测记录在数据库行与领域模型之间的映射。
 *
 * 时间存 epoch 毫秒而不是格式化字符串：排序与范围查询都依赖它，而文本时间戳在跨时区与
 * 夏令时下会排错。领域层仍是 `Instant`，时区只在界面格式化那一步出现。
 *
 * 枚举存名字而不是序号：序号会在枚举顺序调整时把历史数据解释成另一个意思。
 *
 * 解析失败返回 null 而不是抛异常：表里出现无法识别的动作名，只可能来自更高版本写下的数据
 * 或手工改动过的库文件，跳过这一行比让整份记录读不出来更合理（工程规则第 29 节）。
 */
internal fun DomainObservation.toEntity(): ObservationEntity = ObservationEntity(
    observedAt = at.toEpochMilli(),
    host = host,
    action = action.name,
    matchedRule = matchedRule,
    source = source?.name,
    packageName = packageName,
)

internal fun ObservationEntity.toDomain(): DomainObservation? {
    val parsedAction = action.toEnumOrNull<RuleAction>() ?: return null
    return DomainObservation(
        at = Instant.ofEpochMilli(observedAt),
        host = host,
        action = parsedAction,
        matchedRule = matchedRule,
        // 来源解析不出来时按「没有来源」处理，而不是丢掉整条观测：域名与动作仍然成立，
        // 而「是不是内置规则拦的」只是一个补充说明。
        source = source?.toEnumOrNull<RuleSource>(),
        packageName = packageName,
    )
}

/**
 * 按名字解析枚举，大小写不敏感。
 *
 * 数值来自我们自己的数据库，本可以严格匹配；但规则文件那条读取路径是大小写不敏感的，
 * 两条路径保持同一套宽容度，就不会出现「同一个值在一边读得出来、另一边读不出来」。
 */
private inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? =
    enumValues<T>().firstOrNull { value -> value.name.equals(this, ignoreCase = true) }
