package xin.ctkqiang.nezha_cyber.ads_block.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

/**
 * 观测记录的读写。
 *
 * **批量写入而不是逐条写入**：隧道每秒可能产生几十条观测，一条一个事务会把磁盘的同步开销
 * 放大到写入路径上。调用方攒够一小批再调 [appendObservations]，计数与插入在同一个事务里完成，
 * 因此计数与行数不会分叉。
 *
 * 这里**没有**统计聚合查询：统计页要用的分组、时间桶、高频域名等查询在改那张页面时再加。
 * 它们不影响表结构，提前写出来只会是没人调用的死代码。
 */
@Dao
internal interface ObservationDao {
    @Insert
    suspend fun insertBatch(observations: List<ObservationEntity>)

    /** 最新的若干条，供实时流水在启动时把内存窗口填满。 */
    @Query("SELECT * FROM observations ORDER BY observed_at DESC, id DESC LIMIT :limit")
    suspend fun newest(limit: Int): List<ObservationEntity>

    /**
     * 只保留最新的 [keep] 条。
     *
     * 保留量由用户的隐私设置决定（`ObservationRetention`）。调用方按固定间隔调用它，
     * 而不是每插入一条就调一次：这条语句带子查询，放在每一条的写入路径上代价过高。
     */
    @Query(
        "DELETE FROM observations WHERE id NOT IN " +
            "(SELECT id FROM observations ORDER BY observed_at DESC, id DESC LIMIT :keep)",
    )
    suspend fun trimToNewest(keep: Int)

    /** 单行表，主键恒为 [SINGLE_ROW_ID]（0）。 */
    @Query("SELECT * FROM observation_counters WHERE id = 0")
    suspend fun counter(): ObservationCounterEntity?

    @Upsert
    suspend fun upsertCounter(counter: ObservationCounterEntity)

    @Query("DELETE FROM observations")
    suspend fun deleteObservations()

    @Query("DELETE FROM observation_counters")
    suspend fun deleteCounters()

    /**
     * 追加一批观测并同步推进计数。
     *
     * 两件事必须在同一个事务里：分开写会出现「界面上的累计数已经涨了，但记录里查不到对应的行」
     * 这种无法解释的状态。空批次直接返回，否则会白白写一次计数行。
     */
    @Transaction
    suspend fun appendObservations(observations: List<ObservationEntity>, blockedCount: Long) {
        if (observations.isEmpty()) return
        insertBatch(observations)
        val current = counter() ?: ObservationCounterEntity()
        upsertCounter(
            current.copy(
                observed = current.observed + observations.size,
                blocked = current.blocked + blockedCount,
            ),
        )
    }

    /** 清空观测与计数。两者必须一起清，否则「已拦截 0 条但总数还是 1 万」会自相矛盾。 */
    @Transaction
    suspend fun clearAll() {
        deleteObservations()
        deleteCounters()
    }
}
