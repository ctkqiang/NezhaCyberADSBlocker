package xin.ctkqiang.nezha_cyber.ads_block.ui.data

import androidx.compose.runtime.staticCompositionLocalOf
import xin.ctkqiang.nezha_cyber.ads_block.domain.notification.NotificationAccessSource

/**
 * 通知访问权限状态的注入点。
 *
 * 与 VPN 授权不同，通知使用权**没有可请求的对话框**：它只能由用户在系统设置里开启，
 * 因此这个端口只有「查询」与「刷新」，跳转设置页由 `NotificationAccessLauncher` 承担。
 */
val LocalNotificationAccessSource = staticCompositionLocalOf<NotificationAccessSource> {
    error("LocalNotificationAccessSource 未提供：请在组合根补上 CompositionLocalProvider")
}
