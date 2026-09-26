package xin.ctkqiang.nezha_cyber.ads_block.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import xin.ctkqiang.nezha_cyber.ads_block.R
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaSurfaceCard
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme

private val barThickness = 8.dp

private val headlineSize = 34.sp

/** 权重不能取 0，Compose 会直接抛异常；留一个极小值表示「几乎没有」。 */
private const val MIN_WEIGHT = 0.001f

private const val FULL_WEIGHT = 1f

/**
 * 首页的拦截数据面板。
 *
 * 首页此前的两张卡都只有文字，看不出「到底干了多少活」。这里把最能说明问题的三个数放在最上面：
 * 累计拦截（大字、品牌色）、累计观测、其中已放行。
 *
 * 三处刻意的处理：
 * - **大字只给「已拦截」**。三个数一样大等于没有重点，而用户打开这个应用首先想知道的就是它拦住了多少。
 * - **橙色条按「已拦截 ÷ 已观测」量**，且没有观测时画成空条而不是满条——满条会被读成「全都拦住了」，
 *   与「还没有数据」恰好相反。
 * - **口径写在卡里**。这是跨会话累计，不是本次会话；不写清楚，用户会拿它跟刚开保护后的行为对不上。
 */
@Composable
internal fun HomeStatsCard(observed: Long, blocked: Long) {
    val palette = NezhaTheme.palette
    val relayed = (observed - blocked).coerceAtLeast(0L)
    NezhaSurfaceCard {
        BasicText(
            text = stringResource(R.string.home_stats_blocked),
            style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
        )
        BasicText(
            text = countText(blocked),
            style = NezhaTheme.typography.title.copy(
                color = palette.brand,
                fontSize = headlineSize,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        ProportionBar(blocked = blocked, observed = observed)
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        Row(modifier = Modifier.fillMaxWidth()) {
            Metric(
                label = stringResource(R.string.home_stats_observed),
                value = countText(observed),
                modifier = Modifier.weight(FULL_WEIGHT),
            )
            Metric(
                label = stringResource(R.string.home_stats_relayed),
                value = countText(relayed),
                modifier = Modifier.weight(FULL_WEIGHT),
            )
        }
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        BasicText(
            text = stringResource(R.string.home_stats_note),
            style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
        )
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    val palette = NezhaTheme.palette
    Column(modifier = modifier) {
        BasicText(
            text = value,
            style = NezhaTheme.typography.title.copy(color = palette.textPrimary),
        )
        BasicText(
            text = label,
            style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
        )
    }
}

@Composable
private fun ProportionBar(blocked: Long, observed: Long) {
    val palette = NezhaTheme.palette
    val fraction = if (observed <= 0L) 0f else (blocked.toDouble() / observed.toDouble()).toFloat()
    Row(modifier = Modifier.fillMaxWidth().height(barThickness)) {
        Box(
            modifier = Modifier
                .weight(fraction.coerceAtLeast(MIN_WEIGHT))
                .fillMaxHeight()
                .background(palette.brand),
        )
        Box(
            modifier = Modifier
                .weight((FULL_WEIGHT - fraction).coerceAtLeast(MIN_WEIGHT))
                .fillMaxHeight()
                .background(palette.surfaceElevated),
        )
    }
}

private fun countText(value: Long): String = NumberFormat.getIntegerInstance().format(value)
