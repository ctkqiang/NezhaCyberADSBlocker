package xin.ctkqiang.nezha_cyber.ads_block.domain.analysis

/**
 * APK 静态分析结果。
 *
 * [packageName] 与 [versionName] 可能为 null：它们来自平台对清单的解析，解析失败不影响
 * DEX 扫描的结果，因此这里是可空而不是让整次分析失败。
 */
data class ApkAnalysisResult(
    val displayName: String,
    val packageName: String?,
    val versionName: String?,
    val dexFileCount: Int,
    val candidates: List<DomainCandidate>,
    val isTruncated: Boolean,
)
