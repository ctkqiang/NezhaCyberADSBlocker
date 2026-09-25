package xin.ctkqiang.nezha_cyber.ads_block.data.analysis

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xin.ctkqiang.nezha_cyber.ads_block.domain.analysis.ApkAnalysisOutcome
import xin.ctkqiang.nezha_cyber.ads_block.domain.analysis.ApkAnalysisResult
import xin.ctkqiang.nezha_cyber.ads_block.domain.analysis.ApkAnalyzer
import xin.ctkqiang.nezha_cyber.ads_block.domain.analysis.ApkSource

private const val CACHE_FILE_NAME = "analysis-target.apk"

private const val LOG_TAG = "NezhaApkAnalyzer"

/**
 * APK 分析适配器。
 *
 * 步骤与理由：
 * 1. **先把所选文件复制到私有缓存**。平台读 APK 元数据的接口只接受文件路径，不接受内容 URI；
 *    复制一次即可同时满足元数据读取与 ZIP 扫描两件事。
 * 2. **用 `getPackageArchiveInfo` 取包名与版本**。这是平台自己的清单解析器，比自写二进制
 *    XML 解析可靠；读不到也不影响 DEX 扫描，因此它失败只降级不报错。
 * 3. **扫描 DEX 字符串池**（见 [DexDomainScanner]）。
 *
 * 全程只读不执行（工程规则第 21 节），且整段跑在 IO 线程上：几十兆的 APK 有多个 DEX，
 * 放在主线程会直接卡住界面。
 */
internal class AndroidApkAnalyzer(private val context: Context) : ApkAnalyzer {
    private val scanner = DexDomainScanner()

    override suspend fun analyze(source: ApkSource): ApkAnalysisOutcome = withContext(Dispatchers.IO) {
        val localFile = copyToCache(source) ?: return@withContext ApkAnalysisOutcome.Unreadable
        try {
            analyzeLocalCopy(source = source, file = localFile)
        } finally {
            // 缓存文件承载的是用户选择的任意文件，分析完立即删除，不必占用空间也不需要清理策略。
            localFile.delete()
        }
    }

    private fun analyzeLocalCopy(source: ApkSource, file: File): ApkAnalysisOutcome {
        val scan = scanOrNull(file) ?: return ApkAnalysisOutcome.Unreadable
        val archive = readArchiveMetadata(file)
        return ApkAnalysisOutcome.Analyzed(
            ApkAnalysisResult(
                displayName = source.displayName,
                packageName = archive?.packageName,
                versionName = archive?.versionName,
                dexFileCount = scan.dexFileCount,
                candidates = scan.candidates,
                isTruncated = scan.isTruncated,
            ),
        )
    }

    private fun copyToCache(source: ApkSource): File? {
        val target = File(context.cacheDir, CACHE_FILE_NAME)
        val stream = openSourceStream(source) ?: return null
        return try {
            stream.use { input -> target.outputStream().use { output -> input.copyTo(output) } }
            target
        } catch (unreadable: IOException) {
            Log.w(LOG_TAG, "写入分析缓存失败：${source.displayName}", unreadable)
            null
        }
    }

    private fun openSourceStream(source: ApkSource): InputStream? = try {
        context.contentResolver.openInputStream(Uri.parse(source.handle))
    } catch (denied: SecurityException) {
        // 选择器的授权可能已失效（进程被杀后重建），属于预期内的失败，如实返回而不要崩溃。
        Log.w(LOG_TAG, "读取所选文件的授权已失效：${source.displayName}", denied)
        null
    }

    private fun scanOrNull(file: File): DomainScanOutcome? = try {
        ZipFile(file).use { archive -> scanner.scan(archive) }
    } catch (invalid: IOException) {
        // ZipException 也在这条分支下：文件不是有效 ZIP，或 DEX 条目在读取时损坏。
        Log.w(LOG_TAG, "所选文件不是可解析的 APK", invalid)
        null
    }

    /**
     * 读取平台解析出的包名与版本。
     *
     * 这里豁免 TooGenericExceptionCaught：平台的清单解析器对畸形清单抛的是未受检异常，
     * 没有更具体的类型可以捕获。元数据是可选信息，拿不到就留空，让 DEX 扫描结果照常返回，
     * 而不是让整次分析失败。
     */
    @Suppress("TooGenericExceptionCaught")
    private fun readArchiveMetadata(file: File): PackageInfo? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageArchiveInfo(file.absolutePath, PackageManager.PackageInfoFlags.of(0L))
        } else {
            readArchiveMetadataLegacy(file.absolutePath)
        }
    } catch (parseFailure: RuntimeException) {
        Log.w(LOG_TAG, "清单解析失败，仅返回 DEX 扫描结果", parseFailure)
        null
    }

    @Suppress("DEPRECATION")
    private fun readArchiveMetadataLegacy(path: String): PackageInfo? =
        context.packageManager.getPackageArchiveInfo(path, 0)
}
