package xin.ctkqiang.nezha_cyber.ads_block.data.application

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import java.text.Collator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.InstalledApplication
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.InstalledApplicationSource

/**
 * `PackageManager` 适配器。
 *
 * 全部查询都在 IO 线程上执行：包管理服务是跨进程调用，几百个应用逐个查标签会明显卡住主线程
 * （工程规则第 22 节）。
 *
 * 列表按系统区域设置排序（中文按拼音、英文按字母），而不是按码点：码点序对中文而言等于乱序，
 * 用户找不到应用就会以为列表没加载出来。
 */
internal class PackageManagerApplicationSource(context: Context) : InstalledApplicationSource {
    private val context = context.applicationContext

    private val packageManager = context.packageManager

    override suspend fun listInstalledApplications(): List<InstalledApplication> = withContext(Dispatchers.IO) {
        queryLauncherActivities()
            .asSequence()
            .mapNotNull { resolveInfo -> resolveInfo.activityInfo?.applicationInfo }
            .distinctBy { applicationInfo -> applicationInfo.packageName }
            .map { applicationInfo ->
                InstalledApplication(
                    packageName = applicationInfo.packageName,
                    label = packageManager.getApplicationLabel(applicationInfo).toString(),
                )
            }
            .sortedWith(compareBy(Collator.getInstance()) { application -> application.label })
            .toList()
    }

    override suspend fun displayNames(packageNames: Set<String>): Map<String, String> = withContext(Dispatchers.IO) {
        packageNames.mapNotNull { packageName ->
            labelOf(packageName)?.let { label -> packageName to label }
        }.toMap()
    }

    /**
     * 包可见性检测。
     *
     * Android 11 起需要 `QUERY_ALL_PACKAGES` 或 `<queries>` 声明才能看到全部应用。
     * 这里检查权限授予状态：返回 false 时界面必须引导用户去系统设置授权，
     * 否则列表为空会被误认为是 bug。
     *
     * 在 Android 10 及以下没有包可见性限制，直接返回 true。
     */
    override suspend fun hasPackageVisibility(): Boolean = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            true
        } else {
            context.checkSelfPermission(Manifest.permission.QUERY_ALL_PACKAGES) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 包名 → 标签。
     *
     * 这里豁免 SwallowedException：应用被卸载或对本应用不可见是**预期内的正常结果**，
     * 异常本身不携带任何可处置的信息，而契约本来就规定「解析不到返回 null」。
     * 记录它只会在用户卸载应用时刷出无意义的日志。
     */
    @Suppress("SwallowedException")
    private fun labelOf(packageName: String): String? = try {
        packageManager.getApplicationLabel(applicationInfoOf(packageName)).toString()
    } catch (missing: PackageManager.NameNotFoundException) {
        null
    }

    private fun applicationInfoOf(packageName: String) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0L))
    } else {
        applicationInfoOfLegacy(packageName)
    }

    @Suppress("DEPRECATION")
    private fun applicationInfoOfLegacy(packageName: String) = packageManager.getApplicationInfo(packageName, 0)

    private fun queryLauncherActivities(): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(launcherIntent(), PackageManager.ResolveInfoFlags.of(0L))
        } else {
            queryLauncherActivitiesLegacy()
        }

    @Suppress("DEPRECATION")
    private fun queryLauncherActivitiesLegacy(): List<ResolveInfo> =
        packageManager.queryIntentActivities(launcherIntent(), 0)

    private fun launcherIntent() = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
}
