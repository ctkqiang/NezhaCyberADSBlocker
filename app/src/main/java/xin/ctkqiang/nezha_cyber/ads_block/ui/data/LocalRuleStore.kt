package xin.ctkqiang.nezha_cyber.ads_block.ui.data

import androidx.compose.runtime.staticCompositionLocalOf
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleStore

/**
 * 规则存储端口的注入点。
 *
 * 与 `LocalVpnController` 同样的理由：界面只依赖领域端口，实现（文件存储）由组合根下发
 * （工程规则第 38.1、38.2 节）。刻意不提供默认值——缺失说明装配没完成，必须立刻失败，
 * 而不是退化成一个空实现让页面显示 0 条规则。
 */
val LocalRuleStore = staticCompositionLocalOf<RuleStore> {
    error("LocalRuleStore 未提供：请在组合根补上 CompositionLocalProvider")
}
