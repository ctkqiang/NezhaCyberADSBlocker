package xin.ctkqiang.nezha_cyber.ads_block.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * 受保护应用集合的读写。
 *
 * 插入用「冲突即忽略」，理由与关键词一致：重复选中同一个应用是界面状态与数据库短暂不同步的
 * 正常结果，不该让整个开关失败。
 */
@Dao
internal interface ProtectedApplicationDao {
    @Query("SELECT package_name FROM protected_applications ORDER BY package_name")
    suspend fun all(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(application: ProtectedApplicationEntity)

    @Query("DELETE FROM protected_applications WHERE package_name = :packageName")
    suspend fun delete(packageName: String)

    @Query("DELETE FROM protected_applications")
    suspend fun deleteAll()
}
