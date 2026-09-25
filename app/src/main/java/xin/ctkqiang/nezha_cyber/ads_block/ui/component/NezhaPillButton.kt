package xin.ctkqiang.nezha_cyber.ads_block.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaPalette
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme

/**
 * 次要动作按钮。
 *
 * 与主按钮的分工：主按钮是全页唯一的主要动作（开启保护），用品牌实心色；
 * 「取消逐应用选择」这类辅助动作用描边胶囊，视觉重量低一档，避免同一页出现两个抢注意力的按钮。
 *
 * [selected] 供筛选类胶囊表达「当前生效」：它借用品牌实心色，与主按钮同色但尺寸小得多，
 * 因此不会被误认成主要动作。筛选是开关语义而不是动作语义，选中态必须能一眼看出来。
 *
 * 视觉高度 36dp，可点高度仍为 48dp：描边胶囊看起来更轻，但触控面积不能跟着缩水。
 */
@Composable
fun NezhaPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
) {
    val palette = NezhaTheme.palette
    val shape = RoundedCornerShape(NezhaDimens.pillButtonCornerRadius)
    val colors = pillColors(palette = palette, enabled = enabled, selected = selected)
    Box(
        modifier = modifier
            .heightIn(min = NezhaDimens.minTouchTarget)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .background(colors.background)
                .border(NezhaDimens.hairline, colors.border, shape)
                .padding(
                    horizontal = NezhaDimens.pillButtonHorizontalPadding,
                    vertical = NezhaDimens.pillButtonVerticalPadding,
                ),
        ) {
            BasicText(
                text = text,
                style = NezhaTheme.typography.label.copy(color = colors.content),
            )
        }
    }
}

/** 胶囊的三处取色。三者必须一起换，拆成三个参数只会让调用点忘掉其中一个。 */
private data class PillColors(val background: Color, val border: Color, val content: Color)

/**
 * 不可用优先于选中：不可点的东西不该看起来是「当前生效」的。
 *
 * 选中态的背景与描边同色，胶囊因此没有可见描边——这是刻意的，实心块加同色描边
 * 会在边缘多出一圈比胶囊本身更暗的线。
 */
private fun pillColors(palette: NezhaPalette, enabled: Boolean, selected: Boolean): PillColors = when {
    !enabled -> PillColors(palette.surfaceElevated, palette.outline, palette.textSecondary)
    selected -> PillColors(palette.brand, palette.brand, palette.onBrand)
    else -> PillColors(palette.surface, palette.outline, palette.textPrimary)
}
