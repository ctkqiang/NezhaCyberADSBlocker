package xin.ctkqiang.nezha_cyber.ads_block.domain.notification

/**
 * 一条通知拦截规则。
 *
 * [packageName] 是这条规则**唯一**的作用范围。规则永远不会跨应用生效：匹配前先按包名取候选，
 * 因此「给淘宝加的规则」不可能拦到微信的通知，这不是靠约定，而是由数据结构本身保证。
 *
 * [matchText] 一律是「包含」匹配，且已经去掉首尾空白。空字符串没有意义（它会匹配任何有文本的
 * 通知），因此持久层拒绝写入，读取时也跳过——见 [NotificationRuleEditResult.EmptyMatchText]。
 *
 * [enabled] 为 false 的规则在构建索引时就被滤掉，而不是留到匹配阶段再判断：这样「关掉规则」
 * 不仅语义上不生效，连每次通知的比较开销也一起省掉了。
 */
data class NotificationRule(
    val id: NotificationRuleId,
    val packageName: String,
    val matchText: String,
    val enabled: Boolean,
)
