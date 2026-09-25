package xin.ctkqiang.nezha_cyber.ads_block.domain.rule

/**
 * 添加规则的结果。
 *
 * 用封闭类型而不是抛异常或返回布尔：界面需要分别告诉用户「域名不合法」和「这条规则已经存在」，
 * 这两件事的处置方式完全不同（一个要改输入，一个什么都不用做）。
 */
sealed interface RuleEditResult {
    data object Applied : RuleEditResult

    /** 域名不是合法主机名。 */
    data object InvalidHost : RuleEditResult

    /** 同来源同动作下已有该域名。 */
    data object AlreadyExists : RuleEditResult

    /** 试图删除或修改一条不存在的规则。 */
    data object NotFound : RuleEditResult
}
