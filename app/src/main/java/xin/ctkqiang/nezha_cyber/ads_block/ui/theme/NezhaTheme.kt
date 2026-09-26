package xin.ctkqiang.nezha_cyber.ads_block.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * 自绘主题。
 *
 * 本工程不使用 Material，因此主题只承担两件事：向下提供配色与字体，并在明暗之间切换。
 * 组件通过 [palette] 与 [typography] 取值，不直接引用 CompositionLocal。
 */
object NezhaTheme {
    val palette: NezhaPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalNezhaPalette.current

    val typography: NezhaTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalNezhaTypography.current

    /**
     * 窗口底色。
     *
     * 给 Activity 在装载 Compose 之前设置窗口背景用：**首帧之前，系统栏区域显示的就是这个颜色**。
     * 它若与主题里的 `background` 不一致，启动瞬间能在底部看到一条色差带。
     *
     * 与 `res/values{,-night}/colors.xml` 的 `nezha_window_background` 是同一个值的两个来源，
     * 各自解决一半场景：那份资源按**系统**明暗取值，用于系统自己决定窗口底色的场合；
     * 这个函数按**应用主题**取值，用于用户锁定浅色或深色的场合——那时系统明暗与界面明暗不一致，
     * 只靠资源会取到错的那一个。改动其中一处必须同时改另一处。
     */
    fun windowBackgroundColor(darkTheme: Boolean): Color =
        if (darkTheme) darkNezhaPalette.background else lightNezhaPalette.background

    @Composable
    operator fun invoke(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalNezhaPalette provides if (darkTheme) darkNezhaPalette else lightNezhaPalette,
            LocalNezhaTypography provides nezhaTypography,
            content = content,
        )
    }
}
