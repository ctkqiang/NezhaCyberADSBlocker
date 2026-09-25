package xin.ctkqiang.nezha_cyber.ads_block.ui.application

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap

/**
 * 异步取一个应用的图标。
 *
 * [packageName] 为 null（无法归属到应用的观测）时直接返回 null，不发起查询。
 *
 * 用 `produceState` 而不是在 Composable 里直接调用：加载是挂起操作，必须挂在组合的生命周期上，
 * 离开组合时自动取消。切换应用时 key 变化会重启加载，旧请求随之取消，
 * 因此不会出现「列表滚动后图标串到别的应用上」。
 */
@Composable
fun rememberApplicationIcon(
    packageName: String?,
    loader: ApplicationIconLoader = LocalApplicationIconLoader.current,
): ImageBitmap? {
    val icon by produceState<ImageBitmap?>(initialValue = null, packageName, loader) {
        value = packageName?.let { name -> loader.load(name) }
    }
    return icon
}
