package xin.ctkqiang.nezha_cyber.ads_block.ui.application

import androidx.compose.ui.graphics.ImageBitmap

/**
 * 应用图标的来源。
 *
 * 图标是平台资源，按工程规则第 41.8 节既不事件溯源也不进入领域模型，因此这个端口属于**界面层**
 * （与 `ui.analysis.ApkPicker` 同类：都是界面需要、但实现必须留在应用模块的能力），
 * 而不是 domain 端口。
 *
 * 返回 [ImageBitmap] 而不是 `Drawable`：Compose 无法直接绘制 Drawable，若把平台类型往上抛，
 * 「解码与缓存」就会落到 Composable 里，而界面不该做 I/O（第 40.3 节）。
 *
 * 取不到图标时返回 null（应用已卸载、包不可见、系统没有该应用的图标资源）。界面必须显示占位图形，
 * 不能拿一个通用图标冒充该应用自己的图标——那会让用户认错应用。
 */
fun interface ApplicationIconLoader {
    suspend fun load(packageName: String): ImageBitmap?
}
