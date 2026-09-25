package xin.ctkqiang.nezha_cyber.ads_block.feature.rule

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleAction
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.NezhaDataPreviewHost
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaThemePreview

/**
 * 黑名单：用户阻断规则 + 内置清单的搜索与启停。
 *
 * 页面本体与白名单共用 [RuleListContent]，区别只有目标动作。两页都需要的能力
 * （查看、新增、启停、删除、搜索清单）完全一致，分成两份实现只会各自漂移。
 */
@Composable
fun BlocklistScreen(modifier: Modifier = Modifier) {
    RuleListContent(action = RuleAction.BLOCK, modifier = modifier)
}

@Preview(name = "黑名单 · 明暗对照", showBackground = true, widthDp = 800, heightDp = 900)
@Composable
private fun BlocklistScreenPreview() {
    NezhaThemePreview {
        NezhaDataPreviewHost {
            BlocklistScreen()
        }
    }
}
