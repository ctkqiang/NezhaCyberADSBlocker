package xin.ctkqiang.nezha_cyber.ads_block.domain.rule

/**
 * 规则引擎实现：分档后缀索引 + 关键词兜底。
 *
 * 数据结构的取舍（工程规则第 22 节要求对规则规模做出选择）：单次查询只做「按标签数」
 * 次哈希查找，与规则条数无关。每一档都由一个 [HostSuffixIndex] 承载，查询时按优先级依次询问，
 * 第一次命中就是结论，因此「最具体的那条规则胜出」是结构保证的，而不是靠排序约定。
 *
 * 复杂度：O(标签数)，不随规则条数增长；10 万条规则与 10 条规则的查询开销相同。
 *
 * 优先级（第 10 节）由 [indexes] 的顺序与兜底顺序共同表达：
 * 用户显式放行 → 用户显式阻断 → 内置清单 → 应用专属清单 → 关键词兜底。
 * 关键词放最后是刻意的：用户显式放行必须能压过启发式，否则「我明明放行了却还被拦」
 * 就成了无法解释的行为。应用专属清单排在全局内置清单之后，是因为它能匹配的域名范围更窄，
 * 让更具体的全局规则先表态更符合直觉。
 *
 * 内置清单与应用专属清单都只提供阻断，因此没有「内置放行」这一档——
 * 留一个永远为空的档位只会误导读者。
 */
class DomainRuleEngine(
    rules: List<DomainRule>,
    keywordPolicy: KeywordBlockingPolicy,
    appAdRules: List<AppAdRule> = emptyList(),
) : RuleEngine {
    private val indexes = listOf(
        RuleIndex(source = RuleSource.USER, action = RuleAction.ALLOW, rules = rules),
        RuleIndex(source = RuleSource.USER, action = RuleAction.BLOCK, rules = rules),
        RuleIndex(source = RuleSource.BUILTIN, action = RuleAction.BLOCK, rules = rules),
    )

    private val appAdIndex = AppAdRuleIndex(appAdRules)

    private val keywordMatcher = KeywordMatcher(keywordPolicy)

    override fun evaluate(host: String, packageName: String?): FilterDecision {
        val normalized = HostNormalizer.normalizeHost(host) ?: return FilterDecision.Unknown
        return indexes.firstNotNullOfOrNull { index -> index.match(normalized) }
            ?: appAdDecision(normalized, packageName)
            ?: keywordDecision(normalized)
            ?: FilterDecision.Unknown
    }

    private fun appAdDecision(host: String, packageName: String?): FilterDecision? =
        appAdIndex.match(host, packageName)?.let { matched ->
            FilterDecision(action = RuleAction.BLOCK, matchedRule = matched, source = RuleSource.APP_ADS)
        }

    private fun keywordDecision(host: String): FilterDecision? = keywordMatcher.match(host)?.let { keyword ->
        FilterDecision(action = RuleAction.BLOCK, matchedRule = keyword, source = RuleSource.KEYWORD)
    }
}

/** 某个「来源 + 动作」组合下的规则索引。 */
private class RuleIndex(val source: RuleSource, val action: RuleAction, rules: List<DomainRule>) {
    private val index = HostSuffixIndex(
        rules.asSequence()
            .filter { rule -> rule.enabled && rule.source == source && rule.action == action }
            .map { rule -> rule.host }
            .toList(),
    )

    /** 命中则返回决策，未命中返回 null。 */
    fun match(host: String): FilterDecision? = index.match(host)?.let { matched ->
        FilterDecision(action = action, matchedRule = matched, source = source)
    }
}
