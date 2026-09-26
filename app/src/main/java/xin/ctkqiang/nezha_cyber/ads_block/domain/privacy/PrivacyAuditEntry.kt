package xin.ctkqiang.nezha_cyber.ads_block.domain.privacy

import xin.ctkqiang.nezha_cyber.ads_block.domain.application.ApplicationPermission
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.InstalledApplication

/**
 * 隐私审计的一行：一个应用**当前已经拿到**的敏感权限。
 *
 * 只统计 `isGranted` 的条目。声明而未授予不算暴露，把它算进来会让这一页从「谁现在能看到我」
 * 退化成「谁想看到我」，两者的处置方式完全不同（工程规则第 32 节）。
 *
 * 一个应用一项敏感权限都没有时，[of] 返回 null 而不是返回一个空条目：
 * 界面上「没有敏感权限」的应用不需要出现在一张以「谁有问题」为目的的列表里。
 */
data class PrivacyAuditEntry(
    val packageName: String,
    val appLabel: String,
    val grantedPermissions: List<ApplicationPermission>,
) {
    /** 已授予的敏感权限条数。它可能与去重后的权限名称数量不同，两者各有用途。 */
    val grantedCount: Int = grantedPermissions.size

    companion object {
        /** 汇总一个应用的暴露面；没有任何已授予的敏感权限时返回 null。 */
        fun of(application: InstalledApplication, permissions: List<ApplicationPermission>): PrivacyAuditEntry? {
            val sensitive = permissions
                .filter { permission ->
                    permission.isGranted && SensitivePermissionCatalog.isSensitive(permission.name)
                }
                .sortedBy { permission -> permission.label }
            return if (sensitive.isEmpty()) {
                null
            } else {
                PrivacyAuditEntry(
                    packageName = application.packageName,
                    appLabel = application.label,
                    grantedPermissions = sensitive,
                )
            }
        }
    }
}
