package xin.ctkqiang.nezha_cyber.ads_block.domain.analysis

/**
 * 待分析的 APK。
 *
 * [handle] 是交由适配器解释的不透明句柄——在 Android 上就是内容 URI。领域层不解析它、
 * 也不假设它的格式，只负责原样传回，因此这里不出现任何平台类型（工程规则第 38.3 节）。
 */
data class ApkSource(val handle: String, val displayName: String)
