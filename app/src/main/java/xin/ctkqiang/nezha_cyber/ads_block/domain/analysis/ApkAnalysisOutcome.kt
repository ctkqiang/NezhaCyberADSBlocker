package xin.ctkqiang.nezha_cyber.ads_block.domain.analysis

/**
 * 静态分析的结果。
 *
 * 用封闭类型而不是抛异常或返回可空值：读不懂一个 APK 是**预期内的领域结果**——
 * 用户完全可能选了一个改名过的压缩包或损坏的文件，这不是缺陷（工程规则第 29、37.3 节）。
 */
sealed interface ApkAnalysisOutcome {
    data class Analyzed(val result: ApkAnalysisResult) : ApkAnalysisOutcome

    /** 文件读不出来，或者不是一个合法的 APK。 */
    data object Unreadable : ApkAnalysisOutcome
}
