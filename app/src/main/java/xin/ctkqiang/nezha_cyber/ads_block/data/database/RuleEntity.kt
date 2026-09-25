package xin.ctkqiang.nezha_cyber.ads_block.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 一条规则的持久化形态，对应原先 `rules.txt` 里的一行。
 *
 * 这张表同时承载两类行，与文件格式保持一一对应：
 * 1. **用户规则**（`source = USER`）——用户自己加的放行或阻断；
 * 2. **内置条目的启停覆写**（`action = BLOCK`、`source = BUILTIN`），只保存用户改过的那几条。
 *
 * 内置清单本体**不**写入这张表：七万多条域名的事实来源是 `assets/domains.txt`（第 39.1 节），
 * 抄进数据库只会制造两个会各自漂移的事实来源。因此从清单里移除的域名天然不需要物理删除——
 * 它下次加载就不存在了，而用户的覆写仍留在这张表里（第 39.4 节）。
 *
 * [host] 直接做主键：规则的身份就是归一化后的域名（含 `*.` 前缀）。归一化在写入之前完成，
 * 因此表里不会出现同一个域名的两种写法。
 *
 * 已导入的清单版本号不在这张表里，它是一条标量设置，放在 [SettingEntity]。
 */
@Entity(tableName = "rules")
internal data class RuleEntity(
    @PrimaryKey @ColumnInfo(name = "host") val host: String,
    @ColumnInfo(name = "action") val action: String,
    @ColumnInfo(name = "source") val source: String,
    @ColumnInfo(name = "enabled") val enabled: Boolean,
)
