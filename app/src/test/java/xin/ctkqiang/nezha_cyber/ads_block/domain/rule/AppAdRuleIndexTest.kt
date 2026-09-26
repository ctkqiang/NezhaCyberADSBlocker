package xin.ctkqiang.nezha_cyber.ads_block.domain.rule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val WEIBO = "com.sina.weibo"

private const val TAOBAO = "com.taobao.taobao"

/**
 * 应用专属广告规则索引的匹配测试。
 *
 * 这里锁住的是「作用域」这件事：一条应用专属规则只能命中它写明的那个应用。
 * 作用域一旦失效，规则就会外溢到其它应用——那正是「装了广告拦截之后别的应用也跟着坏」
 * 这类最难归因的故障（工程规则第 30、32 节）。
 */
class AppAdRuleIndexTest {
    @Test
    fun `只对写明的包命中`() {
        val index = AppAdRuleIndex(listOf(AppAdRule(packageName = WEIBO, host = "sax.sina.com.cn")))

        assertEquals("sax.sina.com.cn", index.match("sax.sina.com.cn", WEIBO))
        assertNull(index.match("sax.sina.com.cn", TAOBAO))
    }

    @Test
    fun `包名未知时不命中`() {
        val index = AppAdRuleIndex(listOf(AppAdRule(packageName = WEIBO, host = "sax.sina.com.cn")))

        assertNull(index.match("sax.sina.com.cn", null))
    }

    @Test
    fun `父域规则命中其子域名`() {
        val index = AppAdRuleIndex(listOf(AppAdRule(packageName = WEIBO, host = "sina.com.cn")))

        assertEquals("sina.com.cn", index.match("sax.sina.com.cn", WEIBO))
    }

    @Test
    fun `通配规则命中严格子域但不命中裸域`() {
        val index = AppAdRuleIndex(listOf(AppAdRule(packageName = TAOBAO, host = "*.tanx.com")))

        assertEquals("*.tanx.com", index.match("a.tanx.com", TAOBAO))
        assertEquals("*.tanx.com", index.match("a.b.tanx.com", TAOBAO))
        assertNull(index.match("tanx.com", TAOBAO))
    }

    @Test
    fun `同一个包的多条规则互不干扰`() {
        val index = AppAdRuleIndex(
            listOf(
                AppAdRule(packageName = WEIBO, host = "sax.sina.com.cn"),
                AppAdRule(packageName = WEIBO, host = "adbox.sina.com.cn"),
            ),
        )

        assertEquals("adbox.sina.com.cn", index.match("adbox.sina.com.cn", WEIBO))
        assertNull(index.match("sax.sina.com.cn", TAOBAO))
    }

    @Test
    fun `空清单一律不命中`() {
        val index = AppAdRuleIndex(emptyList())

        assertNull(index.match("sax.sina.com.cn", WEIBO))
    }
}
