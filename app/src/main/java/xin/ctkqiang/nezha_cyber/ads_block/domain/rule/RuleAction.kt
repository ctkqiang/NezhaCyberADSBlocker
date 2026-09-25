package xin.ctkqiang.nezha_cyber.ads_block.domain.rule

/**
 * 过滤动作。只有两种结果，且都是确定的。
 *
 * 刻意不提供「可能拦截」这类中间态：规则引擎的每个决策都必须能解释成「命中哪条规则」，
 * 含糊的动作会让界面无法如实说明原因（工程规则第 9、32 节）。
 */
enum class RuleAction {
    ALLOW,
    BLOCK,
}
