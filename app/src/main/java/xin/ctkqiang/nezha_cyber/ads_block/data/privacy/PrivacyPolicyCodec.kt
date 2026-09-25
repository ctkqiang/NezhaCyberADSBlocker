package xin.ctkqiang.nezha_cyber.ads_block.data.privacy

import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.BlockedResponseMode
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.ObservationRetention
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.PrivacyPolicy

private const val SEPARATOR = '\t'

private const val COLUMN_COUNT = 2

private const val KEY_COLUMN = 0

private const val VALUE_COLUMN = 1

private const val LOGGING_KEY = "logging"

private const val RETENTION_KEY = "retention"

private const val BLOCKED_RESPONSE_KEY = "blocked_response"

/**
 * 隐私策略文件的行格式：每行「键 + 制表符 + 值」。
 *
 * 刻意**不是**按行号排列的位置格式。按键读取有三个直接好处：
 * - 未知键直接跳过，因此将来新增设置项不会让旧版本读不懂新文件；
 * - 缺失的键各自退回默认值，少一行也不会把整份配置丢掉；
 * - 文件本身是自描述的，用户打开就能看懂哪一行管什么。
 *
 * [parse] 永不返回 null：**每一项**都有默认值，因此「文件不存在」「文件为空」「键全部无法识别」
 * 三种情况的正确结果都是默认策略，不需要调用方再分情况。
 */
internal object PrivacyPolicyCodec {
    fun format(policy: PrivacyPolicy): List<String> = listOf(
        "$LOGGING_KEY$SEPARATOR${policy.isObservationLoggingEnabled}",
        "$RETENTION_KEY$SEPARATOR${policy.observationRetention.name}",
        "$BLOCKED_RESPONSE_KEY$SEPARATOR${policy.blockedResponseMode.name}",
    )

    fun parse(lines: List<String>): PrivacyPolicy = lines.fold(PrivacyPolicy.Default) { policy, line ->
        val columns = line.split(SEPARATOR)
        if (columns.size == COLUMN_COUNT) apply(columns[KEY_COLUMN], columns[VALUE_COLUMN], policy) else policy
    }

    private fun apply(key: String, value: String, policy: PrivacyPolicy): PrivacyPolicy = when (key.trim()) {
        LOGGING_KEY -> policy.copy(
            isObservationLoggingEnabled = value.trim().toBooleanStrictOrNull()
                ?: policy.isObservationLoggingEnabled,
        )

        RETENTION_KEY -> policy.copy(
            observationRetention = value.trim().toEnumOrNull<ObservationRetention>()
                ?: policy.observationRetention,
        )

        BLOCKED_RESPONSE_KEY -> policy.copy(
            blockedResponseMode = value.trim().toEnumOrNull<BlockedResponseMode>()
                ?: policy.blockedResponseMode,
        )

        // 未知键：来自更新版本写下的文件，或被用户手工改坏的行。跳过而不是让整份配置失效。
        else -> policy
    }

    private inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? =
        enumValues<T>().firstOrNull { value -> value.name.equals(this, ignoreCase = true) }
}
