package xin.ctkqiang.nezha_cyber.ads_block.ui.data

import androidx.compose.runtime.staticCompositionLocalOf
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.ObservationStore

/**
 * 观测与统计端口的注入点。
 *
 * 与 `LocalVpnController` 同样的理由：界面只依赖领域端口，实现由组合根下发。
 * 刻意不提供默认值——缺失时必须立刻失败，否则统计页会显示成「全是 0」，
 * 而这看起来和「真的还没拦到任何东西」一模一样，属于最难排查的一类问题。
 */
val LocalObservationStore = staticCompositionLocalOf<ObservationStore> {
    error("LocalObservationStore 未提供：请在组合根补上 CompositionLocalProvider")
}
