package xin.ctkqiang.nezha_cyber.ads_block.domain.rule

/**
 * 关键词启发式拦截策略。
 *
 * 它补的是精编清单的缺口：清单只覆盖已知域名，而广告域名每天都在新增，
 * 出现 `ads.某个新域名.com` 时清单往往还没收录。
 *
 * **匹配单位是 DNS 标签，不是子串。** 这一条是硬约束，不是实现细节：
 * 第 17 节明确禁止「域名里出现 ads 就拦」这类粗暴规则，因为 `uploads.example.com`、
 * `downloads.example.com` 都含 `ads` 子串却完全无辜。按标签匹配把误伤面收敛到
 * 「有一个标签就叫 ads」这类真正可疑的命名。
 *
 * 默认关键词只收广告语义最强的几个，且刻意**不含** `tracker`、`analytics` 这类追踪词：
 * 它们同样可能出现在自建服务里，等用户能自行编辑关键词时再决定要不要加。
 */
data class KeywordBlockingPolicy(val enabled: Boolean = true, val keywords: Set<String> = DEFAULT_KEYWORDS) {
    companion object {
        /** 默认关键词。全部小写，且都必须是单个合法标签。 */
        val DEFAULT_KEYWORDS: Set<String> = setOf(
            "ads",
            "adserver",
            "adservice",
            "advert",
            "advertising",
            "pagead",
            "adclick",
        )

        val Default = KeywordBlockingPolicy()
    }
}
