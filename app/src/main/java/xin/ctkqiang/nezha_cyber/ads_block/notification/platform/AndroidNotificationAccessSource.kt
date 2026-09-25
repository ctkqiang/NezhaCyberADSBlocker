package xin.ctkqiang.nezha_cyber.ads_block.notification.platform

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xin.ctkqiang.nezha_cyber.ads_block.domain.notification.NotificationAccessSource

/**
 * 通知访问权限的实现。
 *
 * 用 `NotificationManagerCompat.getEnabledListenerPackages` 而不是直接读
 * `Settings.Secure` 里的那串字符串：后者是一个用冒号分隔、各段格式随版本变化的内部格式，
 * 自己解析迟早会在某个版本上解析错，而错的姿势还是「看起来没权限」。compat 方法把这件事封在库里。
 *
 * 权限由用户在系统设置里授予，应用无法自行请求，因此这里没有「申请」这个动作，只有查询与刷新。
 */
internal class AndroidNotificationAccessSource(context: Context) : NotificationAccessSource {
    private val appContext = context.applicationContext

    private val mutableIsGranted = MutableStateFlow(isListenerEnabled())

    override val isGranted: StateFlow<Boolean> = mutableIsGranted.asStateFlow()

    override fun refresh() {
        mutableIsGranted.value = isListenerEnabled()
    }

    private fun isListenerEnabled(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(appContext).contains(appContext.packageName)
}
