package xin.ctkqiang.nezha_cyber.ads_block.network.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import xin.ctkqiang.nezha_cyber.ads_block.R

/**
 * VPN 前台服务的常驻通知。
 *
 * 隧道必须长期驻留且运行状态对用户可见，因此本服务以前台服务形式运行，这条通知就是它的可见载体。
 * 通知里带「停止」动作：用户不打开应用也能断开，这是隧道类应用的底线要求。
 * 内容区用自定义视图（nezha_notification_vpn.xml），与拦截通知共用同一套品牌语言。系统默认的
 * 「标题 + 一行文字」在各厂商 ROM 上各画各的，而这条通知长期挂在通知栏上，那种差异会被反复看到。
 */
internal object VpnNotification {
    private const val CHANNEL_ID = "vpn_session"
    private const val NOTIFICATION_ID = 1001

    /** 「停止」与「打开」两个 PendingIntent 的 requestCode：用途不同，取值必须分开，否则会互相覆盖。 */
    private const val REQUEST_STOP = 2001
    private const val REQUEST_OPEN = 2002

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.vpn_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    fun build(context: Context, contentIntent: PendingIntent?, stopIntent: PendingIntent?): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_shield)
            .setContentTitle(context.getString(R.string.vpn_notification_title))
            .setContentText(context.getString(R.string.vpn_notification_text))
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(contentView(context))
            .setOngoing(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
        contentIntent?.let(builder::setContentIntent)
        stopIntent?.let { intent ->
            builder.addAction(
                R.drawable.ic_notification_shield,
                context.getString(R.string.vpn_notification_action_stop),
                intent,
            )
        }
        return builder.build()
    }

    fun notificationId(): Int = NOTIFICATION_ID

    fun stopIntent(context: Context): PendingIntent {
        val intent = Intent(context, NezhaVpnService::class.java).setAction(NezhaVpnService.ACTION_STOP)
        return PendingIntent.getService(context, REQUEST_STOP, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    fun contentIntent(context: Context): PendingIntent? {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, REQUEST_OPEN, launchIntent, PendingIntent.FLAG_IMMUTABLE)
    }

    /**
     * 内容视图。
     *
     * 布局里不放任何文案，标题与正文都由这里从字符串资源取（工程规则第 42.4 节）。
     */
    private fun contentView(context: Context): RemoteViews =
        RemoteViews(context.packageName, R.layout.nezha_notification_vpn).apply {
            setTextViewText(R.id.notification_title, context.getString(R.string.vpn_notification_title))
            setTextViewText(R.id.notification_text, context.getString(R.string.vpn_notification_text))
        }
}
