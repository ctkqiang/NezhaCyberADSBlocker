package xin.ctkqiang.nezha_cyber.ads_block.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme

/**
 * 应用图标。
 *
 * 统一裁成圆形并固定尺寸：几行并排时图标大小不一会让整列看起来歪，而图标本身是各应用
 * 自己提供的，尺寸与形状都不可控。
 *
 * [icon] 为 null 时显示首字占位，而不是一个通用图标——见 [monogram]。
 *
 * 图标是装饰：旁边永远已经有应用名，因此 `contentDescription` 留空，避免读屏把同一个应用
 * 念两遍。
 */
@Composable
fun NezhaAppIcon(
    icon: ImageBitmap?,
    label: String?,
    modifier: Modifier = Modifier,
    size: Dp = NezhaDimens.appIconSize,
) {
    val palette = NezhaTheme.palette
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(palette.surfaceElevated),
        contentAlignment = Alignment.Center,
    ) {
        if (icon == null) {
            BasicText(
                text = monogram(label),
                style = NezhaTheme.typography.label.copy(color = palette.textSecondary),
            )
        } else {
            Image(
                bitmap = icon,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * 取不到图标时的占位字符。
 *
 * 用应用名的首字而不是一个通用图标：首字仍然能把这一行和具体应用对上，通用图标会让所有
 * 取不到图标的应用看起来是同一个。连名字都取不到（未知来源）时显示问号，如实表达
 * 「这里有一个来源，但它不可识别」（工程规则第 32 节）。
 */
private fun monogram(label: String?): String {
    val initial = label?.trim()?.takeIf { text -> text.isNotEmpty() }?.take(1)
    return initial ?: "?"
}
