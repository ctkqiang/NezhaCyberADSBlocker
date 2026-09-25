package xin.ctkqiang.nezha_cyber.ads_block.feature.application

import xin.ctkqiang.nezha_cyber.ads_block.domain.application.ApplicationPermission

/**
 * 应用详情页状态。
 *
 * 本页**只读**，因此刻意没有意图类型（工程规则第 40.1 节要求方法接受意图，而这里没有方法）。
 *
 * 权限不能在这一页修改，也刻意不提供跳转系统设置的入口：前者平台不允许普通应用做，
 * 后者会在用户心里留下「这个应用能改权限」的印象，而它并不能。
 * 两者的理由见 `ApplicationPermissionSource`。
 *
 * [isPermissionDataAvailable] 区分「读到了，该应用确实没有声明权限」与「读不到这个包的权限」。
 * 前者是审计结论，后者是数据缺失，措辞必须不同（第 32 节）。
 */
data class ApplicationDetailUiState(
    val packageName: String? = null,
    val appLabel: String? = null,
    val permissions: List<ApplicationPermission> = emptyList(),
    val isPermissionDataAvailable: Boolean = false,
    val domains: List<DomainUsage> = emptyList(),
    val hiddenDomainCount: Int = 0,
    val totalObserved: Int = 0,
    val totalBlocked: Int = 0,
    val isLoading: Boolean = true,
) {
    /** 运行时权限里已授予的数量。摘要行用它，清单顺序也由它决定。 */
    val dangerousGrantedCount: Int
        get() = permissions.count { permission -> permission.isDangerous && permission.isGranted }

    val dangerousTotalCount: Int
        get() = permissions.count { permission -> permission.isDangerous }
}

/** 该应用请求过的一个域名，以及观测与拦截次数。 */
data class DomainUsage(val host: String, val observed: Int, val blocked: Int)
