package xin.ctkqiang.nezha_cyber.ads_block.notification.platform

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import xin.ctkqiang.nezha_cyber.ads_block.domain.notification.NotificationContent
import xin.ctkqiang.nezha_cyber.ads_block.domain.notification.NotificationFilterDecision
import xin.ctkqiang.nezha_cyber.ads_block.requireAppContainer

/**
 * 观察通知栏并在命中规则时改以「已拦截」的形式呈现。
 *
 * **这是「发布之后」的观察点，不是发布前的防火墙。** 系统在应用把通知交给通知栏之后才回调这里，
 * 因此本服务既无法阻止应用生成通知，也无法阻止它在生成通知时做的任何事——它只能改变这条通知
 * 在通知栏里的去向。界面上也照这个口径说明，不宣称能「阻止」通知（工程规则第 32 节）。
 *
 * 本服务只是一个适配器：读文本、问领域层、按结论处理。规则匹配逻辑一行都不在这里
 * （第 8 节的要求，也是为了让判定逻辑可以脱离 Android 单测）。
 *
 * `onNotificationPosted` 在主线程被调用，而处理过程要访问通知管理器与包管理器，
 * 因此立刻切到后台调度器，绝不阻塞通知栏。
 */
class NezhaNotificationListenerService : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val tracker: SelfNotificationTracker by lazy {
        SelfNotificationTracker(ownPackageName = packageName)
    }

    private val poster: BlockedNotificationPoster by lazy {
        BlockedNotificationPoster(
            context = this,
            tracker = tracker,
            // 复用容器里那一份应用发现实现：替代通知要显示原应用的名字，而这件事已经有端口在做。
            applicationSource = requireAppContainer(this).installedApplicationSource,
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val posted = sbn ?: return
        if (tracker.isSelfPosted(packageName = posted.packageName, tag = posted.tag, id = posted.id)) return
        val content = NotificationContentReader.read(posted)
        scope.launch { handlePosted(key = posted.key, id = posted.id, content = content) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /**
     * 判定并处置一条通知。
     *
     * 顺序是刻意的——**先发布替代通知，再取消原通知**：反过来的话，一旦发布失败，那条通知就凭空
     * 消失了，而用户不会收到任何解释。反过来失败最坏的结果只是通知栏里同时出现两条。
     *
     * 取消可能失败：前台服务通知、常驻通知等系统不允许第三方取消。此时原通知仍在，用户会同时看到
     * 原文与我们的标记——这是平台限制，不是漏处理，因此不做补救（没有可用的补救手段）。
     */
    private suspend fun handlePosted(key: String, id: Int, content: NotificationContent) {
        val engine = requireAppContainer(this).notificationRuleEngine.value
        val decision = engine.evaluate(content)
        if (decision !is NotificationFilterDecision.Block) return
        poster.publish(
            BlockedNotification(
                notificationKey = key,
                originalId = id,
                original = content,
                rule = decision.rule,
                field = decision.field,
            ),
        )
        // 按 key 取消。`cancelNotification(包名, tag, id)` 那个重载在 android-36 的
        // api-versions.xml 里标着 deprecated="21"，而 key 是系统给这条通知的权威标识
        // （StatusBarNotification.getKey，API 20 起可用且未废弃），比自己去拼更可靠。
        cancelNotification(key)
    }
}
