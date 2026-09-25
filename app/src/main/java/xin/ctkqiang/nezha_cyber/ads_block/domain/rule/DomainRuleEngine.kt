package xin.ctkqiang.nezha_cyber.ads_block.domain.rule

/**
 * 规则引擎实现：后缀索引查找 + 关键词兜底。
 *
 * 数据结构的取舍（工程规则第 22 节要求对规则规模做出选择）：单次查询只做「按标签数」
 * 次哈希查找，与规则条数无关。实现方式是为每个优先级各建一个精确集合与一个通配父域集合，
 * 查询时从最长的后缀逐级剥离、命中即返回，因此第一次命中天然就是最具体的那条规则。
 *
 * 复杂度：O(标签数)，不随规则条数增长；10 万条规则与 10 条规则的查询开销相同。
 *
 * 优先级（第 10 节）由 [indexes] 的顺序表达：用户显式放行 → 用户显式阻断 → 内置清单，
 * 三者都没命中时才交给关键词兜底。兜底放在最后是刻意的：用户显式放行必须能压过启发式，
 * 否则「我明明放行了却还被拦」就成了无法解释的行为。
 *
 * 内置清单只提供阻断，因此没有「内置放行」这一档——留一个永远为空的档位只会误导读者。
 */
class DomainRuleEngine(rules: List<DomainRule>, keywordPolicy: KeywordBlockingPolicy) : RuleEngine {
    private val indexes = listOf(
        RuleIndex(source = RuleSource.USER, action = RuleAction.ALLOW, rules = rules),
        RuleIndex(source = RuleSource.USER, action = RuleAction.BLOCK, rules = rules),
        RuleIndex(source = RuleSource.BUILTIN, action = RuleAction.BLOCK, rules = rules),
    )

    private val keywordMatcher = KeywordMatcher(keywordPolicy)

    override fun evaluate(host: String): FilterDecision {
        val normalized = HostNormalizer.normalizeHost(host) ?: return FilterDecision.Unknown
        return indexes.firstNotNullOfOrNull { index -> index.match(normalized) }
            ?: keywordDecision(normalized)
            ?: FilterDecision.Unknown
    }

    private fun keywordDecision(host: String): FilterDecision? = keywordMatcher.match(host)?.let { keyword ->
        FilterDecision(action = RuleAction.BLOCK, matchedRule = keyword, source = RuleSource.KEYWORD)
    }
}

/** 某个「来源 + 动作」组合下的规则索引。 */
private class RuleIndex(val source: RuleSource, val action: RuleAction, rules: List<DomainRule>) {
    private val exactHosts: Set<String>

    private val wildcardParents: Set<String>

    init {
        val applicable = rules.filter { rule ->
            rule.enabled && rule.source == source && rule.action == action
        }
        exactHosts = applicable.asSequence()
            .map { rule -> rule.host }
            .filterNot { host -> host.startsWith(WILDCARD_PREFIX) }
            .toHashSet()
        wildcardParents = applicable.asSequence()
            .map { rule -> rule.host }
            .filter { host -> host.startsWith(WILDCARD_PREFIX) }
            .map { host -> host.removePrefix(WILDCARD_PREFIX) }
            .toHashSet()
    }

    /**
     * 命中则返回决策，未命中返回 null。
     *
     * 通配规则只匹配**严格子域**：`*.ads.example.com` 命中 `a.ads.example.com`，
     * 但不命中 `ads.example.com` 本身。实现上靠 `isHostItself` 区分——在后缀走到与规则父域
     * 完全相同的那一步时跳过通配判定。少了这个区分就会连裸域一起拦掉，
     * 而裸域往往正是该服务的正式入口，误拦的代价远大于漏拦一条广告子域。
     */
    fun match(host: String): FilterDecision? {
        var suffix = host
        var isHostItself = true
        while (true) {
            val matchedRule = when {
                suffix in exactHosts -> suffix
                !isHostItself && suffix in wildcardParents -> "$WILDCARD_PREFIX$suffix"
                else -> null
            }
            if (matchedRule != null) {
                return FilterDecision(action = action, matchedRule = matchedRule, source = source)
            }
            val separator = suffix.indexOf('.')
            if (separator < 0) return null
            suffix = suffix.substring(separator + 1)
            isHostItself = false
        }
    }
}
