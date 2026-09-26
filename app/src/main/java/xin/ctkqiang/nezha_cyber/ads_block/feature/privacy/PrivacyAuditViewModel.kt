package xin.ctkqiang.nezha_cyber.ads_block.feature.privacy

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
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.ApplicationPermissionSource
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.InstalledApplicationSource
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.PrivacyAuditEntry

/**
 * 隐私与传感器页的 ViewModel：回答「哪些应用现在拿着敏感权限」。
 *
 * 只做一次全量扫描，不订阅任何流。权限变化来自系统设置，本进程收不到回调；
 * 轮询则会让每个应用每分钟都走一遍跨进程查询。因此这里选择「进页面扫一次 + 用户手动重扫」，
 * 并把「数据是扫描那一刻的」如实写在界面上——比起悄悄显示过期数据的自动刷新，
 * 明确的一次性快照更贴近事实。
 *
 * 扫描是重活：每个应用一次 `getPackageInfo` 加若干次 `getPermissionInfo`，
 * 一百多个应用就是几百次跨进程调用，因此全部跑在 IO 调度器上（适配器内部负责切线程），
 * 且界面在此期间显示「正在扫描」而不是空白。
 */
class PrivacyAuditViewModel(
    private val installedApplicationSource: InstalledApplicationSource,
    private val applicationPermissionSource: ApplicationPermissionSource,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(PrivacyAuditUiState())

    val uiState: StateFlow<PrivacyAuditUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch { scan() }
    }

    fun dispatch(intent: PrivacyAuditUiIntent) {
        when (intent) {
            PrivacyAuditUiIntent.Rescan -> viewModelScope.launch { scan() }
        }
    }

    private suspend fun scan() {
        mutableUiState.update { current -> current.copy(isScanning = true) }
        val applications = installedApplicationSource.listInstalledApplications()
        val packageNames = applications.map { application -> application.packageName }.toSet()
        val permissions = applicationPermissionSource.permissionsOf(packageNames)
        val entries = applications
            .mapNotNull { application ->
                PrivacyAuditEntry.of(application, permissions[application.packageName].orEmpty())
            }
            .map { entry -> entry.toRow() }
            .sortedWith(compareByDescending<PrivacyAuditRow> { row -> row.grantedCount }.thenBy { row -> row.appLabel })
        mutableUiState.update { current ->
            current.copy(
                isScanning = false,
                entries = entries,
                scannedApplicationCount = applications.size,
                unreadableApplicationCount = packageNames.size - permissions.size,
                hasPackageVisibility = installedApplicationSource.hasPackageVisibility(),
            )
        }
    }

    private fun PrivacyAuditEntry.toRow(): PrivacyAuditRow = PrivacyAuditRow(
        packageName = packageName,
        appLabel = appLabel,
        grantedCount = grantedCount,
        permissionLabels = grantedPermissions.map { permission -> permission.label }.distinct(),
    )

    companion object {
        fun factory(
            installedApplicationSource: InstalledApplicationSource,
            applicationPermissionSource: ApplicationPermissionSource,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                PrivacyAuditViewModel(
                    installedApplicationSource = installedApplicationSource,
                    applicationPermissionSource = applicationPermissionSource,
                )
            }
        }
    }
}
