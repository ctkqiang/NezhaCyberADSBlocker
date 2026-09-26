package xin.ctkqiang.nezha_cyber.ads_block.domain.appearance

/**
 * 外观主题偏好。
 *
 * 三档而不是一个布尔开关：用户需要的其实是三种意图——「跟着系统走」「我就是要浅色」
 * 「我就是要深色」。压成 `isDarkTheme: Boolean` 会丢掉「跟随系统」这一档，而它恰恰是默认值，
 * 也是绝大多数用户唯一会用的一档。
 *
 * [System] 是默认值：设备在夜间自动切换是系统级行为，应用没有理由去覆盖它。
 */
enum class ThemePreference {
    /** 跟随系统。默认值。 */
    System,

    /** 始终浅色。 */
    Light,

    /** 始终深色。 */
    Dark,
    ;

    /**
     * 在给定系统明暗下，本次是否使用深色主题。
     *
     * 参数是布尔而不是直接读系统状态：这样这一档语义是纯函数，可以脱离 Android 单测，
     * 而「系统当前是不是深色」由界面层用 `isSystemInDarkTheme()` 提供。
     */
    fun isDark(systemInDarkTheme: Boolean): Boolean = when (this) {
        System -> systemInDarkTheme
        Light -> false
        Dark -> true
    }
}
