package xin.ctkqiang.nezha_cyber.ads_block.notification.platform

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import xin.ctkqiang.nezha_cyber.ads_block.domain.notification.NotificationContent

/**
 * 把系统通知里的文本读成领域模型。
 *
 * 这是 Android 与领域层之间的适配器：`StatusBarNotification`、`Bundle`、`CharSequence` 都止步于
 * 这个文件，往外只交出 [NotificationContent]（工程规则第 38.3 节）。
 *
 * **不预设字段一定存在。** 不同应用往 extras 里放的东西差别很大，缺字段是常态；读不到就是 null，
 * 由领域层按「该字段没有内容」处理，不在这里编造占位文字。
 *
 * 只读文本，不读 `RemoteViews` 之类需要跨进程展开的内容：那既昂贵（会拖慢通知栏），
 * 也可能把用户的通知布局整个拉进本进程。
 */
internal object NotificationContentReader {
    fun read(statusBarNotification: StatusBarNotification): NotificationContent {
        val extras = statusBarNotification.notification.extras
        return NotificationContent(
            packageName = statusBarNotification.packageName,
            title = extras.readText(Notification.EXTRA_TITLE),
            text = extras.readText(Notification.EXTRA_TEXT),
            bigText = extras.readText(Notification.EXTRA_BIG_TEXT),
            subText = extras.readText(Notification.EXTRA_SUB_TEXT),
            textLines = extras.readTextLines(Notification.EXTRA_TEXT_LINES),
        )
    }

    /**
     * 读取单个文本字段。
     *
     * 空白串按「没有内容」处理：它包含不了任何非空的匹配文字，留着只会让下一个人以为这个字段有值。
     * 用 `CharSequence.toString()` 而不是 `getString`：通知里的文本常常是 `SpannableString`
     * （带样式的富文本），`getString` 在类型不匹配时会抛异常。
     */
    private fun Bundle.readText(key: String): String? =
        getCharSequence(key)?.toString()?.takeIf { value -> value.isNotBlank() }

    private fun Bundle.readTextLines(key: String): List<String> = getCharSequenceArray(key)
        ?.mapNotNull { line -> line?.toString()?.takeIf { value -> value.isNotBlank() } }
        .orEmpty()
}
