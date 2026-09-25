package xin.ctkqiang.nezha_cyber.ads_block.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

/**
 * 规则的读写。
 *
 * 写入一律用 upsert，而不是「先查再决定插入还是更新」：后者在并发下会退化成两条语句之间的
 * 竞态，而规则的身份就是主键，由数据库保证唯一性比由应用层保证更可靠。
 *
 * 按来源删除是因为同一域名在不同来源下是两条不同的规则：用户可能既有一条自己的规则，
 * 又有一条内置条目的覆写，删掉其中一个不该动另一个。
 */
@Dao
internal interface RuleDao {
    @Query("SELECT * FROM rules")
    suspend fun all(): List<RuleEntity>

    @Query("SELECT * FROM rules WHERE source = :source")
    suspend fun bySource(source: String): List<RuleEntity>

    @Upsert
    suspend fun upsert(rule: RuleEntity)

    @Query("DELETE FROM rules WHERE host = :host AND source = :source")
    suspend fun delete(host: String, source: String)

    /**
     * 改启停状态。
     *
     * 返回受影响的行数而不是 Unit：调用方据此判断「这条规则是否存在」，
     * 从而把「找不到」与「改成功」区分开，与 `RuleStore` 的契约一致。
     */
    @Query("UPDATE rules SET enabled = :enabled WHERE host = :host AND source = :source")
    suspend fun setEnabled(host: String, source: String, enabled: Boolean): Int
}
