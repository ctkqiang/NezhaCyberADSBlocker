package xin.ctkqiang.nezha_cyber.ads_block.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 受保护应用集合中的一个包名。
 *
 * 这是**我们的数据**：托管范围是用户的选择，平台不持有它。
 * 与它相对的是已安装应用清单——那份清单的事实来源是 `PackageManager`，不进数据库
 * （第 41.8 节）。界面要显示应用名与图标时，按包名去平台查，而不是在这里存副本。
 *
 * 空表有明确含义：「接管全部应用」而不是「一个都不接管」。这一点由
 * `ProtectedApplicationStore` 的契约表达，读取方必须区分这两种状态。
 */
@Entity(tableName = "protected_applications")
internal data class ProtectedApplicationEntity(@PrimaryKey @ColumnInfo(name = "package_name") val packageName: String)
