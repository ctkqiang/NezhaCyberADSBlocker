package xin.ctkqiang.nezha_cyber.ads_block.data.notification

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import xin.ctkqiang.nezha_cyber.ads_block.data.database.NezhaDatabase
import xin.ctkqiang.nezha_cyber.ads_block.data.database.NotificationRuleEntity
import xin.ctkqiang.nezha_cyber.ads_block.data.database.toDomain
import xin.ctkqiang.nezha_cyber.ads_block.domain.notification.NotificationRule
import xin.ctkqiang.nezha_cyber.ads_block.domain.notification.NotificationRuleEditResult
import xin.ctkqiang.nezha_cyber.ads_block.domain.notification.NotificationRuleId
import xin.ctkqiang.nezha_cyber.ads_block.domain.notification.NotificationRuleStore

/**
 * 通知拦截规则的数据库实现。
 *
 * 这是**第一个接入数据库的存储**；其余存储仍在读文本文件，搬过去是既定的后续工作。
 *
 * 每个写操作都在同一把锁里完成「校验 → 写入 → 读回快照」：分开做会让两次并发创建都通过
 * 重复检查，然后各自插入一条相同的规则。锁只在进程内有效，这对本应用足够——数据库只有这一个进程在用。
 *
 * 写入之后立即**读回整张表**而不是就地改内存里的列表：规则数量是个位到几十条，重读的代价可以忽略，
 * 换来的是「内存里的顺序与数据库里的顺序永远一致」——而顺序正是引擎判定结果的一部分。
 *
 * 所有数据库访问都在 IO 调度器上：`onNotificationPosted` 在主线程被调用，通知规则又要在那条路径上读，
 * 任何一次主线程上的磁盘访问都会直接卡住通知栏。
 */
internal class RoomNotificationRuleStore(private val database: NezhaDatabase) : NotificationRuleStore {
    private val mutex = Mutex()

    private val dao = database.notificationRuleDao()

    private val mutableRules = MutableStateFlow<List<NotificationRule>>(emptyList())

    override val rules: StateFlow<List<NotificationRule>> = mutableRules.asStateFlow()

    override suspend fun load() {
        withContext(Dispatchers.IO) {
            mutex.withLock { publishSnapshot() }
        }
    }

    override suspend fun create(packageName: String, matchText: String): NotificationRuleEditResult =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val normalized = matchText.trim()
                when {
                    normalized.isEmpty() -> NotificationRuleEditResult.EmptyMatchText

                    dao.countDuplicates(packageName = packageName, matchText = normalized) > 0 ->
                        NotificationRuleEditResult.Duplicate

                    else -> applyInsert(packageName = packageName, matchText = normalized)
                }
            }
        }

    override suspend fun update(
        id: NotificationRuleId,
        packageName: String,
        matchText: String,
    ): NotificationRuleEditResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            val normalized = matchText.trim()
            when {
                normalized.isEmpty() -> NotificationRuleEditResult.EmptyMatchText

                dao.countDuplicates(
                    packageName = packageName,
                    matchText = normalized,
                    excludeId = id.value,
                ) > 0 -> NotificationRuleEditResult.Duplicate

                dao.update(id = id.value, packageName = packageName, matchText = normalized) == 0 ->
                    NotificationRuleEditResult.NotFound

                else -> applyAndPublish()
            }
        }
    }

    override suspend fun setEnabled(id: NotificationRuleId, enabled: Boolean): NotificationRuleEditResult =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                if (dao.setEnabled(id = id.value, enabled = enabled) == 0) {
                    NotificationRuleEditResult.NotFound
                } else {
                    applyAndPublish()
                }
            }
        }

    override suspend fun delete(id: NotificationRuleId): NotificationRuleEditResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (dao.delete(id = id.value) == 0) {
                NotificationRuleEditResult.NotFound
            } else {
                applyAndPublish()
            }
        }
    }

    private suspend fun applyInsert(packageName: String, matchText: String): NotificationRuleEditResult {
        dao.insert(
            NotificationRuleEntity(packageName = packageName, matchText = matchText, enabled = true),
        )
        publishSnapshot()
        return NotificationRuleEditResult.Applied
    }

    private suspend fun applyAndPublish(): NotificationRuleEditResult {
        publishSnapshot()
        return NotificationRuleEditResult.Applied
    }

    private suspend fun publishSnapshot() {
        mutableRules.value = dao.all().map { entity -> entity.toDomain() }
    }
}
