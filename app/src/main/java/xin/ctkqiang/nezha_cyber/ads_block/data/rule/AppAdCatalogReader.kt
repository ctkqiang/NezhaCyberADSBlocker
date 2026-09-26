package xin.ctkqiang.nezha_cyber.ads_block.data.rule

import android.content.res.AssetManager
import android.util.Log
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.AppAdRule
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.HostNormalizer
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.WILDCARD_PREFIX

private const val ASSET_NAME = "app_ads.txt"

private const val COMMENT_PREFIX = "#"

private const val LOG_TAG = "NezhaAppAdCatalog"

private const val COLUMN_COUNT = 2

private const val PACKAGE_COLUMN = 0

private const val HOST_COLUMN = 1

/** 包名至少两段，与 Android 的包名形态一致；它只做格式过滤，不代表该包一定存在。 */
private val PACKAGE_PATTERN = Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+")

/** 列分隔符允许连续空白，因此清单里的对齐空格不会让整行变成非法数据。 */
private val COLUMN_SEPARATOR = Regex("\\s+")

/**
 * 应用专属广告清单读取器（工程规则第 44 节）。
 *
 * 格式为每行 `<包名> <域名>`，随应用打包、运行时只读（与第 39.1 节的全局清单同样的约束）。
 * 解析在后台协程中完成，非法行只跳过并记录条数，绝不让一行坏数据毁掉整次加载（第 29、39.2 节）。
 *
 * 文件缺失或不可读时返回空清单：应用照常启动，只是这一档规则不生效，而不是阻断启动。
 */
internal class AppAdCatalogReader(private val assetManager: AssetManager) {
    suspend fun readCatalog(): List<AppAdRule> = withContext(Dispatchers.IO) { readRules() }

    private fun readRules(): List<AppAdRule> {
        var skippedLines = 0
        val rules = mutableListOf<AppAdRule>()
        try {
            assetManager.open(ASSET_NAME).bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val rule = parse(line)
                    if (rule != null) {
                        rules.add(rule)
                    } else if (isMeaningful(line)) {
                        skippedLines += 1
                    }
                }
            }
        } catch (missing: IOException) {
            Log.w(LOG_TAG, "应用专属清单不可读，本次不启用该档规则", missing)
            return emptyList()
        }
        if (skippedLines > 0) {
            Log.w(LOG_TAG, "应用专属清单有 $skippedLines 行被跳过（格式或域名非法）")
        }
        return rules
    }

    private fun isMeaningful(line: String): Boolean = line.isNotBlank() && !line.trimStart().startsWith(COMMENT_PREFIX)

    private fun parse(line: String): AppAdRule? {
        val columns = line.trim().split(COLUMN_SEPARATOR)
        if (columns.size != COLUMN_COUNT) return null
        val packageName = columns[PACKAGE_COLUMN]
        val host = canonicalHost(columns[HOST_COLUMN])
        return if (host != null && PACKAGE_PATTERN.matches(packageName)) {
            AppAdRule(packageName = packageName, host = host)
        } else {
            null
        }
    }

    private fun canonicalHost(raw: String): String? {
        val normalized = HostNormalizer.normalizeRule(raw) ?: return null
        return if (normalized.isWildcard) "$WILDCARD_PREFIX${normalized.host}" else normalized.host
    }
}
