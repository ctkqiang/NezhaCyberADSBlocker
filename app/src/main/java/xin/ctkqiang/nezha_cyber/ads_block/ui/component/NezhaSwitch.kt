package xin.ctkqiang.nezha_cyber.ads_block.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaThemePreview

/**
 * 自绘开关。
 *
 * 不使用 Material 的 Switch：本项目的视觉基调由自己的令牌决定，而开关的轨道色、滑块色与
 * 动画时长都属于视觉范畴，交给 Material 会带进一套独立的配色与动效，最终两边都要维护。
 *
 * 交互语义按开关而非复选框声明（`Role.Switch`），无障碍服务因此能正确朗读「开 / 关」。
 * 外层容器固定为 48dp 高，轨道居中：轨道本身只有 30dp，直接承担点击会低于可点面积下限。
 */
@Composable
fun NezhaSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val palette = NezhaTheme.palette
    val trackShape = RoundedCornerShape(NezhaDimens.switchTrackHeight / 2)
    val trackColor by animateColorAsState(
        targetValue = if (checked) palette.brand else palette.surfaceElevated,
        label = "nezhaSwitchTrack",
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) palette.onBrand else palette.textSecondary,
        label = "nezhaSwitchThumb",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) NezhaDimens.switchThumbTravel else 0.dp,
        label = "nezhaSwitchThumbOffset",
    )
    Box(
        modifier = modifier
            .size(width = NezhaDimens.switchTrackWidth, height = NezhaDimens.minTouchTarget)
            .toggleable(
                value = checked,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = NezhaDimens.switchTrackWidth, height = NezhaDimens.switchTrackHeight)
                .clip(trackShape)
                .background(trackColor)
                .border(NezhaDimens.hairline, palette.outline, trackShape)
                .padding(horizontal = NezhaDimens.switchThumbPadding),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(NezhaDimens.switchThumbSize)
                    .clip(CircleShape)
                    .background(thumbColor),
            )
        }
    }
}

@Preview(name = "开关 · 明暗对照", showBackground = true, widthDp = 320, heightDp = 120)
@Composable
private fun NezhaSwitchPreview() {
    NezhaThemePreview {
        Box(modifier = Modifier.padding(NezhaDimens.blockGap)) {
            NezhaSwitch(checked = true, onCheckedChange = {})
        }
    }
}
