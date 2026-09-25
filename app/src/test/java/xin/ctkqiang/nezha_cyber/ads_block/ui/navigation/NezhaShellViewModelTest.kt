package xin.ctkqiang.nezha_cyber.ads_block.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 外壳导航状态的测试。
 *
 * 这里断言的是两条对外可见的行为约定：初始落在首页，以及切换标签页时重置到该标签的默认页面。
 * 后者对应 iOS 标签栏「再次点击回到根页面」的交互，属于会被后续改动误伤的行为，必须锁住。
 */
class NezhaShellViewModelTest {
    @Test
    fun `初始状态停在首页标签的默认页面`() {
        val viewModel = NezhaShellViewModel()

        assertEquals(NezhaTab.Home, viewModel.uiState.value.selectedTab)
        assertEquals(NezhaTab.Home.defaultSection, viewModel.uiState.value.selectedSection)
    }

    @Test
    fun `切换标签页时回到该标签的默认页面`() {
        val viewModel = NezhaShellViewModel()

        viewModel.dispatch(NezhaShellUiIntent.SelectSection(NezhaSection.ApkAnalysis))
        viewModel.dispatch(NezhaShellUiIntent.SelectTab(NezhaTab.Rules))

        assertEquals(NezhaTab.Rules, viewModel.uiState.value.selectedTab)
        assertEquals(NezhaSection.Blocklist, viewModel.uiState.value.selectedSection)
    }

    @Test
    fun `切换页面不改变当前标签页`() {
        val viewModel = NezhaShellViewModel()

        viewModel.dispatch(NezhaShellUiIntent.SelectTab(NezhaTab.Rules))
        viewModel.dispatch(NezhaShellUiIntent.SelectSection(NezhaSection.Allowlist))

        assertEquals(NezhaTab.Rules, viewModel.uiState.value.selectedTab)
        assertEquals(NezhaSection.Allowlist, viewModel.uiState.value.selectedSection)
    }
}
