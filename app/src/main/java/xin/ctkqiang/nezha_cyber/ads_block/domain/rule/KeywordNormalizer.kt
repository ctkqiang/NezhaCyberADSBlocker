package xin.ctkqiang.nezha_cyber.ads_block.domain.rule

/** DNS 单个标签的长度上限。 */
private const val MAX_KEYWORD_LENGTH = 63

/** 关键词允许的字符：首字符必须是字母或数字，其后可含连字符与下划线。 */
private val KEYWORD_PATTERN = Regex("[a-z0-9][a-z0-9_-]*")

/**
 * 关键词归一化与合法性校验。
 *
 * 关键词是**单个 DNS 标签**，不是域名：`ads` 合法，`ads.example.com` 不合法。
 * 拒绝多标签不是形式要求，而是匹配语义决定的——[KeywordMatcher] 逐标签比对，
 * 收下一个带点的「关键词」只会让它永远匹配不上，却看起来像已经在生效。
 *
 * 与 [HostNormalizer] 分开的理由同理：一个是主机名规则（要求至少两段），
 * 一个是标签规则（要求恰好一段），共用一套校验会让两边的约束互相污染。
 */
object KeywordNormalizer {
    /** 归一化一个关键词；不是合法标签时返回 null。 */
    fun normalize(raw: String): String? = raw.trim().lowercase().takeIf { candidate -> isLegalKeyword(candidate) }

    private fun isLegalKeyword(candidate: String): Boolean =
        candidate.isNotEmpty() && candidate.length <= MAX_KEYWORD_LENGTH && KEYWORD_PATTERN.matches(candidate)
}
