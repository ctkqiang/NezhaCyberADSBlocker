package xin.ctkqiang.nezha_cyber.ads_block.ui.data

import androidx.compose.runtime.staticCompositionLocalOf
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.KeywordPolicyStore

/**
 * 关键词策略端口的注入点。
 *
 * 读取方只有规则页（展示与编辑）。规则引擎那一侧由 `AppContainer` 直接订阅同一个
 * [KeywordPolicyStore.policy]，不经过组合局部值；两处订阅的是同一个 StateFlow，
 * 因此界面上刚改完的开关立即对隧道生效，不需要重启服务。
 */
val LocalKeywordPolicyStore = staticCompositionLocalOf<KeywordPolicyStore> {
    error("LocalKeywordPolicyStore 未提供：请在组合根补上 CompositionLocalProvider")
}
