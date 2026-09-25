package xin.ctkqiang.nezha_cyber.ads_block.domain.analysis

/**
 * 端口：APK 静态分析。
 *
 * 实现负责取文件、解压、解析 DEX，并保证**只读不执行**（工程规则第 21 节：
 * 静态分析必须保持静态，不得执行从 APK 中提取出的任何内容）。
 */
interface ApkAnalyzer {
    /**
     * 分析一个 APK。
     *
     * 必须是可取消且在主线程之外执行的：一个几十兆的 APK 有多个 DEX，扫描需要时间。
     */
    suspend fun analyze(source: ApkSource): ApkAnalysisOutcome
}
