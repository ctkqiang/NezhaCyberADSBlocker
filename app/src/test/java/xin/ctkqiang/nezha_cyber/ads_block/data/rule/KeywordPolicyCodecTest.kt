package xin.ctkqiang.nezha_cyber.ads_block.data.rule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.KeywordBlockingPolicy

/**
 * 关键词策略文件格式的往返与降级测试。
 *
 * 最关键的一条是最后两个用例：**「文件不存在」与「用户清空了全部关键词」必须区分**。
 * 把后者也当成默认值，用户就永远无法通过清空来关掉关键词拦截。
 */
class KeywordPolicyCodecTest {
    @Test
    fun `往返保持开关与关键词集合`() {
        val policy = KeywordBlockingPolicy(enabled = false, keywords = setOf("ads", "pagead"))

        val parsed = KeywordPolicyCodec.parse(KeywordPolicyCodec.format(policy))

        assertNotNull(parsed)
        assertEquals(false, parsed?.enabled)
        assertEquals(setOf("ads", "pagead"), parsed?.keywords)
    }

    @Test
    fun `关键词按字典序写出`() {
        val policy = KeywordBlockingPolicy(enabled = true, keywords = setOf("pagead", "ads", "advert"))

        assertEquals(listOf("enabled", "ads", "advert", "pagead"), KeywordPolicyCodec.format(policy))
    }

    @Test
    fun `空内容与无法识别的首行都返回 null`() {
        assertNull(KeywordPolicyCodec.parse(emptyList()))
        assertNull(KeywordPolicyCodec.parse(listOf("", "   ")))
        assertNull(KeywordPolicyCodec.parse(listOf("ads", "pagead")))
    }

    @Test
    fun `非法关键词行被跳过而不是中断解析`() {
        val parsed = KeywordPolicyCodec.parse(listOf("enabled", "ads", "ads.example.com", "广告", "pagead"))

        assertNotNull(parsed)
        assertEquals(setOf("ads", "pagead"), parsed?.keywords)
    }

    @Test
    fun `只有首行时得到一份空关键词集合而不是默认值`() {
        val parsed = KeywordPolicyCodec.parse(listOf("disabled"))

        assertNotNull(parsed)
        assertEquals(false, parsed?.enabled)
        assertTrue(parsed?.keywords.orEmpty().isEmpty())
    }
}
