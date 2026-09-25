package xin.ctkqiang.nezha_cyber.ads_block.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** 累计计数只有一行，主键固定为这个值。 */
internal const val SINGLE_ROW_ID = 0

/**
 * 累计计数。
 *
 * 为什么不直接从观测表现场 `COUNT(*)`：这一行会被统计页的醒目数字与首页状态环直接读取，
 * 而观测表是高频写入的。每次插入都全表统计等于把磁盘与 CPU 白烧掉，而计数与行数在同一个
 * 事务里一起更新，因此不会分叉。
 *
 * 「被拦截的不同域名数」**不在这里**：它需要一条 `COUNT(DISTINCT host)` 查询，而这类查询
 * 放在写入路径上代价太高。它由统计页按需查询，且口径是「保留窗口内」而不是「本次会话」——
 * 后者在有了持久化之后已经没有意义，进程重启不该让这个数字归零。
 */
@Entity(tableName = "observation_counters")
internal data class ObservationCounterEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Int = SINGLE_ROW_ID,
    @ColumnInfo(name = "observed") val observed: Long = 0L,
    @ColumnInfo(name = "blocked") val blocked: Long = 0L,
)
