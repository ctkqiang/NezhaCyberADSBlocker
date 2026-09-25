package xin.ctkqiang.nezha_cyber.ads_block.domain.application

import kotlinx.coroutines.flow.StateFlow

/**
 * 端口：受保护应用的集合。
 *
 * 语义必须说清，否则「我明明开了保护却还有广告」无法解释：
 * - **集合为空表示不做逐应用过滤**，也就是接管所有应用的域名解析；
 * - **集合非空表示只接管这些应用**，其余应用完全不经过隧道。
 *
 * 这与工程规则第 6 节一致：用户没有明确配置逐应用过滤时，不实现它。
 */
interface ProtectedApplicationStore {
    val protectedPackages: StateFlow<Set<String>>

    /** 从持久层载入用户的选择。 */
    suspend fun load()

    suspend fun setProtected(packageName: String, isProtected: Boolean)

    /** 清空选择，回到「接管全部应用」。 */
    suspend fun clearSelection()
}
