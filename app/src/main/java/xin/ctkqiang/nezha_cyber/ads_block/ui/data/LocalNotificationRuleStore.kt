package xin.ctkqiang.nezha_cyber.ads_block.ui.data

import androidx.compose.runtime.staticCompositionLocalOf
import xin.ctkqiang.nezha_cyber.ads_block.domain.notification.NotificationRuleStore

/**
 * 通知拦截规则存储的注入点。
 *
 * 界面只读它的快照与调它的写方法，不直接接触数据库：`RoomNotificationRuleStore` 是实现，
 * 换实现时界面不需要改（工程规则第 40.3 节）。
 */
val LocalNotificationRuleStore = staticCompositionLocalOf<NotificationRuleStore> {
    error("LocalNotificationRuleStore 未提供：请在组合根补上 CompositionLocalProvider")
}
