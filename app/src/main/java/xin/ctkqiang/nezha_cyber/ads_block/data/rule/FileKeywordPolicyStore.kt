package xin.ctkqiang.nezha_cyber.ads_block.data.rule

import android.util.Log
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import xin.ctkqiang.nezha_cyber.ads_block.data.writeLinesAtomically
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.KeywordBlockingPolicy
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.KeywordNormalizer
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.KeywordPolicyStore
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleEditResult

private const val FILE_NAME = "keyword-policy.txt"

private const val LOG_TAG = "NezhaKeywordPolicy"

/**
 * 关键词策略的文件实现。
 *
 * 只有一份策略，因此不需要索引结构；写入仍是「先写临时文件再改名」，因为这份文件决定
 * 关键词拦截是否生效，半截内容会让开关落在无法解释的状态上。
 *
 * 载入时**不写回**文件：用户从未改过关键词时文件保持不存在，默认值仍来自
 * [KeywordBlockingPolicy.DEFAULT_KEYWORDS]。把默认值写成一份看起来像「用户配置」的副本，
 * 会让日后调整默认关键词在未改过配置的设备上永远不生效。
 *
 * 文件不可读或首行无法识别时退回默认策略（工程规则第 29 节：不因一份坏文件阻止启动）。
 */
internal class FileKeywordPolicyStore(private val storageDirectory: File) : KeywordPolicyStore {
    private val mutex = Mutex()

    private val file = File(storageDirectory, FILE_NAME)

    private val mutablePolicy = MutableStateFlow(KeywordBlockingPolicy.Default)

    override val policy: StateFlow<KeywordBlockingPolicy> = mutablePolicy.asStateFlow()

    override suspend fun load() {
        withContext(Dispatchers.IO) {
            mutex.withLock { mutablePolicy.value = readPolicy() }
        }
    }

    override suspend fun addKeyword(keyword: String): RuleEditResult {
        val normalized = KeywordNormalizer.normalize(keyword) ?: return RuleEditResult.InvalidHost
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                val current = mutablePolicy.value
                if (normalized in current.keywords) {
                    RuleEditResult.AlreadyExists
                } else {
                    persist(current.copy(keywords = current.keywords + normalized))
                    RuleEditResult.Applied
                }
            }
        }
    }

    override suspend fun removeKeyword(keyword: String): RuleEditResult {
        val normalized = KeywordNormalizer.normalize(keyword) ?: return RuleEditResult.InvalidHost
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                val current = mutablePolicy.value
                if (normalized in current.keywords) {
                    persist(current.copy(keywords = current.keywords - normalized))
                    RuleEditResult.Applied
                } else {
                    RuleEditResult.NotFound
                }
            }
        }
    }

    override suspend fun setEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            mutex.withLock { persist(mutablePolicy.value.copy(enabled = enabled)) }
        }
    }

    /**
     * 读取策略。
     *
     * 「文件不存在」与「用户清空了全部关键词」必须区分：前者返回默认关键词，
     * 后者返回一份空关键词集合。混为一谈会让用户永远无法通过清空关掉关键词拦截。
     */
    private fun readPolicy(): KeywordBlockingPolicy {
        // 文件不存在表示用户从未改过关键词，这是正常状态，不记日志。
        if (!file.exists()) return KeywordBlockingPolicy.Default
        val parsed = readPersisted()
        if (parsed == null) {
            Log.w(LOG_TAG, "关键词策略文件无法解析，本次使用默认关键词")
        }
        return parsed ?: KeywordBlockingPolicy.Default
    }

    private fun readPersisted(): KeywordBlockingPolicy? = try {
        KeywordPolicyCodec.parse(file.readLines())
    } catch (unreadable: IOException) {
        Log.w(LOG_TAG, "关键词策略文件不可读", unreadable)
        null
    }

    private fun persist(policy: KeywordBlockingPolicy) {
        storageDirectory.mkdirs()
        file.writeLinesAtomically(KeywordPolicyCodec.format(policy))
        mutablePolicy.value = policy
    }
}
