package xin.ctkqiang.nezha_cyber.ads_block.feature.application

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.ApplicationLabelCache
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.ApplicationPermissionSource
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.InstalledApplicationSource
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.DomainObservation
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.ObservationStore

/** 每个应用最多展示多少个域名。再多就不适合在手机上扫读，也不符合「最近活动」的定位。 */
private const val DOMAIN_LIMIT = 8

/**
 * 应用详情 ViewModel：回答「这个应用是什么、它拿了哪些权限、它在请求哪些域名」。
 *
 * 只针对**一个**应用，而不再是「全部应用按域名分组」——审计的用法是先把可疑的应用挑出来，
 * 再看它到底拿了什么。因此这个 ViewModel 需要一个目标包名，界面的选中项由外壳持有。
 *
 * 应用名与权限只在进入页面时取一次：卸载与授权变化都是低频事件，而每次观测更新都重查一遍
 * 会让 200 条流水的每一次变化都触发两次跨进程调用。
 */
class ApplicationDetailViewModel(
    private val packageName: String?,
    private val observationStore: ObservationStore,
    installedApplicationSource: InstalledApplicationSource,
    private val applicationPermissionSource: ApplicationPermissionSource,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ApplicationDetailUiState(packageName = packageName))

    private val labelCache = ApplicationLabelCache(installedApplicationSource)

    val uiState: StateFlow<ApplicationDetailUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch { loadApplication() }
        viewModelScope.launch {
            observationStore.recent.collect { observations -> publishDomains(observations) }
        }
    }

    /** 没有选中任何应用时直接结束加载：界面据此显示「请先选一个应用」，而不是一直转圈。 */
    private suspend fun loadApplication() {
        val target = packageName
        if (target == null) {
            mutableUiState.update { current -> current.copy(isLoading = false) }
            return
        }
        val labels = labelCache.resolve(setOf(target))
        val permissions = applicationPermissionSource.permissionsOf(setOf(target))[target]
        mutableUiState.update { current ->
            current.copy(
                appLabel = labels[target],
                permissions = permissions.orEmpty(),
                isPermissionDataAvailable = permissions != null,
                isLoading = false,
            )
        }
    }

    /**
     * 该应用请求过的域名。
     *
     * 排序按拦截次数降序：打开这一页通常是想知道「它偷偷在跟谁通信」，
     * 把被拦最多的排最上面比按域名排序有用。
     */
    private fun publishDomains(observations: List<DomainObservation>) {
        val mine = observations.filter { observation -> observation.packageName == packageName }
        val usage = mine.groupBy { observation -> observation.host }
            .map { (host, items) ->
                DomainUsage(
                    host = host,
                    observed = items.size,
                    blocked = items.count { observation -> observation.isBlocked },
                )
            }
            .sortedWith(
                compareByDescending<DomainUsage> { domain -> domain.blocked }
                    .thenByDescending { domain -> domain.observed },
            )
        mutableUiState.update { current ->
            current.copy(
                domains = usage.take(DOMAIN_LIMIT),
                hiddenDomainCount = (usage.size - DOMAIN_LIMIT).coerceAtLeast(0),
                totalObserved = mine.size,
                totalBlocked = mine.count { observation -> observation.isBlocked },
            )
        }
    }

    companion object {
        fun factory(
            packageName: String?,
            observationStore: ObservationStore,
            installedApplicationSource: InstalledApplicationSource,
            applicationPermissionSource: ApplicationPermissionSource,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ApplicationDetailViewModel(
                    packageName = packageName,
                    observationStore = observationStore,
                    installedApplicationSource = installedApplicationSource,
                    applicationPermissionSource = applicationPermissionSource,
                )
            }
        }
    }
}
