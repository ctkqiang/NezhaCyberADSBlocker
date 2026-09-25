package xin.ctkqiang.nezha_cyber.ads_block.domain.rule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 规则引擎的匹配与优先级测试。
 *
 * 覆盖工程规则第 30 节列出的条目：精确匹配、子域名匹配、通配符、大写、结尾点、非法域名、
 * 白名单覆盖，以及第 10 节要求的确定性优先级。这些都是「错了就会放行该拦的、或拦掉不该拦的」
 * 的行为，必须锁住。
 */
class DomainRuleEngineTest {
    private val adsHost = "ads.example.com"
    private val trackerHost = "tracker.example.net"

    @Test
    fun `内置规则精确命中`() {
        val engine = engineWith(builtin(adsHost))

        val decision = engine.evaluate(adsHost)

        assertEquals(RuleAction.BLOCK, decision.action)
        assertEquals(adsHost, decision.matchedRule)
        assertEquals(RuleSource.BUILTIN, decision.source)
    }

    @Test
    fun `父域规则命中其子域名`() {
        val engine = engineWith(builtin("example.com"))

        val decision = engine.evaluate("cdn.static.example.com")

        assertEquals(RuleAction.BLOCK, decision.action)
        assertEquals("example.com", decision.matchedRule)
    }

    @Test
    fun `通配规则命中多级子域`() {
        val engine = engineWith(builtin("*.ads.example.com"))

        assertEquals(RuleAction.BLOCK, engine.evaluate("a.ads.example.com").action)
        assertEquals(RuleAction.BLOCK, engine.evaluate("a.b.ads.example.com").action)
    }

    @Test
    fun `通配规则不命中裸域`() {
        val engine = engineWith(builtin("*.ads.example.com"))

        assertEquals(FilterDecision.Unknown, engine.evaluate("ads.example.com"))
    }

    @Test
    fun `大写与结尾点被归一化后仍能命中`() {
        val engine = engineWith(builtin(adsHost))

        assertEquals(RuleAction.BLOCK, engine.evaluate("ADS.Example.COM").action)
        assertEquals(RuleAction.BLOCK, engine.evaluate("$adsHost.").action)
    }

    @Test
    fun `用户放行优先于内置阻断`() {
        val engine = engineWith(
            builtin(adsHost),
            user(host = "ads.example.com", action = RuleAction.ALLOW),
        )

        val decision = engine.evaluate(adsHost)

        assertEquals(RuleAction.ALLOW, decision.action)
        assertEquals(RuleSource.USER, decision.source)
    }

    @Test
    fun `用户阻断优先于内置阻断并说明来源`() {
        val engine = engineWith(
            builtin(adsHost),
            user(host = "sub.ads.example.com", action = RuleAction.BLOCK),
        )

        val decision = engine.evaluate("sub.ads.example.com")

        assertEquals(RuleAction.BLOCK, decision.action)
        assertEquals(RuleSource.USER, decision.source)
        assertEquals("sub.ads.example.com", decision.matchedRule)
    }

    @Test
    fun `用户放行对子域名同样生效`() {
        val engine = engineWith(
            builtin("example.com"),
            user(host = "api.example.com", action = RuleAction.ALLOW),
        )

        assertEquals(RuleAction.ALLOW, engine.evaluate("api.example.com").action)
        assertEquals(RuleAction.BLOCK, engine.evaluate("cdn.example.com").action)
    }

    @Test
    fun `未命中任何规则时返回未知而不是放行结论`() {
        val engine = engineWith(builtin(adsHost))

        val decision = engine.evaluate(trackerHost)

        assertEquals(RuleAction.ALLOW, decision.action)
        assertNull(decision.matchedRule)
        assertNull(decision.source)
    }

    @Test
    fun `停用的规则不参与匹配`() {
        val engine = engineWith(builtin(adsHost).copy(enabled = false))

        assertEquals(FilterDecision.Unknown, engine.evaluate(adsHost))
    }

    @Test
    fun `非法域名一律返回未知`() {
        val engine = engineWith(builtin(adsHost))

        assertEquals(FilterDecision.Unknown, engine.evaluate(""))
        assertEquals(FilterDecision.Unknown, engine.evaluate("localhost"))
        assertEquals(FilterDecision.Unknown, engine.evaluate("not a domain"))
        assertEquals(FilterDecision.Unknown, engine.evaluate("https://ads.example.com/path"))
    }

    @Test
    fun `关键词按标签命中并标注为关键词来源`() {
        val engine = engineWith(KeywordBlockingPolicy())

        val decision = engine.evaluate("ads.example.com")

        assertEquals(RuleAction.BLOCK, decision.action)
        assertEquals(RuleSource.KEYWORD, decision.source)
        assertEquals("ads", decision.matchedRule)
    }

    @Test
    fun `关键词命中任意层级的标签`() {
        val engine = engineWith(KeywordBlockingPolicy())

        assertEquals(RuleAction.BLOCK, engine.evaluate("cdn.ads.example.com").action)
        assertEquals(RuleAction.BLOCK, engine.evaluate("ads-banner.example.com").action)
        assertEquals(RuleAction.BLOCK, engine.evaluate("ads_banner.example.com").action)
    }

    @Test
    fun `关键词不做子串匹配以免误伤`() {
        val engine = engineWith(KeywordBlockingPolicy())

        assertEquals(FilterDecision.Unknown, engine.evaluate("uploads.example.com"))
        assertEquals(FilterDecision.Unknown, engine.evaluate("downloads.example.com"))
        assertEquals(FilterDecision.Unknown, engine.evaluate("adsapi.example.com"))
    }

    @Test
    fun `用户放行压过关键词兜底`() {
        val engine = engineWith(
            KeywordBlockingPolicy(),
            user(host = "ads.example.com", action = RuleAction.ALLOW),
        )

        assertEquals(RuleAction.ALLOW, engine.evaluate("ads.example.com").action)
    }

    @Test
    fun `内置清单压过关键词兜底`() {
        val engine = engineWith(KeywordBlockingPolicy(), builtin("cdn.ads.example.com"))

        val decision = engine.evaluate("cdn.ads.example.com")

        assertEquals(RuleSource.BUILTIN, decision.source)
        assertEquals("cdn.ads.example.com", decision.matchedRule)
    }

    @Test
    fun `关键词策略关闭时不再兜底`() {
        val engine = engineWith(KeywordBlockingPolicy(enabled = false))

        assertEquals(FilterDecision.Unknown, engine.evaluate("ads.example.com"))
    }

    /**
     * 通用构造器默认关闭关键词兜底：绝大多数用例考察的是清单与用户规则的匹配，
     * 开着兜底会让 `ads.example.com` 这类测试域名被启发式顺手拦掉，掩盖被测行为。
     * 关键词相关的用例显式传入策略。
     */
    private fun engineWith(vararg rules: DomainRule): RuleEngine =
        DomainRuleEngine(rules.toList(), KeywordBlockingPolicy(enabled = false))

    private fun engineWith(policy: KeywordBlockingPolicy, vararg rules: DomainRule): RuleEngine =
        DomainRuleEngine(rules.toList(), policy)

    private fun builtin(host: String): DomainRule =
        DomainRule(host = host, action = RuleAction.BLOCK, source = RuleSource.BUILTIN)

    private fun user(host: String, action: RuleAction): DomainRule =
        DomainRule(host = host, action = action, source = RuleSource.USER)
}
