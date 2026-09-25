package xin.ctkqiang.nezha_cyber.ads_block.domain.rule

import kotlinx.coroutines.flow.StateFlow

/**
 * 端口：规则集合的读取与编辑。
 *
 * 实现负责持久化技术与导入语义；domain 不需要知道底层是文件、数据库还是内存假实现
 * （工程规则第 38.2 节）。
 *
 * 导入约束（第 39.4 节）：以域名为主键 upsert，且必须保留用户的显式规则与启停覆写。
 * 从清单中移除的域名置为停用而不是删除，以免切断既有观测记录与统计的关联。
 */
interface RuleStore {
    /** 当前规则快照。这是状态端口而非数据流，因此用 StateFlow（第 37.5 节）。 */
    val snapshot: StateFlow<RuleSnapshot>

    /** 载入持久化的用户规则与启停覆写。必须在导入内置清单之前调用一次。 */
    suspend fun load()

    /**
     * 应用内置清单。
     *
     * 清单本身留在 assets 中只读，这里只记录已导入的版本号与用户对条目的启停覆写，
     * 不把数万条域名复制进持久层——那会让每次启动都写一遍大文件，而收益为零。
     */
    suspend fun applyBuiltinCatalog(hosts: Set<String>, version: Int)

    /** 添加或启用一条用户规则。 */
    suspend fun upsertUserRule(host: String, action: RuleAction): RuleEditResult

    /** 删除一条用户规则。内置条目只能被停用，不能删除。 */
    suspend fun removeUserRule(host: String, action: RuleAction): RuleEditResult

    /**
     * 启停一条规则（用户规则或内置条目均可）。
     *
     * 关键词规则不接受这个入口：它不是一条域名规则，而是关键词与开关两个值，
     * 由 [KeywordBlockingPolicy] 单独表达。把它塞进 `when` 里只会让两个模型混在一起。
     */
    suspend fun setRuleEnabled(host: String, action: RuleAction, source: RuleSource, enabled: Boolean): RuleEditResult
}
