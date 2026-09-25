package xin.ctkqiang.nezha_cyber.ads_block.ui.data

import androidx.compose.runtime.staticCompositionLocalOf
import xin.ctkqiang.nezha_cyber.ads_block.domain.analysis.ApkAnalyzer

/**
 * APK 分析端口的注入点。
 *
 * 界面只依赖领域端口，文件复制、ZIP 解压与 DEX 解析全部留在适配器里（工程规则第 38.2 节）。
 */
val LocalApkAnalyzer = staticCompositionLocalOf<ApkAnalyzer> {
    error("LocalApkAnalyzer 未提供：请在组合根补上 CompositionLocalProvider")
}
