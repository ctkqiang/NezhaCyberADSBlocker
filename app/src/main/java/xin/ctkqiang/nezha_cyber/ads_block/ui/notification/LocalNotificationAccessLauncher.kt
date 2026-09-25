package xin.ctkqiang.nezha_cyber.ads_block.ui.notification

import androidx.compose.runtime.staticCompositionLocalOf

/** 通知使用权设置页跳转的注入点。 */
val LocalNotificationAccessLauncher = staticCompositionLocalOf<NotificationAccessLauncher> {
    error("LocalNotificationAccessLauncher 未提供：请在组合根补上 CompositionLocalProvider")
}
