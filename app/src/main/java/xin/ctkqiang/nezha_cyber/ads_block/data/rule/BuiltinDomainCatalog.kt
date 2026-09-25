package xin.ctkqiang.nezha_cyber.ads_block.data.rule

import android.content.res.AssetManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.HostNormalizer
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.WILDCARD_PREFIX

private const val DOMAINS_ASSET = "domains.txt"

private const val VERSION_ASSET = "domains.version"

private const val LOG_TAG = "NezhaBuiltinCatalog"

private const val COMMENT_PREFIX = "#"

/**
 * 内置清单读取器。
 *
 * 清单随应用打包、运行时只读（工程规则第 39.1 节），因此这里只读不写。
 * 解析在后台协程中完成（第 39.5 节），并且对非法行只跳过、不中断整个加载（第 29、39.2 节）：
 * 一份数万行的清单里混进一行坏数据是常态，让它毁掉整次加载才是事故。
 *
 * 文件缺失或不可读时返回空清单，应用照常启动（第 39.2 节要求降级而不是阻断启动）。
 */
internal class BuiltinDomainCatalog(private val assetManager: AssetManager) {
    suspend fun readCatalog(): BuiltinCatalog = withContext(Dispatchers.IO) {
        val hosts = readHosts()
        BuiltinCatalog(hosts = hosts, version = readVersion())
    }

    private fun readHosts(): Set<String> {
        var skippedLines = 0
        val hosts = HashSet<String>()
        try {
            assetManager.open(DOMAINS_ASSET).bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val host = parse(line)
                    if (host == null) {
                        if (line.isNotBlank() && !line.startsWith(COMMENT_PREFIX)) skippedLines += 1
                    } else {
                        hosts.add(host)
                    }
                }
            }
        } catch (missing: java.io.IOException) {
            Log.w(LOG_TAG, "内置清单不可读，本次仅使用用户规则", missing)
            return emptySet()
        }
        if (skippedLines > 0) {
            Log.w(LOG_TAG, "内置清单有 $skippedLines 行被跳过（非法域名）")
        }
        return hosts
    }

    private fun parse(line: String): String? {
        val trimmed = line.trim()
        val normalized = if (trimmed.isEmpty() || trimmed.startsWith(COMMENT_PREFIX)) {
            null
        } else {
            HostNormalizer.normalizeRule(trimmed)
        }
        return when {
            normalized == null -> null
            normalized.isWildcard -> "$WILDCARD_PREFIX${normalized.host}"
            else -> normalized.host
        }
    }

    private fun readVersion(): Int = try {
        assetManager.open(VERSION_ASSET).bufferedReader().readText().trim().toIntOrNull() ?: 0
    } catch (missing: java.io.IOException) {
        Log.w(LOG_TAG, "清单版本号缺失，按版本 0 处理", missing)
        0
    }
}
