package xin.ctkqiang.nezha_cyber.ads_block.data.rule

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import xin.ctkqiang.nezha_cyber.ads_block.data.readIntOrZero
import xin.ctkqiang.nezha_cyber.ads_block.data.writeLinesAtomically
import xin.ctkqiang.nezha_cyber.ads_block.data.writeTextAtomically
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.DomainRule
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.HostNormalizer
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleAction
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleEditResult
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleSnapshot
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleSource
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleStore

private const val RULES_FILE_NAME = "rules.txt"

private const val BUILTIN_VERSION_FILE_NAME = "builtin-version.txt"

/**
 * 规则存储的文件实现。
 *
 * 只有两类数据需要持久化：
 * 1. **用户规则**——用户自己加的放行／阻断，以及用户对某条规则的启停；
 * 2. **内置条目的启停覆写**与**已导入的清单版本号**。
 *
 * 内置清单本身留在 assets 里只读，不复制进持久层。数万条域名每次启动写一遍大文件毫无收益，
 * 而清单只读这一条约束由工程规则第 39.1 节固定。这也让「从清单移除的域名」天然不需要物理删除：
 * 它下一次加载就不存在了，用户的覆写仍留在本文件里（第 39.4 节）。
 *
 * 写入采用「先写临时文件再改名」：规则文件是用户数据的唯一副本，写到一半掉电不应该让它损坏。
 */
internal class FileRuleStore(private val storageDirectory: File) : RuleStore {
    private val mutex = Mutex()

    private val rulesFile = File(storageDirectory, RULES_FILE_NAME)

    private val builtinVersionFile = File(storageDirectory, BUILTIN_VERSION_FILE_NAME)

    /** 用户规则，按域名唯一：同一个域名同时放行又阻断是自相矛盾的输入。 */
    private val userRules = mutableMapOf<String, DomainRule>()

    /** 内置条目的启停覆写：host -> enabled。 */
    private val builtinOverrides = mutableMapOf<String, Boolean>()

    private var builtinHosts: Set<String> = emptySet()

    private var builtinVersion = 0

    private val mutableSnapshot = MutableStateFlow(RuleSnapshot.Empty)

    override val snapshot: StateFlow<RuleSnapshot> = mutableSnapshot.asStateFlow()

    override suspend fun load() {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                userRules.clear()
                builtinOverrides.clear()
                readPersistedEntries()
                builtinVersion = builtinVersionFile.readIntOrZero()
                publishSnapshot()
            }
        }
    }

    override suspend fun applyBuiltinCatalog(hosts: Set<String>, version: Int) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                builtinHosts = hosts
                builtinVersion = version
                builtinVersionFile.writeTextAtomically(version.toString())
                publishSnapshot()
            }
        }
    }

    override suspend fun upsertUserRule(host: String, action: RuleAction): RuleEditResult {
        val normalized = HostNormalizer.normalizeRule(host)
        if (normalized == null) return RuleEditResult.InvalidHost
        val canonical = if (normalized.isWildcard) "*.${normalized.host}" else normalized.host
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                val existing = userRules[canonical]
                if (existing != null && existing.action == action && existing.enabled) {
                    return@withLock RuleEditResult.AlreadyExists
                }
                userRules[canonical] = DomainRule(
                    host = canonical,
                    action = action,
                    source = RuleSource.USER,
                    enabled = true,
                )
                persistEntries()
                publishSnapshot()
                RuleEditResult.Applied
            }
        }
    }

    override suspend fun removeUserRule(host: String, action: RuleAction): RuleEditResult {
        val normalized = HostNormalizer.normalizeRule(host)
        if (normalized == null) return RuleEditResult.InvalidHost
        val canonical = if (normalized.isWildcard) "*.${normalized.host}" else normalized.host
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                val existing = userRules[canonical]
                if (existing == null || existing.action != action) {
                    return@withLock RuleEditResult.NotFound
                }
                userRules.remove(canonical)
                persistEntries()
                publishSnapshot()
                RuleEditResult.Applied
            }
        }
    }

    override suspend fun setRuleEnabled(
        host: String,
        action: RuleAction,
        source: RuleSource,
        enabled: Boolean,
    ): RuleEditResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            when (source) {
                RuleSource.USER -> {
                    val existing = userRules[host]
                    if (existing == null || existing.action != action) {
                        RuleEditResult.NotFound
                    } else {
                        userRules[host] = existing.copy(enabled = enabled)
                        persistEntries()
                        publishSnapshot()
                        RuleEditResult.Applied
                    }
                }
                RuleSource.BUILTIN -> {
                    if (host !in builtinHosts) {
                        RuleEditResult.NotFound
                    } else {
                        builtinOverrides[host] = enabled
                        persistEntries()
                        publishSnapshot()
                        RuleEditResult.Applied
                    }
                }
                // 关键词规则由 KeywordBlockingPolicy 单独表达，不在这份存储的职责范围内。
                // 应用专属清单同样是只读内置数据（assets/app_ads.txt），没有落在这里的覆写。
                RuleSource.KEYWORD, RuleSource.APP_ADS -> RuleEditResult.NotFound
            }
        }
    }

    private fun readPersistedEntries() {
        if (!rulesFile.exists()) return
        rulesFile.forEachLine { line ->
            val rule = RuleFileCodec.parse(line) ?: return@forEachLine
            when (rule.source) {
                RuleSource.USER -> userRules[rule.host] = rule
                RuleSource.BUILTIN -> builtinOverrides[rule.host] = rule.enabled
                // 关键词规则不落在这个文件里；应用专属清单同样不落这里。
                // 出现即视为脏数据，跳过而不是让它污染内存状态。
                RuleSource.KEYWORD, RuleSource.APP_ADS -> Unit
            }
        }
    }

    private fun persistEntries() {
        val lines = buildList {
            userRules.values.forEach { rule -> add(RuleFileCodec.format(rule)) }
            builtinOverrides.forEach { (host, enabled) ->
                add(
                    RuleFileCodec.format(
                        DomainRule(
                            host = host,
                            action = RuleAction.BLOCK,
                            source = RuleSource.BUILTIN,
                            enabled = enabled,
                        ),
                    ),
                )
            }
        }
        rulesFile.writeLinesAtomically(lines)
    }

    private fun publishSnapshot() {
        val builtinRules = builtinHosts.map { host ->
            DomainRule(
                host = host,
                action = RuleAction.BLOCK,
                source = RuleSource.BUILTIN,
                enabled = builtinOverrides[host] ?: true,
            )
        }
        mutableSnapshot.value = RuleSnapshot(
            rules = userRules.values + builtinRules,
            builtinVersion = builtinVersion,
        )
    }
}
