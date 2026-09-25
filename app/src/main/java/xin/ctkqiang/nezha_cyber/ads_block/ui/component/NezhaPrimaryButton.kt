package xin.ctkqiang.nezha_cyber.ads_block.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme

/** 按下时的收缩比例，与底栏保持同一套触感。 */
private const val PRESSED_SCALE = 0.97f

/**
 * 主操作按钮。
 *
 * 自绘而非使用 Material 的 Button：本工程不依赖 Material。高度固定为 56dp，
 * 高于 48dp 的可点下限；禁用时只改配色，不改变尺寸，避免布局跳动。
 *
 * 本工程 UI 不使用阴影，按钮靠底色与圆角与背景拉开层级。
 */
@Composable
fun NezhaPrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = NezhaTheme.palette
    val shape = RoundedCornerShape(NezhaDimens.primaryButtonCornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale = if (pressed && enabled) PRESSED_SCALE else 1f
    val containerColor = if (enabled) palette.brand else palette.surfaceElevated
    val contentColor = if (enabled) palette.onBrand else palette.textSecondary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(NezhaDimens.primaryButtonHeight)
            .scale(scale)
            .clip(shape)
            .background(containerColor)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text,
            style = NezhaTheme.typography.title.copy(color = contentColor),
        )
    }
}
