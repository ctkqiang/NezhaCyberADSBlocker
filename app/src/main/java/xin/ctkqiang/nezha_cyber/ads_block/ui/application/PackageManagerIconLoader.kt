package xin.ctkqiang.nezha_cyber.ads_block.ui.application

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.drawable.AdaptiveIconDrawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 图标解码后的边长（像素）。96 对 40dp 的显示尺寸在 2 倍密度下仍有富余。 */
private const val ICON_PIXEL_SIZE = 96

/** 缓存的图标数量上限。单个位图约 36KB，256 个约 9MB，符合图片缓存的常见量级。 */
private const val ICON_CACHE_ENTRIES = 256

/**
 * `PackageManager` 图标适配器。
 *
 * 解码必须离开主线程：`getApplicationIcon` 要读 APK 里的资源，几百个应用逐个解码会明显卡顿
 * （工程规则第 22 节）。
 *
 * 缓存只增不减，键的规模是设备上出现过的应用数，有界且很小；重复请求同一个应用时直接命中，
 * 因此列表滚动不会反复解码。
 *
 * 已知限制，显式记录而不是假装不存在：自适应图标按**圆形**掩码绘制，这近似于启动器的默认掩码，
 * 但各家启动器的实际掩码形状不同（方圆、水滴等），因此本应用里的图标与桌面上的形状可能略有差异。
 */
internal class PackageManagerIconLoader(context: Context) : ApplicationIconLoader {
    private val packageManager = context.applicationContext.packageManager

    private val cache = LruCache<String, ImageBitmap>(ICON_CACHE_ENTRIES)

    override suspend fun load(packageName: String): ImageBitmap? {
        val cached = cache.get(packageName)
        if (cached != null) return cached

        val decoded = withContext(Dispatchers.IO) { decode(packageName) }
        if (decoded != null) {
            cache.put(packageName, decoded)
        }
        return decoded
    }

    /**
     * 把图标绘制到固定尺寸的位图上。
     *
     * 应用被卸载或对本应用不可见时返回 null：这是**预期内的正常结果**，契约本来就规定
     * 「取不到返回 null」，异常本身不携带可处置的信息，记录它只会在用户卸载应用时刷出无意义日志。
     */
    @Suppress("SwallowedException")
    private fun decode(packageName: String): ImageBitmap? = try {
        val icon = packageManager.getApplicationIcon(packageName)
        val bitmap = Bitmap.createBitmap(ICON_PIXEL_SIZE, ICON_PIXEL_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (icon is AdaptiveIconDrawable) {
            canvas.clipPath(circlePath())
        }
        icon.setBounds(0, 0, ICON_PIXEL_SIZE, ICON_PIXEL_SIZE)
        icon.draw(canvas)
        bitmap.asImageBitmap()
    } catch (missing: PackageManager.NameNotFoundException) {
        null
    }

    /**
     * 自适应图标的圆形掩码。
     *
     * 自适应图标自带的画布比可见范围大（四周留了安全边距），直接画出来会显得「放大」了，
     * 因此这里按启动器的方式裁一次。传统方形图标不裁：它们的设计本来就以整个方形为边界，
     * 裁圆会切掉内容。
     */
    private fun circlePath(): Path = Path().apply {
        addCircle(
            ICON_PIXEL_SIZE / 2f,
            ICON_PIXEL_SIZE / 2f,
            ICON_PIXEL_SIZE / 2f,
            Path.Direction.CW,
        )
    }
}
