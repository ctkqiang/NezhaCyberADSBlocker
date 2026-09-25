package xin.ctkqiang.nezha_cyber.ads_block.domain.notification

/**
 * 一条通知的判定结果。
 *
 * [Block] 带上整条规则与命中的字段，界面据此向用户解释「为什么这条被拦」——
 * 只回一个 BLOCK 而不说清依据，用户无法判断是规则写错了还是真被拦对了（第 32 节）。
 * 命中的文字就是 `rule.matchText`，不再单开一个字段重复它。
 *
 * 没有「未命中」这一态：与域名过滤不同，通知在这里只有两种去向——原样放行，或者被标记为拦截。
 */
sealed interface NotificationFilterDecision {
    data object Allow : NotificationFilterDecision

    data class Block(val rule: NotificationRule, val field: NotificationTextField) : NotificationFilterDecision
}
