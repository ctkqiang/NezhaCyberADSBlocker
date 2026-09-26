package xin.ctkqiang.nezha_cyber.ads_block

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xin.ctkqiang.nezha_cyber.ads_block.network.vpn.AndroidVpnController
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.NezhaDataCompositionRoot
import xin.ctkqiang.nezha_cyber.ads_block.ui.navigation.NezhaNavigationShell
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme

/**
 * 应用唯一的 Activity。
 *
 * 页面切换全部由 Compose 承担，这里只做四件事：开启边到边显示、装配依赖、解析主题偏好、
 * 装载主题与外壳。任何业务逻辑都不放在 Activity 中。
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
        // 无参的 enableEdgeToEdge() 会给**导航栏**套一层半透明遮罩（浅色下约 90% 白、深色下约 50% 黑）。
        // 在导航栏无法真正透明的系统版本上，它会渲染成屏幕底部一条与背景不同的色带。
        // 本应用的背景铺满整个窗口，底部观感完全由自己决定，因此两侧系统栏都显式设为透明。
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        val vpnController = AndroidVpnController(applicationContext)
        val container = requireAppContainer(this)
        setContent {
            val themePreference by container.themePreferenceStore.preference.collectAsStateWithLifecycle()
            NezhaTheme(darkTheme = themePreference.isDark(isSystemInDarkTheme())) {
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
