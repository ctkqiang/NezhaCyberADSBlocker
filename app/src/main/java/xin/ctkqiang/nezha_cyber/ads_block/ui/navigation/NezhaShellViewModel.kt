package xin.ctkqiang.nezha_cyber.ads_block.ui.navigation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 外壳 ViewModel。
 *
 * 只负责导航状态，不接触任何 Android 平台类型：页面切换不需要 Context、PackageManager
 * 或 VPN 能力（工程规则第 40.4 节）。
 */
class NezhaShellViewModel : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        NezhaShellUiState(
            selectedTab = NezhaTab.Home,
            selectedSection = NezhaTab.Home.defaultSection,
        ),
    )

    val uiState: StateFlow<NezhaShellUiState> = mutableUiState.asStateFlow()

    fun dispatch(intent: NezhaShellUiIntent) {
        when (intent) {
            is NezhaShellUiIntent.SelectTab -> selectTab(intent.tab)
            is NezhaShellUiIntent.SelectSection -> selectSection(intent.section)
            is NezhaShellUiIntent.ShowApplicationDetail -> showApplicationDetail(intent.packageName)
        }
    }

    private fun selectTab(tab: NezhaTab) {
        mutableUiState.value = NezhaShellUiState(
            selectedTab = tab,
            selectedSection = tab.defaultSection,
        )
    }

    private fun selectSection(section: NezhaSection) {
        mutableUiState.value = mutableUiState.value.copy(selectedSection = section)
    }

    /**
     * 打开某个应用的详情。
     *
     * 同时把标签也切到「应用」：这个意图目前只由应用列表发出，发生跨标签跳转说明调用方搞错了，
     * 而把用户留在当前标签却显示另一个标签的页面会更难排查。
     */
    private fun showApplicationDetail(packageName: String) {
        mutableUiState.value = NezhaShellUiState(
            selectedTab = NezhaTab.Applications,
            selectedSection = NezhaSection.ApplicationDetail,
            selectedPackageName = packageName,
        )
    }
}
