package xin.ctkqiang.nezha_cyber.ads_block

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import xin.ctkqiang.nezha_cyber.ads_block.ui.privacy.LocalSystemSettingsLauncher
import xin.ctkqiang.nezha_cyber.ads_block.ui.privacy.SystemSettingsLauncher

/** 应用信息页的 data scheme。系统用 `package:<包名>` 定位到具体应用。 */
private const val PACKAGE_SCHEME = "package"

/**
 * 系统隐私设置页跳转的组合根。
 *
 * 与 `NotificationCompositionRoot` 同理：拉起系统界面只能由上层完成，而 ViewModel 侧只表达
 * 「需要用户去系统里做一件事」。这里就是 [SystemSettingsLauncher] 的实现点。
 *
 * 三个跳转都用 `FLAG_ACTIVITY_NEW_TASK`：这里的 context 通常是 Activity，但把它当成 Activity
 * 用是一个**隐含假设**——将来若从 Service 或 Application 触发，没有这个标志会直接抛异常。
 * 加上它，两种来源都成立。
 *
 * **不捕获 `ActivityNotFoundException`。** 这几个 action 由系统设置应用提供，缺失属于设备本身
 * 不合规；而接口没有回报失败的位置，捕获后唯一能做的就是不声不响地什么都不做——那会把
 * 「设备坏了」伪装成「按钮没反应」，比崩溃更难排查（工程规则第 37.3 节）。
 */
@Composable
internal fun PrivacyCompositionRoot(content: @Composable () -> Unit) {
    val context = LocalContext.current
    // 以 context 为 key 记住：主题或配置变化会换掉 Activity 与它的 context，旧引用不能继续用。
    val launcher = remember(context) { AndroidSystemSettingsLauncher(context) }
    CompositionLocalProvider(LocalSystemSettingsLauncher provides launcher, content = content)
}

private class AndroidSystemSettingsLauncher(private val context: Context) : SystemSettingsLauncher {
    override val canOpenPrivacyDashboard: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    override fun openApplicationSettings(packageName: String) {
        launch(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts(PACKAGE_SCHEME, packageName, null)),
        )
    }

    override fun openPrivacyDashboard() {
        // 低版本没有这个页面，调用它只会落在一个不存在的 action 上，因此直接不发起。
        if (!canOpenPrivacyDashboard) return
        launch(Intent(Settings.ACTION_PRIVACY_SETTINGS))
    }

    override fun openDeveloperOptions() {
        launch(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
    }

    private fun launch(intent: Intent) {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
