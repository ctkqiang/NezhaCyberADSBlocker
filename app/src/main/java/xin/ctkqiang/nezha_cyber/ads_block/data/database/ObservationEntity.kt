package xin.ctkqiang.nezha_cyber.ads_block.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 一次域名观测的持久化形态。
 *
 * 只存元数据，与 [xin.ctkqiang.nezha_cyber.ads_block.domain.observation.DomainObservation] 一一对应，
 * 不额外存任何载荷——这条边界由工程规则第 20 节固定。
 *
 * 索引按「界面会怎么查」建，而不是按「看起来该索引什么」：
 * - `observed_at` 倒序：实时流水与统计都按时间取最新的一段；
 * - `package_name`：按应用筛选、按应用聚合；
 * - `host`：按域名搜索、算高频域名。
 *
 * 动作与来源存枚举名而不是序号：序号会在枚举顺序调整时把历史数据解释成另一个意思，
 * 而名字多占的几十字节在这个量级上不值得为它冒险。
 */
@Entity(
    tableName = "observations",
    indices = [
        Index(value = ["observed_at"]),
        Index(value = ["package_name"]),
        Index(value = ["host"]),
    ],
)
internal data class ObservationEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "observed_at") val observedAt: Long,
    @ColumnInfo(name = "host") val host: String,
    @ColumnInfo(name = "action") val action: String,
    @ColumnInfo(name = "matched_rule") val matchedRule: String?,
    @ColumnInfo(name = "source") val source: String?,
    @ColumnInfo(name = "package_name") val packageName: String?,
)
