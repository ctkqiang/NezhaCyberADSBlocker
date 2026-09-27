package xin.ctkqiang.nezha_cyber.ads_block.notification.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import xin.ctkqiang.nezha_cyber.ads_block.R

/**
 * 通知的大图标（large icon）。
 *
 * 为什么需要它：`setSmallIcon` 只接受单色剪影——系统只取 alpha 通道，彩色图会被画成一团白块，
 * 所以状态栏与系统装饰区只能留那个白色盾牌。但在**不支持自定义内容视图**的 ROM（典型是 MIUI）上，
 * 系统回退到标准模板，整条通知里就只剩那枚白盾，浅色背景下几乎看不见，用户会以为通知是空的。
 *
 * `setLargeIcon` 不受单色约束：它接受彩色 Bitmap，在标准模板里显示在内容区左侧。把应用 logo
 * 放进去，即使自定义布局整个失效，用户也能一眼认出这条通知来自哪吒反广。
 *
 * 只加载一次、缓存复用：通知发布频率不低（一次页面加载能拦十几条），每条都去解码 PNG 是浪费。
 * 解码失败时返回 null——`setLargeIcon(null)` 等价于不设，不会让通知构建失败。
 */
internal object NezhaNotificationIcon {

    @DrawableRes
    private val LOGO_RES = R.drawable.ic_launcher_logo

    @Volatile
    private var cached: Bitmap? = null

    fun largeIcon(context: Context): Bitmap? {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: decodeLogo(context).also { decoded -> cached = decoded }
        }
    }

    /**
     * 解码 logo 为通知尺寸的 Bitmap。
     *
     * 原始 logo 是 512px 的方图，直接塞进 largeIcon 会占 1MB 内存。这里按 2x 采样
     * （`inSampleSize = 2` → 256px），既足够清晰，又只占四分之一内存。Bitmap 最终由系统
     * 缩放到通知需要的显示尺寸，这里不需要精确到 dp。
     */
    private fun decodeLogo(context: Context): Bitmap? {
        val options = BitmapFactory.Options().apply { inSampleSize = 2 }
        return BitmapFactory.decodeResource(context.resources, LOGO_RES, options)
    }
}
