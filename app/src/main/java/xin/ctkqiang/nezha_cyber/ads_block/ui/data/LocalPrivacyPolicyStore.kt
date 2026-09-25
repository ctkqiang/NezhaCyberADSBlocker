package xin.ctkqiang.nezha_cyber.ads_block.ui.data

import androidx.compose.runtime.staticCompositionLocalOf
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.PrivacyPolicyStore

/**
 * 隐私策略端口的注入点。
 *
 * 读取方有三处：设置页（编辑）、实时流水页与统计页（只需知道「记录是否开着」，
 * 关掉时它们要如实说明自己为什么是空的）。观测存储与 DNS 中继那一侧由 `AppContainer`
 * 直接订阅同一个 StateFlow，不经过组合局部值。
 */
val LocalPrivacyPolicyStore = staticCompositionLocalOf<PrivacyPolicyStore> {
    error("LocalPrivacyPolicyStore 未提供：请在组合根补上 CompositionLocalProvider")
}
