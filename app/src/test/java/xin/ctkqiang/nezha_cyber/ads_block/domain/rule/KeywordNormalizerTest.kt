package xin.ctkqiang.nezha_cyber.ads_block.domain.rule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 关键词归一化的行为测试。
 *
 * 锁住的是「关键词是单个 DNS 标签」这条约束。它一旦放松，带点的「关键词」会被存下来，
 * 而匹配是按标签逐段比对的，于是这条规则永远命不中任何域名，却在界面上看起来已经生效。
 */
class KeywordNormalizerTest {
    @Test
    fun `单个标签被接受并转为小写`() {
        assertEquals("ads", KeywordNormalizer.normalize("Ads"))
        assertEquals("ads", KeywordNormalizer.normalize("  ads  "))
        assertEquals("ad_server", KeywordNormalizer.normalize("AD_SERVER"))
    }

    @Test
    fun `带连字符与下划线的标签被接受`() {
        assertEquals("ad-server", KeywordNormalizer.normalize("ad-server"))
        assertEquals("ad_banner", KeywordNormalizer.normalize("ad_banner"))
    }

    @Test
    fun `多标签被拒绝`() {
        assertNull(KeywordNormalizer.normalize("ads.example.com"))
        assertNull(KeywordNormalizer.normalize("trailing."))
    }

    @Test
    fun `空值与非标签字符被拒绝`() {
        assertNull(KeywordNormalizer.normalize(""))
        assertNull(KeywordNormalizer.normalize("   "))
        assertNull(KeywordNormalizer.normalize("ad s"))
        assertNull(KeywordNormalizer.normalize("广告"))
    }

    @Test
    fun `以连字符或下划线开头的标签被拒绝`() {
        assertNull(KeywordNormalizer.normalize("-ads"))
        assertNull(KeywordNormalizer.normalize("_ads"))
    }

    @Test
    fun `超过 DNS 标签长度上限的被拒绝`() {
        val maxLength = "a".repeat(63)

        assertEquals(maxLength, KeywordNormalizer.normalize(maxLength))
        assertNull(KeywordNormalizer.normalize("a".repeat(64)))
    }
}
