package xin.ctkqiang.nezha_cyber.ads_block.feature.network

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 网络活动页筛选逻辑的行为测试。
 *
 * 锁住的是三个条件可以叠加、以及「全部应用」与「未知来源」是两件不同的事。
 * 后者一旦混为一谈，用户就永远筛不出那些归属不到应用的记录。
 */
class NetworkActivityUiStateTest {
    private val state = NetworkActivityUiState(
        rows = listOf(
            row(index = 1, host = "ads.example.com", blocked = true, packageName = BROWSER),
            row(index = 2, host = "cdn.example.net", blocked = false, packageName = BROWSER),
            row(index = 3, host = "adservice.example.org", blocked = true, packageName = VIDEO),
            row(index = 4, host = "metrics.example.io", blocked = false, packageName = null),
        ),
        applicationOptions = listOf(
            ApplicationOption(packageName = BROWSER, label = "示例浏览器"),
            ApplicationOption(packageName = VIDEO, label = "示例视频"),
            ApplicationOption(packageName = null, label = null),
        ),
        isLoading = false,
    )

    @Test
    fun `无筛选时显示全部行且不标记为已筛选`() {
        assertEquals(4, state.visibleRows.size)
        assertFalse(state.isFiltered)
    }

    @Test
    fun `只看拦截只留下被拦的行`() {
        val filtered = state.copy(blockedOnly = true)

        assertEquals(listOf("ads.example.com", "adservice.example.org"), filtered.visibleRows.map { it.host })
        assertTrue(filtered.isFiltered)
    }

    @Test
    fun `按应用筛选只留下该应用的行`() {
        val filtered = state.copy(applicationFilter = ApplicationFilter.Source(BROWSER))

        assertEquals(listOf("ads.example.com", "cdn.example.net"), filtered.visibleRows.map { it.host })
    }

    @Test
    fun `未知来源是单独一组而不是全部应用`() {
        val filtered = state.copy(applicationFilter = ApplicationFilter.Source(null))

        assertEquals(listOf("metrics.example.io"), filtered.visibleRows.map { it.host })
    }

    @Test
    fun `域名筛选忽略大小写与首尾空白`() {
        // 「ads」同时出现在 ads.example.com 与 adservice.example.org 里，因此命中两行。
        val matched = state.copy(query = "ADS").visibleRows.map { row -> row.host }

        assertEquals(listOf("ads.example.com", "adservice.example.org"), matched)
        assertEquals(2, state.copy(query = "  ads  ").visibleRows.size)
    }

    @Test
    fun `三个条件同时生效`() {
        val filtered = state.copy(
            blockedOnly = true,
            applicationFilter = ApplicationFilter.Source(BROWSER),
            query = "example",
        )

        assertEquals(listOf("ads.example.com"), filtered.visibleRows.map { it.host })
    }

    @Test
    fun `组合条件无交集时显示空列表但仍是已筛选`() {
        val filtered = state.copy(
            blockedOnly = true,
            applicationFilter = ApplicationFilter.Source(VIDEO),
            query = "cdn.",
        )

        assertTrue(filtered.visibleRows.isEmpty())
        assertTrue(filtered.isFiltered)
    }

    @Test
    fun `只有空白字符的搜索词不算筛选条件`() {
        assertFalse(state.copy(query = "   ").isFiltered)
    }

    private fun row(index: Int, host: String, blocked: Boolean, packageName: String?) = NetworkActivityRow(
        id = "row-$index",
        at = Instant.parse("2026-09-24T10:15:30Z"),
        host = host,
        appLabel = packageName,
        blocked = blocked,
        matchedRule = null,
        source = null,
        packageName = packageName,
    )

    private companion object {
        const val BROWSER = "com.example.browser"

        const val VIDEO = "com.example.video"
    }
}
