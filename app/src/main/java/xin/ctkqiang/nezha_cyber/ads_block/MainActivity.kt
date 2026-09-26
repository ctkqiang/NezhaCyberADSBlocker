package xin.ctkqiang.nezha_cyber.ads_block

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xin.ctkqiang.nezha_cyber.ads_block.network.vpn.AndroidVpnController
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.NezhaDataCompositionRoot
import xin.ctkqiang.nezha_cyber.ads_block.ui.navigation.NezhaNavigationShell
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme
import xin.ctkqiang.nezha_cyber.ads_block.widget.NezhaWidgetProvider

/**
 * 应用唯一的 Activity。
 *
 * 页面切换全部由 Compose 承担，这里只做四件事：装配依赖、把主题与系统栏对齐、开启边到边显示、
 * 装载外壳。任何业务逻辑都不放在 Activity 中。
 *
 * 装配分两层：数据端口来自 Application 持有的容器（与前台服务共享同一份实现），
 * VPN 控制端口与其授权桥由这里构造。两者分开是为了让「谁提供什么」在阅读时不混在一起。
 *
 * 主题在这里解析而不是在 `NezhaTheme` 内部：`NezhaTheme` 只认「深色 / 浅色」，把「跟随系统」
 * 这一档留给偏好值本身。这样那一档的语义是纯函数（`ThemePreference.isDark`），可以单测，
 * 而「系统当前是不是深色」这一平台信息只在最外层读一次。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = requireAppContainer(this)
        val isDarkTheme = container.themePreferenceStore.preference.value.isDark(isSystemInDarkMode())
        // 窗口底色必须在 setContent 之前设：首帧之前，系统栏那两条区域显示的就是它。
        // 只靠 values{,-night} 资源不够——那份按系统明暗取值，而用户可以把主题锁死在另一档。
        window.setBackgroundDrawable(
            ColorDrawable(NezhaTheme.windowBackgroundColor(isDarkTheme).toArgb()),
        )
        applySystemBarAppearance(isDarkTheme)
        val vpnController = AndroidVpnController(applicationContext)
        setContent {
            val themePreference by container.themePreferenceStore.preference.collectAsStateWithLifecycle()
            val isDark = themePreference.isDark(isSystemInDarkTheme())
            // 系统栏样式是固定值，不会自己跟着主题走，因此主题一变就要重新应用一次。
            LaunchedEffect(isDark) { applySystemBarAppearance(isDark) }
            // 会话状态一变就让桌面小组件跟上：小组件每 30 分钟才被系统刷新一次，
            // 光靠那个周期，用户在应用里开了保护、再回到桌面会看到一张说「已停止」的卡。
            // 首次进入组合时也会触发一次，正好把桌面的状态对齐。
            val session by vpnController.session.collectAsStateWithLifecycle()
            LaunchedEffect(session) { NezhaWidgetProvider.refreshAll(this@MainActivity) }
            NezhaTheme(darkTheme = isDark) {
                NezhaDataCompositionRoot(container = container) {
                    VpnCompositionRoot(vpnController = vpnController) {
                        AnalysisCompositionRoot {
                            NotificationCompositionRoot {
                                PrivacyCompositionRoot {
                                    NezhaNavigationShell()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 让系统栏与应用主题对齐。
 *
 * 两件事分开做，各由一个机制负责——少任何一个都会在界面上露出来：
 *
 * 1. **图标明暗**由 [SystemBarStyle] 决定。必须用 [SystemBarStyle.dark] / [SystemBarStyle.light]，
 *    不能用 `auto(...)`：`auto` 按**系统**明暗挑图标颜色，而本应用的主题可以独立于系统
 *    （用户能锁死浅色或深色）。系统深色而应用锁了浅色时，`auto` 会给出浅色图标压在浅色底上。
 * 2. **导航栏不铺遮罩**由 `isNavigationBarContrastEnforced` 决定。API 29 起系统默认给
 *    三键导航的导航栏垫一层半透明色带以保证对比度，那会让底栏与页面底色对不上。
 *    官方文档的原话是：for three-button navigation bar, set
 *    `Window.setNavigationBarContrastEnforced` to false otherwise there will be a translucent scrim applied。
 *
 * 顺带一提，`SystemBarStyle.auto` 的文档本身就写着：在 API 29 及以上它会**忽略传入的颜色**、
 * 改由系统加遮罩；「如果你真的想要自定义颜色，用 `dark` 或 `light`」。这也是上面第 1 条的依据。
 */
private fun ComponentActivity.applySystemBarAppearance(isDarkTheme: Boolean) {
    val style = if (isDarkTheme) {
        SystemBarStyle.dark(Color.TRANSPARENT)
    } else {
        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
    }
    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
    }
}

/**
 * 系统当前是否处于深色。
 *
 * Compose 里用 `isSystemInDarkTheme()`；`onCreate` 不在组合中，只能读配置位。
 * 两处读的是同一件事，因此放在一起并写明用途，避免有人以为其中一个是多余的。
 */
private fun Context.isSystemInDarkMode(): Boolean =
    (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
