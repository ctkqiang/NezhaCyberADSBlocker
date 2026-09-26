package xin.ctkqiang.nezha_cyber.ads_block.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme

/**
 * 图表图例的一个条目：一个色点加一句说明。
 *
 * 图例不是装饰，是图表可读的前提——同一组品牌红与灰在不同图里代表不同的量（拦截 / 放行、
 * 已归因 / 未知来源），没有图例的图只能靠猜。因此每张图都必须配图例（工程规则第 32 节）。
 *
 * [color] 由调用方从主题取，本组件不预设语义色：同一档颜色在不同的图里含义并不相同。
 */
@Composable
fun NezhaChartLegendEntry(color: Color, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(NezhaDimens.chartLegendDotSize)
                .clip(RoundedCornerShape(NezhaDimens.chartLegendDotCornerRadius))
                .background(color),
        )
        Spacer(modifier = Modifier.width(NezhaDimens.tightGap))
        BasicText(
            text = label,
            style = NezhaTheme.typography.label.copy(color = NezhaTheme.palette.textSecondary),
        )
    }
}
