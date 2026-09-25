package xin.ctkqiang.nezha_cyber.ads_block.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme

/**
 * 防护状态环。
 *
 * 首页唯一的强视觉锚点：开启时环体转为品牌色；关闭时环体退为描边色。
 *
 * 环内文字在开启时使用品牌色。它落在比卡片更深的底色上（页面背景而不是卡片面），
 * 明暗两套主题下对底色的对比度分别为 5.2:1 与 5.5:1，均高于 WCAG AA 对正文的要求。
 *
 * 本工程 UI 不使用阴影与光晕，环体仅靠描边色表达状态。
 */
@Composable
fun NezhaProtectionRing(active: Boolean, title: String, subtitle: String, modifier: Modifier = Modifier) {
    val palette = NezhaTheme.palette
    val ringColor = if (active) palette.brand else palette.outline
    val titleColor = if (active) palette.brand else palette.textPrimary
    Box(
        modifier = modifier.size(NezhaDimens.protectionRingSize),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val haloRadius = size.minDimension / 2f
            val strokeWidth = NezhaDimens.protectionRingStroke.toPx()
            val ringRadius = haloRadius - strokeWidth / 2f
            drawCircle(color = palette.background, radius = ringRadius - strokeWidth / 2f)
            drawCircle(color = ringColor, radius = ringRadius, style = Stroke(width = strokeWidth))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(
                text = title,
                style = NezhaTheme.typography.display.copy(color = titleColor),
            )
            Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
            BasicText(
                text = subtitle,
                style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
            )
        }
    }
}
