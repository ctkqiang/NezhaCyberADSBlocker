package xin.ctkqiang.nezha_cyber.ads_block.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

/**
 * 键值设置的读写。
 *
 * 故意没有「一次性读出全部设置」的方法：每一项设置的默认值与容错规则都不一样，
 * 让调用方拿到一个裸的字符串映射，等于把「解析与兜底」的责任推给每个读取方，
 * 迟早出现某处忘了兜底而把默认值当成用户选择。逐项读取，各自解析。
 */
@Dao
internal interface SettingDao {
    @Query("SELECT value FROM settings WHERE key = :key")
    suspend fun value(key: String): String?

    @Upsert
    suspend fun upsert(setting: SettingEntity)

    @Query("DELETE FROM settings WHERE key = :key")
    suspend fun delete(key: String)
}
