package xin.ctkqiang.nezha_cyber.ads_block.data.database

import xin.ctkqiang.nezha_cyber.ads_block.domain.notification.NotificationRule
import xin.ctkqiang.nezha_cyber.ads_block.domain.notification.NotificationRuleId

/**
 * 通知规则在数据库行与领域模型之间的映射。
 *
 * 表里四个字段都非空、也没有需要解析的枚举，因此映射是**全函数**：没有解析失败这一态，
 * 也就不需要返回可空值。这一点与观测记录的映射不同——那边的动作名可能来自更高版本写下的数据。
 */
internal fun NotificationRuleEntity.toDomain(): NotificationRule = NotificationRule(
    id = NotificationRuleId(id),
    packageName = packageName,
    matchText = matchText,
    enabled = enabled,
)
