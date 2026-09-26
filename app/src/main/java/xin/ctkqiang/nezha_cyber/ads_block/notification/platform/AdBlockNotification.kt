package xin.ctkqiang.nezha_cyber.ads_block.notification.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import xin.ctkqiang.nezha_cyber.ads_block.R
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.DomainObservation
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleSource
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.darkNezhaPalette
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.lightNezhaPalette

private const val CHANNEL_ID = "ad_blocks"

private const val REQUEST_OPEN = 3001

/**
 * 单条拦截通知的构造。
 *
 * 与 `VpnNotification` 同样的分工：这里只管「一条通知长什么样」，发布、排队与淘汰由
 * `AdBlockNotifier` 负责。分开的理由是两者变化的原因不同——文案会改，发布策略也会改。
 *
 * **频道保持低重要度、不发提示音**，这是刻意的：一次页面加载可能拦下十几条追踪器，
 * 逐条发声会让这个应用变成噪音源。通知在这里是**可查阅的记录**，不是闹钟。
 *
 * 顺带一条平台约束：频道一旦创建，重要度只能由用户在系统设置里改，**改代码不会影响已安装的
 * 设备**。保持同一个频道 id 正是为了不把用户已有的选择重置掉；若哪天确实要换个重要度，
 * 必须换一个新 id，那等于让所有老用户重新做一次选择。
 */
internal object AdBlockNotification {
    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.ad_block_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        channel.description = context.getString(R.string.ad_block_channel_description)
        manager.createNotificationChannel(channel)
    }

    fun build(context: Context, observation: DomainObservation, appLabel: String): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_shield)
            .setContentTitle(context.getString(R.string.ad_block_title))
            .setContentText(context.getString(R.string.ad_block_text, appLabel, observation.host))
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(contentView(context, observation, appLabel))
            .setCustomBigContentView(contentView(context, observation, appLabel))
            .setColor(brandColor(context))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
        contentIntent(context)?.let(builder::setContentIntent)
        return builder.build()
    }

    /**
     * 自定义内容视图。
     *
     * 用 [NotificationCompat.DecoratedCustomViewStyle] 而不是完全自绘：它保留系统的小图标、
     * 应用名、时间戳与折叠行为，只把内容区换成我们的布局。完全自绘会在不同厂商的系统上
     * 与系统控件风格脱节，也会丢掉展开/折叠这件用户已经形成习惯的事。
     */
    private fun contentView(context: Context, observation: DomainObservation, appLabel: String): RemoteViews =
        RemoteViews(context.packageName, R.layout.nezha_notification_ad_block).apply {
            setImageViewResource(R.id.notification_icon, R.drawable.ic_notification_shield)
            setTextViewText(R.id.notification_title, context.getString(R.string.ad_block_title))
            setTextViewText(R.id.notification_headline, observation.host)
            setTextViewText(R.id.notification_detail, detailOf(context, observation, appLabel))
        }

    /**
     * 品牌强调色。
     *
     * 取的是**系统**明暗下的品牌红，不是应用主题偏好下的：通知的背景由系统按系统明暗绘制，
     * 用应用偏好去选色会在「应用锁深色、系统浅色」时把深色主题的亮红压在浅色通知上。
     */
    private fun brandColor(context: Context): Int {
        val isSystemDark = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
        return if (isSystemDark) darkNezhaPalette.brand.toArgb() else lightNezhaPalette.brand.toArgb()
    }

    /**
     * 展开后的明细。
     *
     * 不再重复域名：域名已经是大字标题，明细里再来一行「域名：……」是同一件事说两遍。
     */
    private fun detailOf(context: Context, observation: DomainObservation, appLabel: String): String = listOfNotNull(
        context.getString(R.string.ad_block_body_app, appLabel),
        observation.matchedRule?.let { matched ->
            context.getString(R.string.ad_block_body_rule, reasonOf(context, observation.source, matched))
        },
    ).joinToString(separator = "\n")

    /**
     * 把来源翻译成界面用语。
     *
     * 与「网络活动」页共用同一批文案：同一件事在通知与列表里必须是同一个说法，
     * 否则用户会以为它们是两种不同的拦截。
     */
    private fun reasonOf(context: Context, source: RuleSource?, matched: String): String = when (source) {
        RuleSource.APP_ADS -> context.getString(R.string.network_reason_app_ads)
        RuleSource.BUILTIN -> context.getString(R.string.network_reason_builtin)
        RuleSource.KEYWORD -> context.getString(R.string.network_reason_keyword, matched)
        RuleSource.USER -> context.getString(R.string.network_reason_user, matched)
        null -> context.getString(R.string.network_reason_none)
    }

    /**
     * 点按通知时打开本应用。
     *
     * 用 `getLaunchIntentForPackage` 而不是直接引用入口类：平台层不该知道入口叫什么。
     * 取不到启动入口就不给内容意图——好过给一个点了没反应的意图。
     *
     * 每条通知都用同一个 requestCode：它们指向同一个入口，系统只保留一个 PendingIntent，
     * 多给几个 code 只是白占位。
     */
    private fun contentIntent(context: Context): PendingIntent? {
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
