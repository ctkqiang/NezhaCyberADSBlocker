package xin.ctkqiang.nezha_cyber.ads_block.domain.application

/**
 * 端口：应用声明与持有的权限。
 *
 * **只读，刻意没有写方法。** 平台不允许普通应用修改其它应用的授权状态：能调用
 * `DevicePolicyManager.setPermissionGrantState` 的只有设备所有者、资料所有者，或被授予
 * `DELEGATION_PERMISSION_GRANT` 委托的应用。留一个 `setGranted` 会让界面以为能改，
 * 而真正调用时只会失败——不如让这个能力在类型上就不存在（工程规则第 32 节要求对能力边界透明）。
 *
 * 一次查一批而不是一次查一个：应用列表要为每一行显示危险权限摘要，逐个调用会产生数百次
 * 跨进程调用（第 22 节）。
 */
interface ApplicationPermissionSource {
    /**
     * 返回能够读到权限的应用。
     *
     * 包已卸载或对本应用不可见的应用**不会出现在返回值里**，调用方必须按「缺失即未知」处理，
     * 不能当成「该应用没有申请任何权限」。
     */
    suspend fun permissionsOf(packageNames: Set<String>): Map<String, List<ApplicationPermission>>
}
