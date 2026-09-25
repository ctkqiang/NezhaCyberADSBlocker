package xin.ctkqiang.nezha_cyber.ads_block.ui.notification

/**
 * 跳转到系统的「通知使用权」设置页。
 *
 * 与 `ApkPicker`、`VpnAuthorizationRequester` 同类：界面需要、但实现必须留在应用模块的能力。
 * ViewModel 只发意图，拉起系统界面这一步由组合根注入的实现完成，因此 ViewModel 里不出现
 * 任何 Android 的 `Intent`（工程规则第 40.4 节）。
 */
fun interface NotificationAccessLauncher {
    fun launch()
}
