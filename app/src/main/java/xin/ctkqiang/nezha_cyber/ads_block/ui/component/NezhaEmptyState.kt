package xin.ctkqiang.nezha_cyber.ads_block.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import xin.ctkqiang.nezha_cyber.ads_block.R
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaThemePreview

/**
 * 尚未接入页面的空状态。
 *
 * 这些页面目前没有数据，用一句裸文案代替会显得像没做完。这里统一给出「状态标签 + 说明」的结构：
 * 标签如实标注该功能属于后续阶段，说明写清缺什么，用户看到的是「还没做」而不是「做坏了」。
 *
 * 刻意不写「敬请期待」这类话术：工程规则第 32 节要求对能力边界保持透明，措辞必须能被核对。
 */
@Composable
fun NezhaEmptyState(description: String, modifier: Modifier = Modifier, tag: String? = null) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = NezhaDimens.emptyStateTopGap),
        contentAlignment = Alignment.TopCenter,
    ) {
        NezhaSurfaceCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                EmptyStateTag(text = tag ?: stringResource(R.string.empty_state_tag))
                Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
                BasicText(
                    text = description,
                    style = NezhaTheme.typography.caption.copy(
                        color = NezhaTheme.palette.textSecondary,
                        textAlign = TextAlign.Center,
                    ),
                )
            }
        }
    }
}

/**
 * 状态标签。
 *
 * 用中性面色而不是品牌色：品牌色要么作为 12sp 文字压在品牌浅底上（深色主题只有 3.1:1），
 * 要么反过来把品牌色当底配白字（3.5:1），两种组合都低于 WCAG AA 对正文的要求。
 * 中性面色在明暗两套主题下分别为 5.9:1 与 7.1:1，且不影响卡片本身的品牌存在感。
 */
@Composable
private fun EmptyStateTag(text: String) {
    val palette = NezhaTheme.palette
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(NezhaDimens.tagPillCornerRadius))
            .background(palette.surfaceElevated)
            .padding(
                horizontal = NezhaDimens.tagPillHorizontalPadding,
                vertical = NezhaDimens.tagPillVerticalPadding,
            ),
    ) {
        BasicText(
            text = text,
            style = NezhaTheme.typography.label.copy(color = palette.textSecondary),
        )
    }
}

@Preview(name = "空状态 · 明暗对照", showBackground = true, widthDp = 800, heightDp = 400)
@Composable
private fun NezhaEmptyStatePreview() {
    NezhaThemePreview {
        NezhaScreenScaffold {
            NezhaEmptyState(description = stringResource(R.string.placeholder_application_list))
        }
    }
}
