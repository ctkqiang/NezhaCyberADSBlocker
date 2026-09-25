package xin.ctkqiang.nezha_cyber.ads_block.data.rule

import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.KeywordBlockingPolicy
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.KeywordNormalizer

private const val ENABLED_LINE = "enabled"

private const val DISABLED_LINE = "disabled"

/**
 * 关键词策略文件的行格式。
 *
 * 第一行是开关（`enabled` 或 `disabled`），其余每行一个关键词。开关单独占一行而不是写在
 * 每个关键词前面：它只有一个，重复 N 遍既冗长，又制造出「同一文件里两个开关互相矛盾」的可能。
 *
 * [parse] 在首行缺失或无法识别时返回 null，由调用方退回默认策略。这一点很关键：
 * 「文件不存在」与「用户清空了全部关键词」必须区分开，只有前者才退回默认值。
 * 非法关键词行跳过而不中断解析（工程规则第 29、39.2 节）。
 */
internal object KeywordPolicyCodec {
    fun format(policy: KeywordBlockingPolicy): List<String> =
        listOf(if (policy.enabled) ENABLED_LINE else DISABLED_LINE) + policy.keywords.sorted()

    fun parse(lines: List<String>): KeywordBlockingPolicy? {
        val meaningful = lines.map { line -> line.trim() }.filter { line -> line.isNotEmpty() }
        val enabled = meaningful.firstOrNull()?.toEnabledFlag() ?: return null
        val keywords = meaningful.asSequence()
            .drop(1)
            .mapNotNull { line -> KeywordNormalizer.normalize(line) }
            .toSet()
        return KeywordBlockingPolicy(enabled = enabled, keywords = keywords)
    }

    private fun String.toEnabledFlag(): Boolean? = when (this) {
        ENABLED_LINE -> true
        DISABLED_LINE -> false
        else -> null
    }
}
