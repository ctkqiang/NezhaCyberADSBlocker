package xin.ctkqiang.nezha_cyber.ads_block.domain.notification

import kotlinx.coroutines.flow.StateFlow

/**
 * 端口：通知拦截规则的持久化。
 *
 * 实现负责持久化技术与线程调度；domain 不需要知道底层是 Room 还是内存假实现（第 38.2 节）。
 *
 * [rules] 是冷启动后由 [load] 填满的快照流，按 id 升序。**升序是契约的一部分**：
 * 引擎在多条规则同时命中时取第一条，顺序不稳定会让同一段文字今天被拦、明天被放行。
 *
 * 所有写操作都返回 [NotificationRuleEditResult] 而不是抛异常：这些失败都在预期之内。
 */
interface NotificationRuleStore {
    val rules: StateFlow<List<NotificationRule>>

    suspend fun load()

    suspend fun create(packageName: String, matchText: String): NotificationRuleEditResult

    suspend fun update(id: NotificationRuleId, packageName: String, matchText: String): NotificationRuleEditResult

    suspend fun setEnabled(id: NotificationRuleId, enabled: Boolean): NotificationRuleEditResult

    suspend fun delete(id: NotificationRuleId): NotificationRuleEditResult
}
