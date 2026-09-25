package xin.ctkqiang.nezha_cyber.ads_block.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * 液态玻璃的用色。
 *
 * 收成一个值而不是让调用点逐个传色：它们必须**成套变化**。单独调其中一层会立刻破坏层次——
 * 例如只加深底色，内缘折射就会在同一块玻璃里显得刺眼；只提亮高光又会让玻璃看起来像塑料。
 *
 * 三层与绘制顺序一一对应（见 `liquidGlass`）：[fill] 是玻璃底，[highlight] 同时用于体内高光与
 * 内缘折射，[edge] 是轮廓细边。体内底阴影已随阴影一起移除。
 *
 * **不透明度是刻意的折中**：真正的液态玻璃很透明，但透明度越高，落在玻璃上的文字对比度就越
 * 取决于背后是什么内容。Apple 用自适应染色解决这件事，我们没有那套能力，因此把底色留在
 * 半透明区间——能看见模糊后的内容，同时保证文字在明暗两套主题下都读得清。
 */
@Immutable
data class LiquidGlassColors(
    /** 玻璃底色。半透明，让下方模糊后的内容透出。 */
    val fill: Color,
    /** 高光。体内自上而下的柔光与内缘折射都用它。 */
    val highlight: Color,
    /** 轮廓细边。给玻璃一个可辨认的边界，通常是很淡的中性色。 */
    val edge: Color,
)
