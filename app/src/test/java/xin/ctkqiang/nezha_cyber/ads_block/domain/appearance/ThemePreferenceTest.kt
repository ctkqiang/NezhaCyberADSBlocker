package xin.ctkqiang.nezha_cyber.ads_block.domain.appearance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 主题档位的解析语义。
 *
 * 锁住两件事：**默认档是「跟随系统」**，以及「跟随系统」确实随系统变、另外两档确实不随。
 * 后者看起来显然，但正是它决定了「用户在夜间锁定浅色」这件事能不能成立——
 * 一旦某档偷偷跟着系统走，用户就会觉得设置没生效。
 */
class ThemePreferenceTest {
    @Test
    fun `默认档是跟随系统`() {
        assertEquals(ThemePreference.System, ThemePreference.entries.first())
    }

    @Test
    fun `跟随系统时采用系统明暗`() {
        assertTrue(ThemePreference.System.isDark(systemInDarkTheme = true))
        assertFalse(ThemePreference.System.isDark(systemInDarkTheme = false))
    }

    @Test
    fun `浅色档不随系统变化`() {
        assertFalse(ThemePreference.Light.isDark(systemInDarkTheme = true))
        assertFalse(ThemePreference.Light.isDark(systemInDarkTheme = false))
    }

    @Test
    fun `深色档不随系统变化`() {
        assertTrue(ThemePreference.Dark.isDark(systemInDarkTheme = true))
        assertTrue(ThemePreference.Dark.isDark(systemInDarkTheme = false))
    }
}
