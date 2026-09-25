package xin.ctkqiang.nezha_cyber.ads_block.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/** 新建规则时用于「排除自己」的占位 id。自增主键从 1 开始，因此不会有真实行用到它。 */
private const val NO_RULE_ID = -1L

/**
 * 通知拦截规则的读写。
 *
 * 查询一律 `ORDER BY id ASC`：规则的顺序是契约的一部分（引擎在多条规则同时命中时取第一条），
 * 顺序不稳定会让同一段通知文字今天被这条规则拦、明天被那条拦。
 *
 * 改与删都返回受影响行数：调用方据此把「改成功了」与「这条规则已经不存在了」分开，
 * 对应到界面上是两种不同的提示。
 */
@Dao
internal interface NotificationRuleDao {
    @Query("SELECT * FROM notification_rules ORDER BY id ASC")
    suspend fun all(): List<NotificationRuleEntity>

    @Query(
        "SELECT COUNT(*) FROM notification_rules " +
            "WHERE package_name = :packageName AND match_text = :matchText AND id != :excludeId",
    )
    suspend fun countDuplicates(packageName: String, matchText: String, excludeId: Long = NO_RULE_ID): Int

    @Insert
    suspend fun insert(rule: NotificationRuleEntity): Long

    @Query("UPDATE notification_rules SET package_name = :packageName, match_text = :matchText WHERE id = :id")
    suspend fun update(id: Long, packageName: String, matchText: String): Int

    @Query("UPDATE notification_rules SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean): Int

    @Query("DELETE FROM notification_rules WHERE id = :id")
    suspend fun delete(id: Long): Int
}
