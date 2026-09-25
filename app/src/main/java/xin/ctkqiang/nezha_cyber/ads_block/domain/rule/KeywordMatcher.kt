package xin.ctkqiang.nezha_cyber.ads_block.domain.rule

private const val LABEL_SEPARATOR = '.'

private const val KEYWORD_SUFFIX_SEPARATOR = '-'

private const val KEYWORD_SUFFIX_UNDERSCORE = "_"

/**
 * 关键词匹配。
 *
 * 一个标签命中关键词的条件（三者之一）：
 * - 与关键词完全相同：`ads.example.com` 里的 `ads`；
 * - 以「关键词 + `-`」开头：`ads-banner.example.com`；
 * - 以「关键词 + `_`」开头：`ads_banner.example.com`。
 *
 * 刻意不做子串匹配，也不做无分隔符前缀匹配：后者会把 `adsapi.example.com`（可能是自有接口）
 * 一并拦掉，而按分隔符切分能让「这是独立的广告标签」这个判断站得住。
 *
 * 复杂度：标签数 × 关键词数。关键词只有几个，且它位于优先级最后一档，
 * 只有前面三档全部未命中时才会执行，因此不需要为它建索引。
 */
internal class KeywordMatcher(policy: KeywordBlockingPolicy) {
    private val enabled = policy.enabled

    private val keywords = policy.keywords.map { keyword -> keyword.lowercase() }

    /** 命中则返回命中的关键词，未命中返回 null。 */
    fun match(host: String): String? = if (enabled) {
        host.split(LABEL_SEPARATOR).firstNotNullOfOrNull { label -> matchLabel(label) }
    } else {
        null
    }

    private fun matchLabel(label: String): String? = keywords.firstOrNull { keyword -> label.matchesKeyword(keyword) }

    private fun String.matchesKeyword(keyword: String): Boolean = this == keyword ||
        startsWith(keyword + KEYWORD_SUFFIX_SEPARATOR) ||
        startsWith(keyword + KEYWORD_SUFFIX_UNDERSCORE)
}
