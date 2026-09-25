package xin.ctkqiang.nezha_cyber.ads_block.domain.application

/**
 * 端口：已安装应用的来源。
 *
 * domain 只依赖这个抽象。`PackageManager`、包可见性配置与线程调度都属于适配器的职责
 * （工程规则第 38.2 节）。
 */
interface InstalledApplicationSource {
    /**
     * 列出用户可识别的应用（带启动入口的应用）。
     *
     * 刻意不返回全部已安装包：系统组件与无界面的库会有数百项，混进来之后逐应用开关就无法使用。
     * 这个范围是刻意的取舍，记在这里而不是留给读者去猜。
     */
    suspend fun listInstalledApplications(): List<InstalledApplication>

    /**
     * 把包名解析成显示名。
     *
     * 观测记录里只有包名，界面要显示「哪个应用」。取不到的包（已卸载、权限不可见）
     * 不会出现在返回值里，由界面显示为未知——不能凭空编一个名字出来。
     */
    suspend fun displayNames(packageNames: Set<String>): Map<String, String>

    /**
     * 本应用是否具备查询所有已安装包的权限。
     *
     * Android 11 起包可见性默认受限：没有 `QUERY_ALL_PACKAGES` 或 `<queries>` 声明时，
     * `listInstalledApplications` 只能返回自动可见的少量应用。列表为空时用这个方法区分
     * 「确实没有应用」与「被权限过滤了」，前者不需要引导，后者必须引导用户授权。
     */
    suspend fun hasPackageVisibility(): Boolean
}
