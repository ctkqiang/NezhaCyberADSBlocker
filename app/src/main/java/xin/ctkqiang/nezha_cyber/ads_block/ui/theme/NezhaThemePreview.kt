package xin.ctkqiang.nezha_cyber.ads_block.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 明暗对照预览容器。
 *
 * 工程规则第 40.5 节要求每个页面同时提供浅色与深色两套预览。把两种方案并排渲染在同一个
 * @Preview 里，既满足该要求，又避免每个页面重复写两个预览函数。
 */
@Composable
fun NezhaThemePreview(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            NezhaTheme(darkTheme = false) { content() }
        }
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            NezhaTheme(darkTheme = true) { content() }
        }
    }
}
