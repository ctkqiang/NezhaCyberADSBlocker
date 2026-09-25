package xin.ctkqiang.nezha_cyber.ads_block.data.rule

import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.DomainRule
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.HostNormalizer
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleAction
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleSource
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.WILDCARD_PREFIX

private const val COLUMN_COUNT = 4

private const val ACTION_COLUMN = 0

private const val SOURCE_COLUMN = 1

private const val ENABLED_COLUMN = 2

private const val HOST_COLUMN = 3

private const val SEPARATOR = '\t'

/**
 * 规则文件的行格式。
 *
 * 列：动作、来源、是否启用、域名（通配规则带 `*.` 前缀）。用制表符而不是逗号或空格：
 * 域名里不可能出现制表符，因此无需转义，也就不存在「转义写错导致规则被静默改义」的风险。
 *
 * 非法行返回 null 由调用方跳过并记日志，绝不让一行坏数据阻断整个文件（工程规则第 29、39.2 节）。
 * 校验拆成三个小函数，每一步只判两个条件：单一函数里堆一长串 null 判断既超复杂度上限，
 * 也让「哪一步失败」这件事在阅读时消失。
 */
internal object RuleFileCodec {
    fun format(rule: DomainRule): String = listOf(
        rule.action.name,
        rule.source.name,
        rule.enabled.toString(),
        rule.host,
    ).joinToString(SEPARATOR.toString())

    fun parse(line: String): DomainRule? {
        val columns = line.split(SEPARATOR)
        return if (columns.size == COLUMN_COUNT) toRule(columns) else null
    }

    private fun toRule(columns: List<String>): DomainRule? {
        val action = columns[ACTION_COLUMN].toEnumOrNull<RuleAction>()
        val source = columns[SOURCE_COLUMN].toEnumOrNull<RuleSource>()
        val enabled = columns[ENABLED_COLUMN].toBooleanStrictOrNull()
        val host = canonicalHost(columns[HOST_COLUMN])
        return if (action != null && source != null) {
            buildRule(action, source, enabled, host)
        } else {
            null
        }
    }

    private fun buildRule(action: RuleAction, source: RuleSource, enabled: Boolean?, host: String?): DomainRule? =
        if (enabled != null && host != null) {
            DomainRule(host = host, action = action, source = source, enabled = enabled)
        } else {
            null
        }

    private fun canonicalHost(raw: String): String? {
        val normalized = HostNormalizer.normalizeRule(raw) ?: return null
        return if (normalized.isWildcard) "$WILDCARD_PREFIX${normalized.host}" else normalized.host
    }

    private inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? =
        enumValues<T>().firstOrNull { value -> value.name.equals(this, ignoreCase = true) }
}
