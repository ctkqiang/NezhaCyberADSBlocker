package xin.ctkqiang.nezha_cyber.ads_block.domain.notification

/**
 * 通知规则引擎。
 *
 * 只做一件事：给一条通知的文本，回答「放行还是拦截，依据是哪条规则」。
 * 它不知道通知从哪里来、也不知道拦截之后怎么呈现——那是平台层的事。
 *
 * **规则按包名建索引**，匹配时只取该应用的候选规则。因此：
 * - 一条规则不可能跨应用生效（索引以包名为键，这是结构性保证而不是约定）；
 * - 规则再多，单条通知的比较次数也只取决于**该应用**的规则数，而不是全库规则数（第 22 节）。
 *
 * **已禁用的规则在建索引时就被滤掉**，而不是留到匹配阶段再判——「关掉规则」不仅语义上不生效，
 * 每次通知的比较开销也一并省掉。
 *
 * **多规则命中时取第一条**，顺序由构建时传入的列表顺序决定；持久层按 id 升序返回，
 * 因此同一份规则集每次得到的结论都一样。这个确定性是刻意的：同一段文字今天拦、明天放，
 * 用户只会认为功能坏了。
 *
 * 构造器私有，只能经 [from] 创建：索引必须在构造时建好，否则「按包名取候选」这条保证就漏了。
 */
class NotificationRuleEngine private constructor(private val rulesByPackage: Map<String, List<NotificationRule>>) {
    /**
     * 判定一条通知。
     *
     * 字段的检查顺序固定为标题 → 正文 → 长正文 → 副标题 → 逐条正文。它决定「命中位置」在
     * 同一条规则同时命中多个字段时报哪一个，仅用于解释，不影响放行/拦截的结论。
     */
    fun evaluate(content: NotificationContent): NotificationFilterDecision {
        val candidates = rulesByPackage[content.packageName] ?: return NotificationFilterDecision.Allow
        for (rule in candidates) {
            val field = content.fieldContaining(rule.matchText) ?: continue
            return NotificationFilterDecision.Block(rule = rule, field = field)
        }
        return NotificationFilterDecision.Allow
    }

    private fun NotificationContent.fieldContaining(matchText: String): NotificationTextField? = when {
        title.containsText(matchText) -> NotificationTextField.Title
        text.containsText(matchText) -> NotificationTextField.Text
        bigText.containsText(matchText) -> NotificationTextField.BigText
        subText.containsText(matchText) -> NotificationTextField.SubText
        textLines.any { line -> line.containsText(matchText) } -> NotificationTextField.TextLines
        else -> null
    }

    /**
     * 包含匹配，大小写不敏感。
     *
     * 中文没有大小写之分，这个开关只对拉丁字母生效；[String.contains] 的大小写不敏感重载
     * 不依赖当前区域设置，因此同一份规则在不同语言的设备上结论一致。
     *
     * 空匹配文字直接判为不命中：空串被任何非空文本包含，放它进来等于让一条规则拦下该应用的
     * 全部通知。持久层会拒绝写入空匹配文字，这里再挡一次是因为引擎可能被喂进未经持久层的规则。
     */
    private fun String?.containsText(matchText: String): Boolean =
        matchText.isNotEmpty() && this != null && contains(matchText, ignoreCase = true)

    companion object {
        /** 没有任何规则时的引擎。所有通知一律放行。 */
        val Empty = NotificationRuleEngine(emptyMap())

        fun from(rules: List<NotificationRule>): NotificationRuleEngine = NotificationRuleEngine(
            rules.filter { rule -> rule.enabled }
                .groupBy { rule -> rule.packageName },
        )
    }
}
