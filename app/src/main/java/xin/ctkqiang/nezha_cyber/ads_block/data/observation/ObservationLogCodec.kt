package xin.ctkqiang.nezha_cyber.ads_block.data.observation

import java.time.Instant
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.DomainObservation
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleAction
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleSource

private const val COLUMN_COUNT = 6

private const val TIME_COLUMN = 0

private const val ACTION_COLUMN = 1

private const val MATCHED_RULE_COLUMN = 2

private const val SOURCE_COLUMN = 3

private const val PACKAGE_COLUMN = 4

private const val HOST_COLUMN = 5

private const val SEPARATOR = '\t'

private const val EMPTY = "-"

/**
 * 观测日志的行格式。
 *
 * 列：时间戳（epoch 毫秒）、动作、命中规则、规则来源、应用包名、域名。
 * 缺失值写成 `-`：域名不可能等于 `-`（单标签被归一化拒绝），因此它不会与真实值冲突。
 */
internal object ObservationLogCodec {
    fun format(observation: DomainObservation): String = listOf(
        observation.at.toEpochMilli().toString(),
        observation.action.name,
        observation.matchedRule ?: EMPTY,
        observation.source?.name ?: EMPTY,
        observation.packageName ?: EMPTY,
        observation.host,
    ).joinToString(SEPARATOR.toString())

    fun parse(line: String): DomainObservation? {
        val columns = line.split(SEPARATOR)
        return if (columns.size == COLUMN_COUNT) toObservation(columns) else null
    }

    private fun toObservation(columns: List<String>): DomainObservation? {
        val epochMillis = columns[TIME_COLUMN].toLongOrNull()
        val action = columns[ACTION_COLUMN].toEnumOrNull<RuleAction>()
        val host = columns[HOST_COLUMN]
        return if (epochMillis != null && action != null && host.isNotEmpty()) {
            DomainObservation(
                at = Instant.ofEpochMilli(epochMillis),
                host = host,
                action = action,
                matchedRule = columns[MATCHED_RULE_COLUMN].takeIf { value -> value != EMPTY },
                source = columns[SOURCE_COLUMN].takeIf { value -> value != EMPTY }?.toEnumOrNull<RuleSource>(),
                packageName = columns[PACKAGE_COLUMN].takeIf { value -> value != EMPTY },
            )
        } else {
            null
        }
    }

    private inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? =
        enumValues<T>().firstOrNull { value -> value.name.equals(this, ignoreCase = true) }
}
