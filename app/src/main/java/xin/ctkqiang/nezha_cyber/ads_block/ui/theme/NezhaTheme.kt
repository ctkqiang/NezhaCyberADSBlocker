package xin.ctkqiang.nezha_cyber.ads_block.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

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

    @Composable
    operator fun invoke(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalNezhaPalette provides if (darkTheme) darkNezhaPalette else lightNezhaPalette,
            LocalNezhaTypography provides nezhaTypography,
            content = content,
        )
    }
}
