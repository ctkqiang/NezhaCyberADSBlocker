package xin.ctkqiang.nezha_cyber.ads_block.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme

/**
 * 内容卡片。
 *
 * 本工程 UI 不使用阴影，卡片靠圆角与底色与背景拉开层级，而不是描边或投影。
 *
 * 玻璃**只用于悬浮在内容之上的部件**（底栏、将来的浮层控件），卡片不用玻璃：卡片是内容本身，
 * 它底下没有值得借景的东西，做成玻璃只会白白多花一次绘制。这也是 Apple 的分工——
 * Liquid Glass 属于界面层，不属于内容层。
 *
 * 组件只负责容器语义，内部排版交给调用方，因此不做任何固定的标题或间距假设。
 */
@Composable
fun NezhaSurfaceCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val palette = NezhaTheme.palette
    val shape = RoundedCornerShape(NezhaDimens.cardCornerRadius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.surface)
            .padding(NezhaDimens.cardPadding),
        content = content,
    )
}
