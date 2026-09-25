package xin.ctkqiang.nezha_cyber.ads_block.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme

/**
 * 自绘分段控件。
 *
 * 同一标签页下的多个页面用它切换，避免为一个二级页面引入完整的导航栈。
 * 选中项用「槽底色 + 高亮滑块」表达层级，没有使用任何 Material 组件。
 *
 * @param items 分段项，顺序即展示顺序。
 * @param selected 当前选中的分段项，按 equals 判定。
 * @param label 分段项的显示文案，交给调用方决定从哪里取（通常是字符串资源）。
 */
@Composable
fun <T> NezhaSegmentedControl(
    items: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = NezhaTheme.palette
    val trackShape = RoundedCornerShape(NezhaDimens.segmentedCornerRadius)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(trackShape)
            .background(palette.surfaceElevated)
            .border(NezhaDimens.hairline, palette.outline, trackShape)
            .padding(NezhaDimens.segmentedTrackPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            NezhaSegment(
                text = label(item),
                selected = item == selected,
                onClick = { onSelect(item) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NezhaSegment(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = NezhaTheme.palette
    val thumbShape = RoundedCornerShape(NezhaDimens.segmentedThumbCornerRadius)
    Box(
        modifier = modifier
            .height(NezhaDimens.segmentedThumbHeight)
            .clip(thumbShape)
            .background(if (selected) palette.surface else Color.Transparent)
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = NezhaTheme.typography.label.copy(
                color = if (selected) palette.textPrimary else palette.textSecondary,
            ),
        )
    }
}
