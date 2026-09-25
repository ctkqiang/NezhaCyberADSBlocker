package xin.ctkqiang.nezha_cyber.ads_block.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * 关键词条目的读写。
 *
 * 插入用「冲突即忽略」而不是 upsert：关键词没有需要更新的字段，重复添加同一个词是用户的
 * 正常误操作，静默忽略即可，不该因此报错、也不该把它当成不存在的输入。
 */
@Dao
internal interface KeywordDao {
    @Query("SELECT keyword FROM keyword_entries ORDER BY keyword")
    suspend fun all(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(keyword: KeywordEntity)

    @Query("DELETE FROM keyword_entries WHERE keyword = :keyword")
    suspend fun delete(keyword: String)

    @Query("DELETE FROM keyword_entries")
    suspend fun deleteAll()
}
