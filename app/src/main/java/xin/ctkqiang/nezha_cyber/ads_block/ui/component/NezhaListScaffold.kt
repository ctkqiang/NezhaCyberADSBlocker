package xin.ctkqiang.nezha_cyber.ads_block.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme

/**
 * 页面内容容器（可能上百条的列表）。
 *
 * 与 [NezhaScreenScaffold] 的区别只在滚动方式：那个是 `verticalScroll`，适合几十条以内的成组内容；
 * 这里是 `LazyColumn`。**两者不能嵌套**——同方向的可滚动容器套在一起会在测量阶段直接抛异常。
 *
 * 底部预留与左右内边距和另一侧共享同一组令牌，因此切换页面时内容的边界不会跳。
 *
 * **表头钉在列表内部，而不是放在列表之外的固定区域。** 放在外面时它不参与滚动，屏幕一变矮
 * （横屏、小屏、或者表头多出两行说明）就会把列表压成 0 高度，而表头被裁掉的部分又滚不到，
 * 成为一个无解的死局。钉在内部则两种屏幕都能用：表头始终可见，列表也永远有空间。
 * 钉住的表头必须自铺底色，否则行会从它下面透出来。
 *
 * [state] 与 [verticalArrangement] 暴露给调用方是为了实时流水：新记录在最前面插入时，
 * 只有拿着滚动状态才能把视口保持在顶部；而流水需要零间距配合自己的发丝分隔线。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NezhaListScaffold(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    header: (@Composable ColumnScope.() -> Unit)? = null,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(NezhaDimens.blockGap),
    content: LazyListScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NezhaTheme.palette.background)
            .padding(horizontal = NezhaDimens.screenHorizontalPadding)
            .navigationBarsPadding(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = state,
            contentPadding = PaddingValues(
                top = NezhaDimens.screenTopPadding,
                bottom = NezhaDimens.contentBottomReserve,
            ),
            verticalArrangement = verticalArrangement,
        ) {
            if (header != null) {
                stickyHeader {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NezhaTheme.palette.background)
                            .padding(bottom = NezhaDimens.blockGap),
                        content = header,
                    )
                }
            }
            content()
        }
    }
}
