package xin.ctkqiang.nezha_cyber.ads_block.data.privacy

import org.junit.Assert.assertEquals
import org.junit.Test
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.BlockedResponseMode
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.ObservationRetention
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.PrivacyPolicy

/**
 * 隐私策略文件格式的往返与容错测试。
 *
 * 这个文件是用户「不要记录我」这条意愿的落点，因此容错方向必须是**保守**的：
 * 读不懂的键一律跳过、缺失的键退回默认值，绝不因为一行坏数据把整份配置丢掉或把记录悄悄打开。
 */
class PrivacyPolicyCodecTest {
    @Test
    fun `往返保持三项设置`() {
        val policy = PrivacyPolicy(
            isObservationLoggingEnabled = false,
            observationRetention = ObservationRetention.Extended,
            blockedResponseMode = BlockedResponseMode.ZeroAddress,
        )

        assertEquals(policy, PrivacyPolicyCodec.parse(PrivacyPolicyCodec.format(policy)))
    }

    @Test
    fun `空内容退回默认策略`() {
        assertEquals(PrivacyPolicy.Default, PrivacyPolicyCodec.parse(emptyList()))
        assertEquals(PrivacyPolicy.Default, PrivacyPolicyCodec.parse(listOf("", "   ")))
    }

    @Test
    fun `未知键被跳过而不影响已知键`() {
        val parsed = PrivacyPolicyCodec.parse(
            listOf(
                "logging\tfalse",
                "future_setting\twhatever",
                "retention\tminimal",
            ),
        )

        assertEquals(false, parsed.isObservationLoggingEnabled)
        assertEquals(ObservationRetention.Minimal, parsed.observationRetention)
        // 未出现的键保持默认，而不是被前面的坏行带偏。
        assertEquals(PrivacyPolicy.Default.blockedResponseMode, parsed.blockedResponseMode)
    }

    @Test
    fun `无法识别的取值退回该项的默认值`() {
        val parsed = PrivacyPolicyCodec.parse(
            listOf(
                "logging\tmaybe",
                "retention\tunknown",
                "blocked_response\tunknown",
            ),
        )

        assertEquals(PrivacyPolicy.Default, parsed)
    }

    @Test
    fun `列数不对的行被忽略`() {
        val parsed = PrivacyPolicyCodec.parse(listOf("logging", "logging\tfalse\textra"))

        assertEquals(PrivacyPolicy.Default, parsed)
    }
}
