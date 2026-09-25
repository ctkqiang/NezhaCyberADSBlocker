package xin.ctkqiang.nezha_cyber.ads_block.domain.application

/**
 * 包名到显示名的记忆。
 *
 * 观测列表在每次域名查询之后都会更新，若每次更新都重新解析一遍包名，浏览网页时会变成
 * 每几十毫秒一次跨进程调用。这里把已经解析过的结果留下来。
 *
 * 记忆只增不减：键的规模是设备上出现过的应用数，有界且很小；已卸载应用的旧名字留着也无害，
 * 观测记录本身还会引用它。
 *
 * 本类不是线程安全的，调用方必须始终在同一个调度器上使用它。当前三处调用点都只在
 * `viewModelScope` 上访问，因此不需要额外加锁。
 */
class ApplicationLabelCache(private val source: InstalledApplicationSource) {
    private val resolved = mutableMapOf<String, String>()

    /**
     * 返回能够解析出显示名的包名到名称的映射。
     *
     * 解析不到的名字不会出现在返回值里，由界面显示为未知——不能凭空编一个名字出来
     * （工程规则第 32 节）。
     */
    suspend fun resolve(packageNames: Set<String>): Map<String, String> {
        val unresolved = packageNames - resolved.keys
        if (unresolved.isNotEmpty()) {
            resolved += source.displayNames(unresolved)
        }
        return resolved
    }
}
