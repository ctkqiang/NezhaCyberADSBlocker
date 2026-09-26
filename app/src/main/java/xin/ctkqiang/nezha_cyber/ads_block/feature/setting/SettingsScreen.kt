package xin.ctkqiang.nezha_cyber.ads_block.feature.setting

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import xin.ctkqiang.nezha_cyber.ads_block.BuildConfig
import xin.ctkqiang.nezha_cyber.ads_block.R
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.BlockedResponseMode
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.ObservationRetention
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaInfoRow
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaMetricRow
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaScreenScaffold
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaSegmentedControl
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaSurfaceCard
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaSwitch
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.LocalPrivacyPolicyStore
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.LocalRuleStore
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.LocalThemePreferenceStore
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.NezhaDataPreviewHost
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaThemePreview

/**
 * 设置：规则规模、接管范围，以及**用户可以自己定的隐私与安全规则**。
 *
 * 隐私区块里的三项都是真能生效的：
 * - 观测记录开关直接决定隧道是否留痕（关掉后过滤照常工作，见 `PrivacyPolicy` 的说明）；
 * - 观测保留量同时决定界面条数与磁盘日志行数上限；
 * - 拦截应答方式决定命中规则时回给客户端的应答形式。
 *
 * 页面里如实写明本应用**做不到**的事：无法修改其它应用的权限。把做不到的说清楚，
 * 比摆一排灰掉的开关更有用（工程规则第 32 节）。
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val ruleStore = LocalRuleStore.current
    val privacyPolicyStore = LocalPrivacyPolicyStore.current
    val themePreferenceStore = LocalThemePreferenceStore.current
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(
            ruleStore = ruleStore,
            privacyPolicyStore = privacyPolicyStore,
            themePreferenceStore = themePreferenceStore,
        ),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsContent(uiState = uiState, onIntent = viewModel::dispatch, modifier = modifier)
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onIntent: (SettingsUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = NezhaTheme.palette
    NezhaScreenScaffold(modifier = modifier) {
        SettingsAppearanceCard(uiState = uiState, onIntent = onIntent)
        Spacer(modifier = Modifier.height(NezhaDimens.sectionGap))
        AboutCard(uiState = uiState)
        Spacer(modifier = Modifier.height(NezhaDimens.sectionGap))
        DeveloperCard()
        Spacer(modifier = Modifier.height(NezhaDimens.sectionGap))
        PrivacyControlsCard(uiState = uiState, onIntent = onIntent)
        Spacer(modifier = Modifier.height(NezhaDimens.sectionGap))
        ScopeCard()
        Spacer(modifier = Modifier.height(NezhaDimens.sectionGap))
        NoteCard(
            title = stringResource(R.string.settings_keyword_title),
            note = stringResource(R.string.settings_keyword_note),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.sectionGap))
        NoteCard(
            title = stringResource(R.string.settings_privacy_title),
            note = stringResource(R.string.settings_privacy_note),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.sectionGap))
        BasicText(
            text = stringResource(R.string.settings_pending_note),
            style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
        )
    }
}

@Composable
private fun AboutCard(uiState: SettingsUiState) {
    val palette = NezhaTheme.palette
    NezhaSurfaceCard {
        BasicText(
            text = stringResource(R.string.settings_about_title),
            style = NezhaTheme.typography.title.copy(color = palette.textPrimary),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        NezhaMetricRow(
            label = stringResource(R.string.settings_app_version),
            value = BuildConfig.VERSION_NAME,
            highlight = false,
        )
        NezhaMetricRow(
            label = stringResource(R.string.statistics_builtin_rules),
            value = stringResource(
                R.string.statistics_builtin_rules_value,
                uiState.builtinVersion,
                uiState.builtinRuleCount,
            ),
            highlight = false,
        )
        NezhaMetricRow(
            label = stringResource(R.string.statistics_user_rules),
            value = stringResource(R.string.statistics_count_value, uiState.userRuleCount),
            highlight = false,
        )
    }
}

/**
 * 关于开发者。
 *
 * 作者与来源信息是开源项目规范的一部分（工程规则第 0.1 节），因此常驻设置页而不是藏进某个二级页。
 * 包名取 `BuildConfig.APPLICATION_ID`、应用名取 `R.string.app_name`，都不再抄一份字面量：
 * 这一页存在的意义就是给出可核对的准确信息，抄一份就意味着改包名或改名后它会静默地对不上。
 */
@Composable
private fun DeveloperCard() {
    val palette = NezhaTheme.palette
    val applicationName = stringResource(R.string.settings_dev_app_name_value, stringResource(R.string.app_name))
    val rows = listOf(
        R.string.settings_dev_app_name to applicationName,
        R.string.settings_dev_package to BuildConfig.APPLICATION_ID,
        R.string.settings_dev_author to stringResource(R.string.settings_dev_author_value),
        R.string.settings_dev_contact to stringResource(R.string.settings_dev_contact_value),
        R.string.settings_dev_repository to stringResource(R.string.settings_dev_repository_value),
        R.string.settings_dev_license to stringResource(R.string.settings_dev_license_value),
        R.string.settings_dev_audience to stringResource(R.string.settings_dev_audience_value),
    )
    NezhaSurfaceCard {
        BasicText(
            text = stringResource(R.string.settings_dev_title),
            style = NezhaTheme.typography.title.copy(color = palette.textPrimary),
        )
        rows.forEach { (labelRes, value) ->
            Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
            NezhaInfoRow(label = stringResource(labelRes), value = value)
        }
    }
}

/** 隐私与安全：三项可执行的规则，外加一句本应用做不到什么。 */
@Composable
private fun PrivacyControlsCard(uiState: SettingsUiState, onIntent: (SettingsUiIntent) -> Unit) {
    val palette = NezhaTheme.palette
    NezhaSurfaceCard {
        BasicText(
            text = stringResource(R.string.settings_privacy_controls_title),
            style = NezhaTheme.typography.title.copy(color = palette.textPrimary),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = stringResource(R.string.settings_privacy_logging),
                modifier = Modifier.weight(1f),
                style = NezhaTheme.typography.body.copy(color = palette.textPrimary),
            )
            NezhaSwitch(
                checked = uiState.isObservationLoggingEnabled,
                onCheckedChange = { enabled ->
                    onIntent(SettingsUiIntent.SetObservationLoggingEnabled(enabled))
                },
            )
        }
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        BasicText(
            text = stringResource(R.string.settings_privacy_logging_note),
            style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.sectionGap))
        RetentionSection(
            retention = uiState.observationRetention,
            onSelect = { retention -> onIntent(SettingsUiIntent.SetObservationRetention(retention)) },
        )
        Spacer(modifier = Modifier.height(NezhaDimens.sectionGap))
        BlockedResponseSection(
            mode = uiState.blockedResponseMode,
            onSelect = { mode -> onIntent(SettingsUiIntent.SetBlockedResponseMode(mode)) },
        )
        Spacer(modifier = Modifier.height(NezhaDimens.sectionGap))
        BasicText(
            text = stringResource(R.string.settings_permission_boundary_note),
            style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
        )
    }
}

/**
 * 观测保留量。
 *
 * 选项文案直接显示条数而不是「少 / 中 / 多」：用户要判断的正是「留多少」，
 * 而数字来自枚举本身（`ObservationRetention.capacity`），因此界面不会和实际值说两套话。
 */
@Composable
private fun RetentionSection(retention: ObservationRetention, onSelect: (ObservationRetention) -> Unit) {
    val palette = NezhaTheme.palette
    BasicText(
        text = stringResource(R.string.settings_privacy_retention),
        style = NezhaTheme.typography.body.copy(color = palette.textPrimary),
    )
    Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
    NezhaSegmentedControl(
        items = ObservationRetention.entries,
        selected = retention,
        label = { option -> stringResource(R.string.settings_retention_option, option.capacity) },
        onSelect = onSelect,
    )
    Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
    BasicText(
        text = stringResource(R.string.settings_privacy_retention_note),
        style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
    )
}

/** 命中规则时的应答方式。三种都在本机合成，差别只在客户端怎么理解这次失败。 */
@Composable
private fun BlockedResponseSection(mode: BlockedResponseMode, onSelect: (BlockedResponseMode) -> Unit) {
    val palette = NezhaTheme.palette
    BasicText(
        text = stringResource(R.string.settings_blocked_response_title),
        style = NezhaTheme.typography.body.copy(color = palette.textPrimary),
    )
    Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
    NezhaSegmentedControl(
        items = BlockedResponseMode.entries,
        selected = mode,
        label = { option ->
            stringResource(
                when (option) {
                    BlockedResponseMode.NxDomain -> R.string.settings_blocked_response_nxdomain
                    BlockedResponseMode.Refused -> R.string.settings_blocked_response_refused
                    BlockedResponseMode.ZeroAddress -> R.string.settings_blocked_response_zero
                },
            )
        },
        onSelect = onSelect,
    )
    Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
    BasicText(
        text = stringResource(R.string.settings_blocked_response_note),
        style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
    )
}

@Composable
private fun ScopeCard() {
    val palette = NezhaTheme.palette
    NezhaSurfaceCard {
        BasicText(
            text = stringResource(R.string.home_scope_title),
            style = NezhaTheme.typography.title.copy(color = palette.textPrimary),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        BasicText(
            text = stringResource(R.string.home_scope_value),
            style = NezhaTheme.typography.label.copy(color = palette.brand),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        BasicText(
            text = stringResource(R.string.home_scope_note),
            style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
        )
    }
}

@Composable
private fun NoteCard(title: String, note: String) {
    val palette = NezhaTheme.palette
    NezhaSurfaceCard {
        BasicText(
            text = title,
            style = NezhaTheme.typography.title.copy(color = palette.textPrimary),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        BasicText(
            text = note,
            style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
        )
    }
}

@Preview(name = "设置 · 明暗对照", showBackground = true, widthDp = 800, heightDp = 1600)
@Composable
private fun SettingsScreenPreview() {
    NezhaThemePreview {
        NezhaDataPreviewHost {
            SettingsScreen()
        }
    }
}
