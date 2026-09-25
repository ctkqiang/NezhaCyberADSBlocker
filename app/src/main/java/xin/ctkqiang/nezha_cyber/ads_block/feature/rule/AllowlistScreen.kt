package xin.ctkqiang.nezha_cyber.ads_block.feature.rule

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleAction
import xin.ctkqiang.nezha_cyber.ads_block.ui.data.NezhaDataPreviewHost
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaThemePreview

/**
 * 白名单：用户放行规则。
 *
 * 放行规则在优先级上高于内置清单与关键词兜底，用途是纠正误拦；因此这一页不提供内置清单，
 * 清单里本来就只有阻断条目，列在这里没有意义。
 */
@Composable
fun AllowlistScreen(modifier: Modifier = Modifier) {
    RuleListContent(action = RuleAction.ALLOW, modifier = modifier)
}

@Preview(name = "白名单 · 明暗对照", showBackground = true, widthDp = 800, heightDp = 900)
@Composable
private fun AllowlistScreenPreview() {
    NezhaThemePreview {
        NezhaDataPreviewHost {
            AllowlistScreen()
        }
    }
}
