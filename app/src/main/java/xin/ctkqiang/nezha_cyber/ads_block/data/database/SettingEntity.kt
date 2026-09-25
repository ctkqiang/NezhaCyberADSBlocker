package xin.ctkqiang.nezha_cyber.ads_block.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 键值设置。
 *
 * 用键值表而不是「一个设置一列」：设置项会持续增加（主题偏好、保留档位、应答方式、
 * 关键词开关，以及将来的更多项），每加一项都改表结构意味着每次都要写一次迁移。
 * 标量设置没有关系结构，键值表就是它准确的形状。
 *
 * 代价是失去了列级类型约束，因此解析归调用方负责，并且**必须**遵守同一套容错规则：
 * 未知键跳过、缺失或无法解析的取值退回该设置自己的默认值。这与原先文本文件的读法一致，
 * 因此升级不会因为一个坏值把整份配置丢掉。
 *
 * 不存的东西：已安装应用清单、应用图标与标签、VPN 实时状态——它们是平台与内核的事实，
 * 不是我们的数据（第 41.8 节）。
 */
@Entity(tableName = "settings")
internal data class SettingEntity(
    @PrimaryKey @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "value") val value: String,
)
