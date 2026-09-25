package xin.ctkqiang.nezha_cyber.ads_block

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import xin.ctkqiang.nezha_cyber.ads_block.ui.notification.LocalNotificationAccessLauncher
import xin.ctkqiang.nezha_cyber.ads_block.ui.notification.NotificationAccessLauncher

/**
 * 通知使用权设置页跳转的组合根。
 *
 * 与 `AnalysisCompositionRoot` 同理：拉起另一个应用（这里是系统设置）的界面只能由上层完成，
 * 而 ViewModel 侧只发一条「请打开设置」的意图。因此这里是 [NotificationAccessLauncher]
 * 的实现点，`NotificationRulesScreen` 与它下面的任何代码都不出现 `Intent`（工程规则第 40.4 节）。
 *
 * 本根**只**提供跳转能力，不提供规则存储与权限查询——那两项是数据端口，属于
 * `NezhaDataCompositionRoot`。分开是因为它们的来源不同：一个是每次组合时构造的界面能力，
 * 另一个是 Application 持有的长期对象。混在一个 Provider 里，读的人会以为它们同生命周期。
 */
@Composable
internal fun NotificationCompositionRoot(content: @Composable () -> Unit) {
    val context = LocalContext.current
    // 以 context 为 key 记住：主题或配置变化会换掉 Activity 与它的 context，旧引用不能继续用。
    val launcher = remember(context) {
        NotificationAccessLauncher { context.openNotificationListenerSettings() }
    }
    CompositionLocalProvider(LocalNotificationAccessLauncher provides launcher, content = content)
}

/**
 * 打开系统的「通知使用权」列表页。
 *
 * 用列表页而不是本应用自己的详情页：详情页的 action 在 API 30 才存在，而列表页自 API 22 起
 * 一直可用，本应用的 minSdk 是 26。少一个版本分支，也少一处只在部分设备上才被走到、
 * 因而最难被发现的代码。
 *
 * `FLAG_ACTIVITY_NEW_TASK` 是刻意的：这里的 context 通常是 Activity，但把它当成 Activity
 * 来用是一个**隐含假设**——将来若从 Service 或 Application 触发跳转，没有这个标志就会直接抛异常。
 * 加上它，两种来源都成立。
 *
 * **不捕获 `ActivityNotFoundException`。** 这个 action 从 API 22 起由系统设置应用提供，
 * 缺失属于设备本身不合规；而 `NotificationAccessLauncher` 的契约没有回报失败的位置，
 * 捕获后唯一能做的就是不声不响地什么都不做——那会把「设备坏了」伪装成「按钮没反应」，
 * 是比崩溃更难排查的形态（工程规则第 37.3 节）。
 */
private fun Context.openNotificationListenerSettings() {
    startActivity(
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
