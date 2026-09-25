package xin.ctkqiang.nezha_cyber.ads_block.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme

/**
 * 页面内容容器（少量成组内容）。
 *
 * 与 [NezhaListScaffold] 的区别只在滚动方式：这里是 `verticalScroll`，适合几十条以内的成组内容；
 * 那边是 `LazyColumn`，适合可能上百条的流水。**两者不能嵌套**——同方向的可滚动容器套在一起
 * 会在测量阶段直接抛异常，因此一个页面只能用其中一个。
 *
 * **底部预留写在滚动容器之内**，这是与列表容器保持一致的关键：写在之外它会变成滚动区之外的一
 * 条死白边，内容永远滚不到悬浮底栏下面，而列表页的内容能——同一份数值在两种页面上产生两种
 * 观感，切换页面时底部会「跳」。写在之内，两种容器的行为就完全一致：最后一项都能滚到底栏之下。
 *
 * 系统栏内边距则是**滚动容器之外**的：导航栏属于系统，内容不该滚进它下面。
 */
@Composable
fun NezhaScreenScaffold(
    modifier: Modifier = Modifier,
    bottomReserve: Dp = NezhaDimens.contentBottomReserve,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NezhaTheme.palette.background)
            .padding(horizontal = NezhaDimens.screenHorizontalPadding)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(top = NezhaDimens.screenTopPadding, bottom = bottomReserve),
        content = content,
    )
}
