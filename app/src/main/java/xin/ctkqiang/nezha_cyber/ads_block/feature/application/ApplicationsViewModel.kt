package xin.ctkqiang.nezha_cyber.ads_block.feature.application

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.ApplicationPermission
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.ApplicationPermissionSource
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.InstalledApplication
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.InstalledApplicationSource
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.ProtectedApplicationStore
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.DomainObservation
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.ObservationStore
import xin.ctkqiang.nezha_cyber.ads_block.domain.vpn.VpnController
import xin.ctkqiang.nezha_cyber.ads_block.domain.vpn.VpnStartResult

private const val EFFECT_BUFFER_CAPACITY = 1

/**
 * 应用列表 ViewModel。
 *
 * 四个数据源（已安装应用、受保护集合、最近观测、权限）各自是独立的流，状态在 [publish] 里统一重建。
 * 比在每个流里各自改一部分状态更难出错：谁也说不清「上一次是谁把 rows 改坏的」，
 * 而集中重建只有一处逻辑。
 *
 * 已安装应用与权限合并在同一个协程里加载：权限要按包名批量查，必须先拿到应用清单，
 * 拆成两个协程只会让「权限查的是哪一批应用」变得含糊。
 *
 * 逐应用开关会改变隧道路由。路由在建立隧道时由 `addAllowedApplication` 固定，运行中改不了，
 * 因此修改选择后必须重建隧道——这一点在界面上如实提示，不隐藏这次短暂中断。
 */
class ApplicationsViewModel(
    private val installedApplicationSource: InstalledApplicationSource,
    private val applicationPermissionSource: ApplicationPermissionSource,
    private val protectedApplicationStore: ProtectedApplicationStore,
    private val observationStore: ObservationStore,
    private val vpnController: VpnController,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ApplicationsUiState())

    private val mutableEffect = MutableSharedFlow<ApplicationsUiEffect>(extraBufferCapacity = EFFECT_BUFFER_CAPACITY)

    private var installedApplications: List<InstalledApplication> = emptyList()

    private var permissions: Map<String, List<ApplicationPermission>> = emptyMap()

    private var protectedPackages: Set<String> = emptySet()

    private var recentObservations: List<DomainObservation> = emptyList()

    val uiState: StateFlow<ApplicationsUiState> = mutableUiState.asStateFlow()

    val effect: SharedFlow<ApplicationsUiEffect> = mutableEffect.asSharedFlow()

    init {
        viewModelScope.launch {
            loadApplications()
        }
        viewModelScope.launch {
            protectedApplicationStore.protectedPackages.collect { packages ->
                protectedPackages = packages
                publish()
            }
        }
        viewModelScope.launch {
            observationStore.recent.collect { observations ->
                recentObservations = observations
                publish()
            }
        }
    }

    fun dispatch(intent: ApplicationsUiIntent) {
        when (intent) {
            is ApplicationsUiIntent.ToggleProtection -> toggleProtection(intent.packageName)
            ApplicationsUiIntent.ClearSelection -> clearSelection()
            is ApplicationsUiIntent.AuthorizationResult -> onAuthorizationResult(intent.granted)
            ApplicationsUiIntent.RetryLoad -> reloadApplications()
            ApplicationsUiIntent.OpenSettings -> mutableEffect.tryEmit(ApplicationsUiEffect.OpenApplicationSettings)
        }
    }

    private fun reloadApplications() {
        viewModelScope.launch {
            mutableUiState.update { current -> current.copy(isLoading = true) }
            loadApplications()
        }
    }

    private suspend fun loadApplications() {
        val applications = installedApplicationSource.listInstalledApplications()
        installedApplications = applications
        permissions = applicationPermissionSource.permissionsOf(
            applications.map { application -> application.packageName }.toSet(),
        )
        val needsVisibility = applications.isEmpty() && !installedApplicationSource.hasPackageVisibility()
        mutableUiState.update { current ->
            current.copy(needsPackageVisibility = needsVisibility)
        }
        publish()
    }

    private fun toggleProtection(packageName: String) {
        viewModelScope.launch {
            protectedApplicationStore.setProtected(packageName, packageName !in protectedPackages)
            restartTunnelIfRunning()
        }
    }

    private fun clearSelection() {
        viewModelScope.launch {
            protectedApplicationStore.clearSelection()
            restartTunnelIfRunning()
        }
    }

    private fun onAuthorizationResult(granted: Boolean) {
        if (!granted) return
        viewModelScope.launch { vpnController.start() }
    }

    private suspend fun restartTunnelIfRunning() {
        if (!vpnController.session.value.isActive) return
        mutableUiState.update { current -> current.copy(isRestarting = true) }
        vpnController.stop()
        val result = vpnController.start()
        mutableUiState.update { current -> current.copy(isRestarting = false) }
        if (result is VpnStartResult.PermissionDenied) {
            mutableEffect.tryEmit(ApplicationsUiEffect.RequestVpnAuthorization)
        }
    }

    private fun publish() {
        val perApplication = recentObservations
            .filter { observation -> observation.packageName != null }
            .groupBy { observation -> observation.packageName.orEmpty() }
            .mapValues { (_, observations) ->
                observations.count() to observations.count { observation -> observation.isBlocked }
            }
        val rows = installedApplications.map { application ->
            val counts = perApplication[application.packageName]
            ApplicationRow(
                packageName = application.packageName,
                label = application.label,
                isProtected = application.packageName in protectedPackages,
                observedCount = counts?.first ?: 0,
                blockedCount = counts?.second ?: 0,
                permissions = permissions[application.packageName].toSummary(),
            )
        }.sortedWith(
            compareByDescending<ApplicationRow> { row -> row.isProtected }
                .thenByDescending { row -> row.blockedCount }
                .thenBy { row -> row.label },
        )
        mutableUiState.update { current ->
            current.copy(
                rows = rows,
                protectedCount = protectedPackages.size,
                isLoading = false,
            )
        }
    }

    /**
     * 把权限清单压成列表页要显示的两个数字。
     *
     * 接收可空类型而不是先判空：**读不到**与**读到了但没有运行时权限**是两件不同的事，
     * 前者必须一路传到界面显示为「不可读」，不能在某一层被悄悄折叠成 0 项。
     */
    private fun List<ApplicationPermission>?.toSummary(): DangerousPermissionSummary? = this?.let { permissions ->
        val dangerous = permissions.filter { permission -> permission.isDangerous }
        DangerousPermissionSummary(
            granted = dangerous.count { permission -> permission.isGranted },
            total = dangerous.size,
        )
    }

    companion object {
        fun factory(
            installedApplicationSource: InstalledApplicationSource,
            applicationPermissionSource: ApplicationPermissionSource,
            protectedApplicationStore: ProtectedApplicationStore,
            observationStore: ObservationStore,
            vpnController: VpnController,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ApplicationsViewModel(
                    installedApplicationSource = installedApplicationSource,
                    applicationPermissionSource = applicationPermissionSource,
                    protectedApplicationStore = protectedApplicationStore,
                    observationStore = observationStore,
                    vpnController = vpnController,
                )
            }
        }
    }
}
