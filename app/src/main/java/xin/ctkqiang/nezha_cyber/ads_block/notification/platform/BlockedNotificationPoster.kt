package xin.ctkqiang.nezha_cyber.ads_block.notification.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import xin.ctkqiang.nezha_cyber.ads_block.R
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.InstalledApplicationSource
import xin.ctkqiang.nezha_cyber.ads_block.domain.notification.NotificationContent

private const val CHANNEL_ID = "blocked_notifications"

/** 替代通知统一挂在同一个 tag 下，靠 id 区分：通知栏里它们属于同一组。 */
private const val NOTIFICATION_TAG = "blocked"

/** 原通知正文在替代通知里的最大长度。展示用，截断不会丢判定信息。 */
private const val MAX_ORIGINAL_TEXT_LENGTH = 500

/**
 * 发布「已被拦截」的替代通知。
 *
 * **Android 没有任何 API 可以改写另一个应用已发布的通知。** 能做的只有取消它，再以本应用的名义
 * 发一条新的。因此这里的做法是：保留原通知里能读到的内容，加上拦截标记，以本应用的名义发出去，
 * 然后由调用方取消原通知。
 *
 * 顺序是刻意的——**先发替代通知，再取消原通知**：反过来的话，一旦发布失败，那条通知就凭空消失了，
 * 而用户不会收到任何解释，正是「静默删除」。反过来失败最坏的结果只是通知栏里同时出现两条。
 *
 * 替代通知挂在低重要度频道上：原通知发布时已经提醒过一次，替代通知再响一次等于对同一条通知
 * 打扰两遍。低重要度不发声、不弹横幅，但通知栏里看得见，符合「标记」而不是「再轰炸一次」。
 */
internal class BlockedNotificationPoster(
    private val context: Context,
    private val tracker: SelfNotificationTracker,
    private val applicationSource: InstalledApplicationSource,
) {
    private val manager: NotificationManager? = context.applicationContext
        .getSystemService(NotificationManager::class.java)

    suspend fun publish(blocked: BlockedNotification) {
        val notificationManager = manager ?: return
        val notificationId = notificationIdOf(blocked)
        ensureChannel(notificationManager)
        val appLabel = applicationLabelOf(blocked.original.packageName)
        // 先登记再发布：发布与 onNotificationPosted 回调之间没有先后保证，先登记才不会漏掉自己。
        tracker.rememberPosted(tag = NOTIFICATION_TAG, id = notificationId)
        notificationManager.notify(NOTIFICATION_TAG, notificationId, build(blocked = blocked, appLabel = appLabel))
    }

    private fun build(blocked: BlockedNotification, appLabel: String): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_shield)
            .setContentTitle(context.getString(R.string.notification_blocked_title))
            .setContentText(context.getString(R.string.notification_blocked_original_app, appLabel))
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyOf(blocked = blocked, appLabel = appLabel)))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
        launchIntent()?.let { intent -> builder.setContentIntent(intent) }
        return builder.build()
    }

    private fun bodyOf(blocked: BlockedNotification, appLabel: String): String = listOf(
        context.getString(R.string.notification_blocked_original_app, appLabel),
        context.getString(R.string.notification_blocked_matched_rule, blocked.rule.matchText),
        context.getString(R.string.notification_blocked_original_text, originalTextOf(blocked.original)),
    ).joinToString(separator = "\n")

    /**
     * 把原通知里读到的文本拼起来。
     *
     * 去重是必要的：消息类通知常常把同一句话同时放在 `text` 与 `textLines` 里，不去重就会显示两遍。
     *
     * 一个字段都没有时给出一句明确的「没有可读取的文本」，而不是编一句占位内容——用户需要知道的
     * 是「这条通知确实没读出内容」，而不是看到一句我们造出来的话（工程规则第 32 节）。
     */
    private fun originalTextOf(content: NotificationContent): String {
        val parts = listOfNotNull(content.title, content.text, content.bigText, content.subText) + content.textLines
        return parts.distinct()
            .joinToString(separator = " ")
            .take(MAX_ORIGINAL_TEXT_LENGTH)
            .takeIf { text -> text.isNotBlank() }
            ?: context.getString(R.string.notification_blocked_original_text_missing)
    }

    /**
     * 原应用的显示名。
     *
     * 走**既有的应用发现端口**，而不是自己再查一次包管理器：标签解析与「查不到时如何降级」
     * 这两件事只应该有一处实现，否则通知里显示的应用名与界面里显示的迟早会不一致。
     * 端口自身负责调度与异常处理，这里只负责在解析不到时退回包名——包名仍然能定位到它，
     * 而「未知应用」会把两条不同来源的通知说成同一个（工程规则第 32 节）。
     */
    private suspend fun applicationLabelOf(packageName: String): String =
        applicationSource.displayNames(setOf(packageName))[packageName] ?: packageName

    /**
     * 替代通知的 id。
     *
     * 不直接复用原通知的 id：原 id 在应用之间各自独立（大量应用都用 0），直接复用会让不同应用
     * 被拦的通知互相覆盖。用「包名 + 原 id」的稳定散列，不同应用得到不同 id，而同一条原通知
     * 再次被拦时仍然落在同一个 id 上（因此是覆盖，不是堆积）。
     *
     * 散列存在理论碰撞，碰撞的后果只是两条被拦通知合并成一条，不影响判定本身。
     */
    private fun notificationIdOf(blocked: BlockedNotification): Int =
        "${blocked.original.packageName}#${blocked.originalId}".hashCode()

    private fun ensureChannel(notificationManager: NotificationManager) {
        if (notificationManager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_blocked_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        channel.description = context.getString(R.string.notification_blocked_channel_description)
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * 点按替代通知时打开本应用。
     *
     * 用 `getLaunchIntentForPackage` 而不是直接引用界面入口类：平台层不该知道入口叫什么，
     * 换入口时这里不需要跟着改。取不到启动入口就不给内容意图——好过给一个点了没反应的意图。
     */
    private fun launchIntent(): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
