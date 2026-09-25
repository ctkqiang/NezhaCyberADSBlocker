package xin.ctkqiang.nezha_cyber.ads_block.domain.rule

/** 归一化后的规则文本。[isWildcard] 为 true 表示原文带 `*.` 前缀。 */
data class NormalizedRule(val host: String, val isWildcard: Boolean)
