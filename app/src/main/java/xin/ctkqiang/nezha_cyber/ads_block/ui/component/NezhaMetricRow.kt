package xin.ctkqiang.nezha_cyber.ads_block.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme

/**
 * 「名称 —— 数值」的读数行。
 *
 * 统计与设置两页都是成组的读数，统一用它排版，避免每处各写一套对齐方式导致同一页里
 * 数字左右不齐。数值用品牌色而不是正文色，是为了让「读数」在一眼扫过时先被看到。
 *
 * [highlight] 为 false 时数值用次要面色：0 值或者不确定的值不应当被强调，
 * 强调它们等于在暗示「这里有值得注意的事情」。
 */
@Composable
fun NezhaMetricRow(label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = true) {
    val palette = NezhaTheme.palette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = NezhaDimens.minTouchTarget),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = label,
            style = NezhaTheme.typography.body.copy(color = palette.textSecondary),
        )
        BasicText(
            text = value,
            style = NezhaTheme.typography.title.copy(
                color = if (highlight) palette.brand else palette.textSecondary,
            ),
        )
    }
}
