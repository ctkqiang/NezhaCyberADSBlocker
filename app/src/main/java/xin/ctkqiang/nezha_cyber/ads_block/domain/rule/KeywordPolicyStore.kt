package xin.ctkqiang.nezha_cyber.ads_block.domain.rule

import kotlinx.coroutines.flow.StateFlow

/**
 * 端口：关键词启发式拦截策略的读取与编辑。
 *
 * 关键词规则与域名规则是两种模型：前者是一个开关加一组标签，后者是逐条域名记录。
 * 因此它有自己的存储与编辑入口，不进 [RuleStore]——后者的
 * [RuleStore.setRuleEnabled] 签名是「域名 + 动作 + 来源」，本来就没有位置表达一个开关。
 *
 * 「文件不存在」与「用户清空了全部关键词」是两件事：前者退回默认关键词，
 * 后者是一份**空关键词集合**，必须被如实保存与读出。把后者也当成默认值，
 * 用户就永远无法通过清空来关掉关键词拦截。
 */
interface KeywordPolicyStore {
    /** 当前策略。状态端口，因此用 StateFlow（工程规则第 37.5 节）。 */
    val policy: StateFlow<KeywordBlockingPolicy>

    /** 从持久层载入。文件缺失或首行无法识别时保持默认策略，不阻止应用启动（第 29 节）。 */
    suspend fun load()

    /** 添加一个关键词。只接受单个合法 DNS 标签，重复添加返回 [RuleEditResult.AlreadyExists]。 */
    suspend fun addKeyword(keyword: String): RuleEditResult

    suspend fun removeKeyword(keyword: String): RuleEditResult

    /** 打开或关闭关键词拦截。关掉它不清空关键词，下次打开时原样恢复。 */
    suspend fun setEnabled(enabled: Boolean)
}
