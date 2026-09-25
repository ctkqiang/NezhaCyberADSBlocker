package xin.ctkqiang.nezha_cyber.ads_block.ui.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import xin.ctkqiang.nezha_cyber.ads_block.AppContainer
import xin.ctkqiang.nezha_cyber.ads_block.ui.application.LocalApplicationIconLoader

/**
 * 数据端口的组合根。
 *
 * 应用模块是唯一允许把领域端口与实现装配起来的地方（工程规则第 38.1 节），这里是那个点：
 * 容器由 `NezhaApplication` 持有，服务与界面因此共享同一份存储与规则索引，
 * 不会出现「界面显示 3 条规则、隧道按 0 条判定」这种不一致。
 */
@Composable
internal fun NezhaDataCompositionRoot(container: AppContainer, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalRuleStore provides container.ruleStore,
        LocalKeywordPolicyStore provides container.keywordPolicyStore,
        LocalPrivacyPolicyStore provides container.privacyPolicyStore,
        LocalObservationStore provides container.observationStore,
        LocalInstalledApplicationSource provides container.installedApplicationSource,
        LocalApplicationPermissionSource provides container.applicationPermissionSource,
        LocalProtectedApplicationStore provides container.protectedApplicationStore,
        LocalNotificationRuleStore provides container.notificationRuleStore,
        LocalNotificationAccessSource provides container.notificationAccessSource,
        LocalApkAnalyzer provides container.apkAnalyzer,
        LocalApplicationIconLoader provides container.applicationIconLoader,
        content = content,
    )
}
