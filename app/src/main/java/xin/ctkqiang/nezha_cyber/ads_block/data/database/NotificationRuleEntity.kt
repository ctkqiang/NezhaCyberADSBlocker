package xin.ctkqiang.nezha_cyber.ads_block.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 一条通知拦截规则的持久化形态。
 *
 * `id` 由数据库自增生成，且**按 id 升序读回**：规则引擎在多条规则同时命中时取第一条，
 * 顺序不稳定会让同一段通知文字今天被这条规则拦、明天被那条拦。
 *
 * `package_name` 上有索引：规则引擎按包名取候选，通知来一条就要查一次，这是热路径。
 *
 * 这里**只存规则本身**，不存任何通知内容。通知正文可能包含私信、验证码这类信息，
 * 持久化它们不属于这个功能的必要代价（工程规则第 20 节）。
 */
@Entity(
    tableName = "notification_rules",
    indices = [Index(value = ["package_name"])],
)
internal data class NotificationRuleEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "match_text") val matchText: String,
    @ColumnInfo(name = "enabled") val enabled: Boolean,
)
