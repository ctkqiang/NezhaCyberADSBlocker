package xin.ctkqiang.nezha_cyber.ads_block.ui.privacy

/**
 * 跳转到系统的隐私相关设置页。
 *
 * 与 `ApkPicker`、`NotificationAccessLauncher` 同类：界面需要、但实现必须留在应用模块的能力。
 * 组合根把系统 `Intent` 包成这个接口下发，因此界面与 ViewModel 里都不出现 `Intent`
 * （工程规则第 40.4 节）。
 *
 * 这里提供跳转而不是提供开关，是这项功能唯一诚实的形态：Android 不允许普通应用改动其它应用的
 * 授权状态，真正能撤销权限的只有用户本人在系统设置里操作。摆一个点不动的开关，
 * 比不放开关更糟——用户会以为已经被保护了（工程规则第 32 节）。
 */
interface SystemSettingsLauncher {
    /** 系统是否提供隐私仪表盘。`ACTION_PRIVACY_SETTINGS` 自 API 31 起才存在。 */
    val canOpenPrivacyDashboard: Boolean

    /** 打开某个应用的系统信息页，用户在那里可以逐项关闭权限。 */
    fun openApplicationSettings(packageName: String)

    /** 打开系统的隐私仪表盘，查看近期哪些应用用过相机、麦克风与位置。 */
    fun openPrivacyDashboard()

    /**
     * 打开开发者选项。
     *
     * 系统的「传感器已关闭」快捷开关藏在开发者选项里，而这是唯一能真正停掉传感器的机制，
     * 因此这里提供的是一条通往它的路径，而不是假装本应用能代劳。
     */
    fun openDeveloperOptions()
}
