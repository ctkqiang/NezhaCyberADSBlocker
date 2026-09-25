package xin.ctkqiang.nezha_cyber.ads_block.domain.notification

/**
 * 一条通知里可供匹配的文本。
 *
 * 只携带文本，不带任何 Android 类型：`StatusBarNotification`、`Notification`、`Bundle` 都留在
 * 平台层的适配器里（工程规则第 38.3 节）。
 *
 * 每个字段都可能为 null：不同应用往 extras 里放的东西不一样，缺字段是**常态而不是异常**，
 * 因此这里没有「至少一个字段非空」这类约束——一条什么都没有的通知应当被正常放行，而不是报错。
 *
 * [textLines] 在规格要求的最低四个字段之外。加它是因为消息类通知（聊天、短信）常把正文放在
 * `Notification.EXTRA_TEXT_LINES` 里而 `text` 为空，漏掉它会让整类通知静默地不参与匹配。
 */
data class NotificationContent(
    val packageName: String,
    val title: String? = null,
    val text: String? = null,
    val bigText: String? = null,
    val subText: String? = null,
    val textLines: List<String> = emptyList(),
)
