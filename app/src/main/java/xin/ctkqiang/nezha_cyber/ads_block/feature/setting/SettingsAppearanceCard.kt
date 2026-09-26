package xin.ctkqiang.nezha_cyber.ads_block.feature.setting

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import xin.ctkqiang.nezha_cyber.ads_block.R
import xin.ctkqiang.nezha_cyber.ads_block.domain.appearance.ThemePreference
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaSegmentedControl
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaSurfaceCard
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme

/**
 * 外观：主题三档。
 *
 * 单独成文件而不是留在设置页里：设置页已经承载了版本、开发者、隐私、接管范围四块内容，
 * 再往里塞具体控件会让那个文件同时承担「页面编排」与「单块渲染」两件事。
 *
 * 选项顺序固定为「跟随系统 / 浅色 / 深色」，且默认选中第一档。把「跟随系统」放最后
 * 会让默认态看起来像一个需要用户去改的中间选项，而它其实是最合理的默认。
 */
@Composable
internal fun SettingsAppearanceCard(uiState: SettingsUiState, onIntent: (SettingsUiIntent) -> Unit) {
    val palette = NezhaTheme.palette
    NezhaSurfaceCard {
        BasicText(
            text = stringResource(R.string.settings_appearance_title),
            style = NezhaTheme.typography.title.copy(color = palette.textPrimary),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        NezhaSegmentedControl(
            items = ThemePreference.entries,
            selected = uiState.themePreference,
            label = { option ->
                stringResource(
                    when (option) {
                        ThemePreference.System -> R.string.settings_theme_system
                        ThemePreference.Light -> R.string.settings_theme_light
                        ThemePreference.Dark -> R.string.settings_theme_dark
                    },
                )
            },
            onSelect = { option -> onIntent(SettingsUiIntent.SetThemePreference(option)) },
        )
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        BasicText(
            text = stringResource(R.string.settings_appearance_note),
            style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
        )
    }
}
