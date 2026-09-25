package xin.ctkqiang.nezha_cyber.ads_block.domain.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val TAOBAO = "com.taobao.taobao"

private const val WECHAT = "com.tencent.mm"

/** 本应用自己的包名。回环防护的判据是它，因此不能写成散落在测试里的字面量。 */
private const val OWN_PACKAGE = "xin.ctkqiang.nezha_cyber.ads_block"

/**
 * 通知规则引擎的判定测试。
 *
 * 覆盖规格要求的最小用例集：包名一致、包名不一致、包含匹配、不匹配、规则已禁用、多规则命中、
 * 字段全空。这些用例锁住的是「用户配了什么就该发生什么」——引擎一旦在这些点上错，
 * 用户看到的是通知被莫名拦掉或规则形同虚设。
 *
 * 断言用的通知内容一律直接构造 [NotificationContent]，不再套一层转发同名参数的辅助函数：
 * 那个辅助函数的默认值与模型完全一致，只是把每个参数原样传一遍，多一层就多一处会与模型脱节的地方。
 */
class NotificationRuleEngineTest {
    @Test
    fun `包名一致且文本包含匹配文字时拦截`() {
        val decision = evaluate(
            rules = listOf(rule(packageName = TAOBAO, matchText = "优惠")),
            content = NotificationContent(packageName = TAOBAO, text = "限时优惠活动"),
        )

        assertTrue(decision is NotificationFilterDecision.Block)
    }

    @Test
    fun `包名不一致时放行`() {
        val decision = evaluate(
            rules = listOf(rule(packageName = TAOBAO, matchText = "优惠")),
            content = NotificationContent(packageName = WECHAT, text = "限时优惠活动"),
        )

        assertEquals(NotificationFilterDecision.Allow, decision)
    }

    @Test
    fun `匹配文字出现在文本任意位置都算命中`() {
        val engine = NotificationRuleEngine.from(listOf(rule(packageName = TAOBAO, matchText = "优惠")))

        listOf("今日优惠", "新人优惠活动", "限时优惠").forEach { text ->
            val decision = engine.evaluate(NotificationContent(packageName = TAOBAO, text = text))
            assertTrue("『$text』应当命中", decision is NotificationFilterDecision.Block)
        }
    }

    @Test
    fun `不包含匹配文字时放行`() {
        val decision = evaluate(
            rules = listOf(rule(packageName = TAOBAO, matchText = "优惠")),
            content = NotificationContent(packageName = TAOBAO, text = "您的订单已经发货"),
        )

        assertEquals(NotificationFilterDecision.Allow, decision)
    }

    @Test
    fun `规则已禁用时放行`() {
        val decision = evaluate(
            rules = listOf(rule(packageName = TAOBAO, matchText = "优惠", enabled = false)),
            content = NotificationContent(packageName = TAOBAO, text = "限时优惠活动"),
        )

        assertEquals(NotificationFilterDecision.Allow, decision)
    }

    @Test
    fun `无任何规则时放行`() {
        assertEquals(
            NotificationFilterDecision.Allow,
            NotificationRuleEngine.Empty.evaluate(NotificationContent(packageName = TAOBAO, text = "限时优惠活动")),
        )
    }

    @Test
    fun `命中记录命中的是哪条规则与哪个字段`() {
        val matchedRule = rule(packageName = TAOBAO, matchText = "优惠")
        val decision = evaluate(
            rules = listOf(rule(packageName = TAOBAO, matchText = "别的"), matchedRule),
            content = NotificationContent(packageName = TAOBAO, title = "双11优惠活动开始啦"),
        )

        val block = decision as NotificationFilterDecision.Block
        assertEquals(matchedRule, block.rule)
        assertEquals(NotificationTextField.Title, block.field)
    }

    @Test
    fun `多个规则同时命中时取列表中的第一条`() {
        val first = rule(packageName = TAOBAO, matchText = "优惠")
        val second = rule(packageName = TAOBAO, matchText = "活动")
        val engine = NotificationRuleEngine.from(listOf(first, second))

        val decision = engine.evaluate(NotificationContent(packageName = TAOBAO, text = "限时优惠活动"))

        // 顺序不稳定会让同一段文字今天被这条规则拦、明天被那条拦，用户无法判断是哪条生效。
        assertEquals(first, (decision as NotificationFilterDecision.Block).rule)
    }

    @Test
    fun `规则顺序不同则命中的规则随之不同`() {
        val first = rule(packageName = TAOBAO, matchText = "优惠")
        val second = rule(packageName = TAOBAO, matchText = "活动")
        val engine = NotificationRuleEngine.from(listOf(second, first))

        val decision = engine.evaluate(NotificationContent(packageName = TAOBAO, text = "限时优惠活动"))

        assertEquals(second, (decision as NotificationFilterDecision.Block).rule)
    }

    @Test
    fun `各文本字段任一命中都算命中`() {
        val engine = NotificationRuleEngine.from(listOf(rule(packageName = TAOBAO, matchText = "优惠")))

        listOf(
            NotificationContent(packageName = TAOBAO, title = "优惠"),
            NotificationContent(packageName = TAOBAO, text = "优惠"),
            NotificationContent(packageName = TAOBAO, bigText = "优惠"),
            NotificationContent(packageName = TAOBAO, subText = "优惠"),
            NotificationContent(packageName = TAOBAO, textLines = listOf("第一条", "优惠")),
        ).forEach { candidate ->
            assertTrue("应当命中：$candidate", engine.evaluate(candidate) is NotificationFilterDecision.Block)
        }
    }

    @Test
    fun `所有文本字段为空时放行且不抛异常`() {
        val decision = evaluate(
            rules = listOf(rule(packageName = TAOBAO, matchText = "优惠")),
            content = NotificationContent(packageName = TAOBAO),
        )

        assertEquals(NotificationFilterDecision.Allow, decision)
    }

    @Test
    fun `大小写不敏感`() {
        val engine = NotificationRuleEngine.from(listOf(rule(packageName = TAOBAO, matchText = "Sale")))
        val upper = NotificationContent(packageName = TAOBAO, text = "BIG SALE NOW")
        val lower = NotificationContent(packageName = TAOBAO, text = "big sale now")

        assertTrue(engine.evaluate(upper) is NotificationFilterDecision.Block)
        assertTrue(engine.evaluate(lower) is NotificationFilterDecision.Block)
    }

    @Test
    fun `匹配文字为空时不命中任何通知`() {
        // 空串被任何非空文本包含。若放它进来，一条空规则会拦下该应用的全部通知。
        val engine = NotificationRuleEngine.from(listOf(rule(packageName = TAOBAO, matchText = "")))

        assertEquals(
            NotificationFilterDecision.Allow,
            engine.evaluate(NotificationContent(packageName = TAOBAO, text = "您的订单已经发货")),
        )
        assertEquals(
            NotificationFilterDecision.Allow,
            engine.evaluate(NotificationContent(packageName = TAOBAO, text = "限时优惠活动")),
        )
    }

    @Test
    fun `本应用自己的包名不会被别的应用的规则误伤`() {
        // 回环防护的主判据就是包名：替代通知由本应用发布，因此天然落在任何规则的包名之外。
        val engine = NotificationRuleEngine.from(listOf(rule(packageName = TAOBAO, matchText = "哪吒")))

        assertEquals(
            NotificationFilterDecision.Allow,
            engine.evaluate(NotificationContent(packageName = OWN_PACKAGE, text = "哪吒反广已拦截此通知")),
        )
    }

    private fun evaluate(rules: List<NotificationRule>, content: NotificationContent): NotificationFilterDecision =
        NotificationRuleEngine.from(rules).evaluate(content)

    private fun rule(packageName: String, matchText: String, enabled: Boolean = true) = NotificationRule(
        id = NotificationRuleId(0L),
        packageName = packageName,
        matchText = matchText,
        enabled = enabled,
    )
}
