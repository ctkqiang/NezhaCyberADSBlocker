package xin.ctkqiang.nezha_cyber.ads_block.domain.notification

/**
 * 通知里可读到的字段。
 *
 * 用于向用户解释命中的位置（「匹配到的是标题还是正文」），本身不参与判定逻辑。
 */
enum class NotificationTextField {
    Title,
    Text,
    BigText,
    SubText,

    /** 消息类通知的逐条正文。多数聊天应用把内容放在这里而不是 `text`。 */
    TextLines,
}
