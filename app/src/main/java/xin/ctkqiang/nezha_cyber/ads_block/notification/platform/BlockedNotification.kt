package xin.ctkqiang.nezha_cyber.ads_block.notification.platform

import xin.ctkqiang.nezha_cyber.ads_block.domain.notification.NotificationContent
import xin.ctkqiang.nezha_cyber.ads_block.domain.notification.NotificationRule
import xin.ctkqiang.nezha_cyber.ads_block.domain.notification.NotificationTextField

/**
 * 一条被拦截的通知，以及重新呈现它所需的一切。
 *
 * [notificationKey] 是系统给这条通知的标识，用它取消原通知（`cancelNotification(key)`）。
 * 不用「包名 + tag + id」那个三重参数的重载：它在 `api-versions.xml` 里标着 `deprecated="21"`，
 * 而 key 本身就是权威标识，拿它比自己再去拼一次更可靠。
 *
 * [originalId] 用于派生替代通知的标识，因此「同一条原通知再次被拦截」会覆盖上一次的替代通知，
 * 而不是在通知栏里越堆越多。
 */
internal data class BlockedNotification(
    val notificationKey: String,
    val originalId: Int,
    val original: NotificationContent,
    val rule: NotificationRule,
    val field: NotificationTextField,
)
