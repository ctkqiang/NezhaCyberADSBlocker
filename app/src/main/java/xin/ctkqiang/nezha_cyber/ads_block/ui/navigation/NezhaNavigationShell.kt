package xin.ctkqiang.nezha_cyber.ads_block.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import xin.ctkqiang.nezha_cyber.ads_block.feature.analysis.ApkAnalysisScreen
import xin.ctkqiang.nezha_cyber.ads_block.feature.application.ApplicationDetailScreen
import xin.ctkqiang.nezha_cyber.ads_block.feature.application.ApplicationsScreen
import xin.ctkqiang.nezha_cyber.ads_block.feature.home.HomeScreen
import xin.ctkqiang.nezha_cyber.ads_block.feature.network.NetworkActivityScreen
import xin.ctkqiang.nezha_cyber.ads_block.feature.notification.NotificationRulesScreen
import xin.ctkqiang.nezha_cyber.ads_block.feature.privacy.PrivacyAuditScreen
import xin.ctkqiang.nezha_cyber.ads_block.feature.rule.AllowlistScreen
import xin.ctkqiang.nezha_cyber.ads_block.feature.rule.BlocklistScreen
import xin.ctkqiang.nezha_cyber.ads_block.feature.setting.SettingsScreen
import xin.ctkqiang.nezha_cyber.ads_block.feature.statistic.StatisticsScreen
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaSegmentedControl
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.liquidGlassSource
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.rememberLiquidGlassBackdrop
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme

/**
 * 应用外壳：串联页头、页面内容与悬浮底栏。
 *
 * 布局采用「内容全屏 + 底栏浮层」而不是纵向排列，是为了让底栏真正悬浮在内容之上。
 * 内容侧需要预留的底部空间由 NezhaScreenScaffold 负责，两者通过 NezhaDimens 共享同一组数值。
 *
 * 这里没有引入 navigation-compose：本阶段的页面关系是「五个标签页 + 页内分段」，
 * 用状态即可完整表达，等出现深层跳转与返回栈需求时再评估替换（工程规则第 35.15 节）。
 */
@Composable
fun NezhaNavigationShell(modifier: Modifier = Modifier, viewModel: NezhaShellViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NezhaNavigationShellContent(
        uiState = uiState,
        onIntent = viewModel::dispatch,
        modifier = modifier,
    )
}

@Composable
internal fun NezhaNavigationShellContent(
    uiState: NezhaShellUiState,
    onIntent: (NezhaShellUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = NezhaTheme.palette
    val backdrop = rememberLiquidGlassBackdrop()
    val stateHolder = rememberSaveableStateHolder()
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // 底栏会采样这一层：它把自己覆盖的那块内容以模糊方式重画一遍，因此图标与文字
                // 保持锐利、背后的内容真的糊掉。底色必须在这里给出，理由见 liquidGlassSource。
                .liquidGlassSource(backdrop = backdrop, background = palette.background),
        ) {
            NezhaShellHeader(uiState = uiState, onIntent = onIntent)
            // 每个页面各自保留自己的滚动位置：切到别的标签再回来应当停在原处，而不是弹回顶部。
            // `when` 分支会让旧页面离开组合，rememberScrollState / rememberLazyListState 的值随之
            // 被丢弃，因此必须按页面键把状态显式存下来。键用页面名而不是序号：
            // 枚举顺序调整时序号会变，那会让状态错配到另一个页面上。
            stateHolder.SaveableStateProvider(uiState.selectedSection.name) {
                NezhaSectionHost(
                    uiState = uiState,
                    onIntent = onIntent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }
        FloatingNavigationBar(
            selectedTab = uiState.selectedTab,
            onTabSelected = { onIntent(NezhaShellUiIntent.SelectTab(it)) },
            backdrop = backdrop,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = NezhaDimens.floatingBarBottomMargin),
        )
    }
}

/**
 * 页头。
 *
 * 标签页下只有一个页面时直接显示页面标题；有多个页面时改用分段控件，由控件本身承担
 * 标题的作用，避免标题与选中项重复表达同一件事。
 *
 * 高度固定为 [NezhaDimens.headerHeight] 并让内容居中：两种页头的内容高度不同，
 * 若各自按内容撑开，切换标签时下方内容会跟着上下跳。
 */
@Composable
private fun NezhaShellHeader(uiState: NezhaShellUiState, onIntent: (NezhaShellUiIntent) -> Unit) {
    val palette = NezhaTheme.palette
    val sections = uiState.selectedTab.sections
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = NezhaDimens.screenHorizontalPadding)
            .height(NezhaDimens.headerHeight),
        verticalArrangement = Arrangement.Center,
    ) {
        if (sections.size > 1) {
            NezhaSegmentedControl(
                items = sections,
                selected = uiState.selectedSection,
                label = { stringResource(it.titleRes) },
                onSelect = { onIntent(NezhaShellUiIntent.SelectSection(it)) },
            )
        } else {
            BasicText(
                text = stringResource(uiState.selectedSection.titleRes),
                style = NezhaTheme.typography.display.copy(color = palette.textPrimary),
            )
        }
    }
}

/**
 * 页面宿主。
 *
 * 整份状态而不是单个页面枚举传进来：应用详情页需要知道**哪一个**应用，
 * 而选中项属于外壳状态。把状态拆成参数传会让「详情页的入参从哪来」这件事散在签名里。
 */
@Composable
private fun NezhaSectionHost(
    uiState: NezhaShellUiState,
    onIntent: (NezhaShellUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState.selectedSection) {
        NezhaSection.Home -> HomeScreen(modifier = modifier)
        NezhaSection.NetworkActivity -> NetworkActivityScreen(modifier = modifier)
        NezhaSection.ApplicationList -> ApplicationsScreen(
            modifier = modifier,
            onOpenDetail = { packageName -> onIntent(NezhaShellUiIntent.ShowApplicationDetail(packageName)) },
        )
        NezhaSection.ApplicationDetail -> ApplicationDetailScreen(
            packageName = uiState.selectedPackageName,
            modifier = modifier,
        )
        NezhaSection.ApkAnalysis -> ApkAnalysisScreen(modifier = modifier)
        NezhaSection.Blocklist -> BlocklistScreen(modifier = modifier)
        NezhaSection.Allowlist -> AllowlistScreen(modifier = modifier)
        NezhaSection.NotificationRules -> NotificationRulesScreen(modifier = modifier)
        NezhaSection.Statistics -> StatisticsScreen(modifier = modifier)
        NezhaSection.Settings -> SettingsScreen(modifier = modifier)
        NezhaSection.PrivacyAudit -> PrivacyAuditScreen(modifier = modifier)
    }
}
