package xin.ctkqiang.nezha_cyber.ads_block.domain.notification

/**
 * 一次规则编辑的结果。
 *
 * 用封闭类型而不是布尔或异常：这四种结果都是**预期内的领域结果**，不是缺陷。
 * 布尔值会让「匹配文字为空」与「规则已不存在」在界面里显示成同一句话，
 * 而两者的处置完全不同——前者要提示用户补内容，后者说明这条规则已经被别处删掉了。
 */
sealed interface NotificationRuleEditResult {
    data object Applied : NotificationRuleEditResult

    /** 目标规则不存在（已被删除，或界面持有的是过期列表）。 */
    data object NotFound : NotificationRuleEditResult

    /** 匹配文字去掉首尾空白后为空。空串会被任何非空文本包含，等于拦下该应用的全部通知。 */
    data object EmptyMatchText : NotificationRuleEditResult

    /** 同一个应用下已存在一条匹配文字相同的规则。重复的规则在列表里无法区分，只会让用户困惑。 */
    data object Duplicate : NotificationRuleEditResult
}
