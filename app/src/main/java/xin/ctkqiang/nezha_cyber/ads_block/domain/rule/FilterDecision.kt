package xin.ctkqiang.nezha_cyber.ads_block.domain.rule

/**
 * 一次过滤决策。
 *
 * 携带足够元数据供日志与统计使用（工程规则第 9 节）：界面要能回答「谁拦的、凭什么拦的」，
 * 只返回一个布尔值就做不到这一点。
 *
 * [matchedRule] 与 [source] 在未命中任何规则时为 null，此时动作为 ALLOW 但语义是「未知」，
 * 界面必须区分「用户放行」与「没有规则命中」——把后者说成「已放行」是在编造结论（第 32 节）。
 *
 * 当 [source] 是 [RuleSource.KEYWORD] 时，[matchedRule] 是命中的关键词（单个标签，
 * 例如 `ads`），而不是完整域名。界面在解释原因时必须按来源分开措辞，不能把它当成一条域名规则。
 *
 * 当 [source] 是 [RuleSource.APP_ADS] 时，[matchedRule] 是被命中的域名规则原文（通配规则带
 * `*.` 前缀）；它只对目标应用成立的这一层作用域信息不在这里，而是由发起查询的包名决定。
 */
data class FilterDecision(val action: RuleAction, val matchedRule: String?, val source: RuleSource?) {
    companion object {
        /** 没有任何规则命中。 */
        val Unknown = FilterDecision(action = RuleAction.ALLOW, matchedRule = null, source = null)
    }
}
