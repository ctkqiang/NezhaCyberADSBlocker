package xin.ctkqiang.nezha_cyber.ads_block.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 一条关键词兜底规则。
 *
 * 键就是关键词本身，且**只允许单个 DNS 标签**（`ads`、`tracker`），归一化在写入之前完成。
 * 因此表里不会出现 `ads.example.com` 这种带点的值——那属于 [RuleEntity]，
 * 两者混在一起会让「按标签匹配」与「按域名后缀匹配」这两种语义在数据层就分辨不出来。
 */
@Entity(tableName = "keyword_entries")
internal data class KeywordEntity(@PrimaryKey @ColumnInfo(name = "keyword") val keyword: String)
