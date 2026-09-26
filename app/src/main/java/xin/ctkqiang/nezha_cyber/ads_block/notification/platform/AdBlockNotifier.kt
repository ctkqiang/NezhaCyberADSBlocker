package xin.ctkqiang.nezha_cyber.ads_block.notification.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import xin.ctkqiang.nezha_cyber.ads_block.R
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.InstalledApplicationSource
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.DomainObservation
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleSource

private const val CHANNEL_ID = "ad_blocks"

/** 汇总通知固定挂在同一个 tag/id 上：新的一次拦截覆盖上一次，通知栏里始终只有一条。 */
private const val NOTIFICATION_TAG = "ad-block"

private const val NOTIFICATION_ID = 1002

private const val REQUEST_OPEN = 3001

/**
 * 两次发布之间的最小间隔。
 *
 * 广告拦截发生在 DNS 热路径上，一次应用启动可能触发几十条。逐条推送等于刷屏，且每条都要走一次
 * 通知栏 binder 调用。合并到固定节奏后，用户看到的是「最近被拦了什么」，而不是一屏重复条目。
 */
private const val PUBLISH_INTERVAL_MILLIS = 1500L

/**
 * 拦截结果的汇总通知。
 *
 * 设计要点：
 * - **[record] 必须立刻返回**：它在中继线程上被每个「被拦下的」DNS 查询调用，只做两次无锁更新
 *   （计数与最新一条），落盘、查包名与发布通知全部留给 [runWhile] 所在的协程；
 * - **合并而不是堆积**：始终复用同一个通知 id，因此结果是「实时更新的一条」，
 *   既不会淹没通知栏，也不会因为高频拦截产生大量对象；
 * - **只报告事实**：通知里写的是「哪个应用请求了哪个域名、命中哪条规则」，
 *   不把域名断言成广告（工程规则第 32 节）。
 *
 * 这一档通知与「通知拦截」是两回事：前者拦的是网络请求，后者处理的是通知栏里的通知内容，
 * 二者共用同一个通知管理器，但频道、id 与文案都各自独立。
 */
internal class AdBlockNotifier(
    applicationContext: Context,
    private val applicationSource: InstalledApplicationSource,
) {
    private val context = applicationContext.applicationContext

    private val manager: NotificationManager? = context.getSystemService(NotificationManager::class.java)

    private val sessionBlocked = AtomicLong(0)

    private val latest = MutableStateFlow<DomainObservation?>(null)

    /** 记录一次被拦下的查询。在中继线程上调用，必须立刻返回。 */
    fun record(observation: DomainObservation) {
        sessionBlocked.incrementAndGet()
        latest.value = observation
    }

    /**
     * 按固定节奏把最新的拦截结果发布会出去，直到调用方取消这个协程。
     *
     * 只在内容真的变化时才发布：没有新的拦截就什么都不做，避免一条静止的通知被反复重发。
     */
    suspend fun runWhile(isActive: () -> Boolean) {
        var publishedObservation: DomainObservation? = null
        var publishedCount = 0L
        while (isActive()) {
            val observation = latest.value
            val count = sessionBlocked.get()
            if (observation != null && (observation !== publishedObservation || count != publishedCount)) {
                publish(observation = observation, count = count)
                publishedObservation = observation
                publishedCount = count
            }
            delay(PUBLISH_INTERVAL_MILLIS)
        }
    }

    /** 隧道停止时撤下汇总通知，并清空最新一条，使下一次会话从干净状态开始。 */
    fun cancel() {
        latest.value = null
        manager?.cancel(NOTIFICATION_TAG, NOTIFICATION_ID)
    }

    private suspend fun publish(observation: DomainObservation, count: Long) {
        val notificationManager = manager ?: return
        ensureChannel(notificationManager)
        val appLabel = applicationLabelOf(observation.packageName)
        notificationManager.notify(
            NOTIFICATION_TAG,
            NOTIFICATION_ID,
            buildNotification(observation = observation, appLabel = appLabel, count = count),
        )
    }

    private fun buildNotification(observation: DomainObservation, appLabel: String, count: Long): Notification {
        val body = bodyOf(observation = observation, appLabel = appLabel, count = count)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_shield)
            .setContentTitle(context.getString(R.string.ad_block_title))
            .setContentText(context.getString(R.string.ad_block_text, appLabel, observation.host))
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
        contentIntent()?.let(builder::setContentIntent)
        return builder.build()
    }

    private fun bodyOf(observation: DomainObservation, appLabel: String, count: Long): String = listOfNotNull(
        context.getString(R.string.ad_block_body_count, count),
        context.getString(R.string.ad_block_body_domain, observation.host),
        context.getString(R.string.ad_block_body_app, appLabel),
        observation.matchedRule?.let { matched ->
            context.getString(R.string.ad_block_body_rule, reasonOf(source = observation.source, matched = matched))
        },
    ).joinToString(separator = "\n")

    /**
     * 把来源翻译成界面用语。
     *
     * 与「网络活动」页共用同一批文案：同一件事在通知与列表里必须是同一个说法，
     * 否则用户会以为它们是两种不同的拦截。
     */
    private fun reasonOf(source: RuleSource?, matched: String): String = when (source) {
        RuleSource.APP_ADS -> context.getString(R.string.network_reason_app_ads)
        RuleSource.BUILTIN -> context.getString(R.string.network_reason_builtin)
        RuleSource.KEYWORD -> context.getString(R.string.network_reason_keyword, matched)
        RuleSource.USER -> context.getString(R.string.network_reason_user, matched)
        null -> context.getString(R.string.network_reason_none)
    }

    /**
     * 应用显示名。
     *
     * 走既有的应用发现端口，而不是自己再查一次包管理器：解析规则与「查不到时如何降级」
     * 只应有一处实现。归属不可用时如实显示「未知来源」，不猜一个应用出来（第 32 节）。
     */
    private suspend fun applicationLabelOf(packageName: String?): String {
        if (packageName == null) return context.getString(R.string.ad_block_app_unknown)
        return applicationSource.displayNames(setOf(packageName))[packageName] ?: packageName
    }

    private fun ensureChannel(notificationManager: NotificationManager) {
        if (notificationManager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.ad_block_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        channel.description = context.getString(R.string.ad_block_channel_description)
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * 点按通知时打开本应用。
     *
     * 用 `getLaunchIntentForPackage` 而不是直接引用入口类：平台层不该知道入口叫什么。
     * 取不到启动入口就不给内容意图——好过给一个点了没反应的意图。
     */
    private fun contentIntent(): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            REQUEST_OPEN,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
