package xin.ctkqiang.nezha_cyber.ads_block.domain.rule

/** 通配符前缀。规则里只允许这一种通配写法（工程规则第 39.2 节）。 */
const val WILDCARD_PREFIX = "*."

private const val LABEL_SEPARATOR = '.'

private const val MAX_LABEL_LENGTH = 63

private const val MAX_HOST_LENGTH = 253

/** 标签允许的字符：字母、数字、连字符、下划线。下划线在真实 DNS 里是合法的（例如 _dmarc）。 */
private val LABEL_PATTERN = Regex("[a-z0-9_-]+")

/**
 * 域名归一化与合法性校验。
 *
 * 输入来自三个都不可信的地方：内置清单文件、用户输入、网络报文里的 QNAME。
 * 一旦这里放过非法值，下游就会拿它去建索引、去写规则文件、去和查询比对，错误会被放大。
 *
 * 规则：转小写、去掉一个结尾点、拒绝单标签（`com` 这种规则会拦掉整个后缀）、
 * 拒绝空标签、拒绝超长、拒绝协议/路径/端口等非域名内容。
 */
object HostNormalizer {
    /** 归一化一个普通主机名；不是合法域名时返回 null。 */
    fun normalizeHost(raw: String): String? =
        raw.trim().lowercase().removeSuffix(LABEL_SEPARATOR.toString()).takeIf { candidate -> isLegalHost(candidate) }

    private fun isLegalHost(candidate: String): Boolean {
        if (candidate.isEmpty() || candidate.length > MAX_HOST_LENGTH) return false
        val labels = candidate.split(LABEL_SEPARATOR)
        if (labels.size < MIN_LABEL_COUNT) return false
        return labels.all { label -> isValidLabel(label) }
    }

    /**
     * 归一化一条规则文本，返回去掉通配前缀后的主机名与是否为通配规则。
     *
     * 通配符只接受 `*.` 前缀；`ad*.example.com`、`*.example.*` 这类写法一律判为非法，
     * 因为它们的匹配语义无法用后缀索引表达，收下只会让行为不可预测。
     */
    fun normalizeRule(raw: String): NormalizedRule? = normalizeWildcard(raw.trim().lowercase())

    /**
     * 通配前缀只在这里被剥掉，调用方不必关心它的存在。
     * `removePrefix` 在前缀不存在时原样返回，因此不需要额外的分支。
     */
    private fun normalizeWildcard(trimmed: String): NormalizedRule? =
        normalizeHost(trimmed.removePrefix(WILDCARD_PREFIX))?.let { host ->
            NormalizedRule(host = host, isWildcard = trimmed.startsWith(WILDCARD_PREFIX))
        }

    private fun isValidLabel(label: String): Boolean =
        label.isNotEmpty() && label.length <= MAX_LABEL_LENGTH && LABEL_PATTERN.matches(label)

    private const val MIN_LABEL_COUNT = 2
}
