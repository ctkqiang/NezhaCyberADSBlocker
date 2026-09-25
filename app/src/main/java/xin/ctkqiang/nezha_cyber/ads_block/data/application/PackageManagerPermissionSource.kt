package xin.ctkqiang.nezha_cyber.ads_block.data.application

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.ApplicationPermission
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.ApplicationPermissionSource

/**
 * `PackageManager` 权限适配器。
 *
 * 三个判定依据全部来自平台公开 API，都经过文档核对，不靠猜测：
 * - **声明了哪些**：`PackageInfo.requestedPermissions`（需带 `GET_PERMISSIONS` 标志），
 *   官方说明它包含全部 `uses-permission`，即使未授予或系统不认识；
 * - **此刻是否已授予**：`requestedPermissionsFlags` 的 `REQUESTED_PERMISSION_GRANTED` 位，
 *   官方定义是 "the requested permission is currently granted to the application"；
 * - **是否是运行时权限**：`PermissionInfo.protectionLevel` 的基础级别是否为 `PROTECTION_DANGEROUS`。
 *
 * 只保留**显式声明**的权限：带 `REQUESTED_PERMISSION_IMPLICIT` 的是系统为版本兼容补上的隐式项
 * （应用并没有在清单里写），列出来只会给审计视图添噪音。
 *
 * 权限名称的本地化查询按名称缓存：一台设备上去重后的权限名只有几百个，而应用有一百多个，
 * 不缓存会变成上万次跨进程调用。缓存用并发映射，因为批量查询跑在多线程调度器上。
 */
internal class PackageManagerPermissionSource(context: Context) : ApplicationPermissionSource {
    private val packageManager = context.applicationContext.packageManager

    private val labels = ConcurrentHashMap<String, String>()

    override suspend fun permissionsOf(packageNames: Set<String>): Map<String, List<ApplicationPermission>> =
        withContext(Dispatchers.IO) {
            packageNames.mapNotNull { packageName ->
                readPermissions(packageName)?.let { permissions -> packageName to permissions }
            }.toMap()
        }

    /**
     * 排序把最该被看见的排在最前面：已授予的运行时权限 → 未授予的运行时权限 → 普通权限。
     *
     * 这一页的用途是「它拿到了哪些敏感能力」。若按字母序排，用户得在几十条 INTERNET、VIBRATE
     * 之间自己翻找 CAMERA。
     *
     * 这里豁免 SwallowedException：包已卸载或对本应用不可见是**预期内的正常结果**，
     * 契约规定「缺失即未知」，异常本身不携带任何可处置的信息——它的种类已经说明了原因，
     * 记录它只会在用户卸载应用时刷出无意义的日志。
     */
    @Suppress("SwallowedException")
    private fun readPermissions(packageName: String): List<ApplicationPermission>? = try {
        val info = packageInfoOf(packageName)
        val names = info.requestedPermissions ?: emptyArray()
        val flags = info.requestedPermissionsFlags ?: IntArray(0)
        names.mapIndexedNotNull { index, name ->
            val flag = flags.getOrNull(index)
            if (flag == null || !flag.isExplicitlyRequested()) {
                null
            } else {
                describe(name = name, granted = flag.isGranted())
            }
        }.sortedWith(
            compareByDescending<ApplicationPermission> { permission -> permission.isDangerous && permission.isGranted }
                .thenByDescending { permission -> permission.isDangerous }
                .thenBy { permission -> permission.label },
        )
    } catch (missing: PackageManager.NameNotFoundException) {
        // 包已卸载或对本应用不可见。契约规定「缺失即未知」，返回 null 由调用方跳过这一项。
        null
    }

    private fun describe(name: String, granted: Boolean): ApplicationPermission {
        val info = permissionInfoOf(name)
        return ApplicationPermission(
            name = name,
            label = labelOf(name = name, info = info),
            isDangerous = info != null && info.isDangerous(),
            isGranted = granted,
        )
    }

    /** 权限的本地化名称。系统查不到该权限时退回平台标识，而不是编一个名称出来。 */
    private fun labelOf(name: String, info: PermissionInfo?): String {
        if (info == null) return name
        return labels.getOrPut(name) { info.loadLabel(packageManager).toString() }
    }

    /**
     * 是否是运行时权限。
     *
     * `protectionLevel` 是位域：低字节是基础保护级别，高位是附加标志（例如 `PROTECTION_FLAG_APPOP`）。
     * 必须先按 `PROTECTION_MASK_BASE` 取基础级别再比较，否则任何带附加标志的危险权限都会被漏判。
     *
     * 这两个成员在 API 36 上被标记为废弃，但本工程的 `minSdk` 是 26，它们是唯一能覆盖
     * 全部受支持版本的写法，因此这里保留并显式豁免：换成只在较新版本上可用的接口，
     * 会让权限审计在旧设备上直接失效——而旧设备恰恰更依赖用户自己去看清权限。
     */
    @Suppress("DEPRECATION")
    private fun PermissionInfo.isDangerous(): Boolean =
        (protectionLevel and PermissionInfo.PROTECTION_MASK_BASE) == PermissionInfo.PROTECTION_DANGEROUS

    private fun Int.isExplicitlyRequested(): Boolean = and(PackageInfo.REQUESTED_PERMISSION_IMPLICIT) == 0

    private fun Int.isGranted(): Boolean = and(PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0

    /**
     * 读取包信息。
     *
     * 两个分支是同一件事在 API 33 前后的两种重载，因此合成一个函数：拆成两个函数只会让
     * 「取什么标志」这个唯一重要的信息分散在两处。豁免 DEPRECATION 是不可避免的——
     * 旧重载在 minSdk 26 上仍然必须保留。
     */
    @Suppress("DEPRECATION")
    private fun packageInfoOf(packageName: String): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()),
            )
        } else {
            packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        }

    /**
     * 权限元数据。
     *
     * `getPermissionInfo` 只有一个签名、且从未被废弃：它的 `flags` 参数固定为 0，
     * 因此没有 API 33 前后两套重载的问题，也不需要版本分支。
     *
     * 查不到是**预期内的正常结果**：应用可以声明本机型不存在的权限（例如厂商定制权限被移除）。
     * 异常不携带可处置的信息，契约也规定按「未知」处理，因此这里返回 null 而不是抛出。
     * 这种情况下按普通权限处理并保留平台标识：既不误报为危险权限，也不让这条声明凭空消失。
     */
    @Suppress("SwallowedException")
    private fun permissionInfoOf(name: String): PermissionInfo? = try {
        packageManager.getPermissionInfo(name, 0)
    } catch (unknown: PackageManager.NameNotFoundException) {
        null
    }
}
