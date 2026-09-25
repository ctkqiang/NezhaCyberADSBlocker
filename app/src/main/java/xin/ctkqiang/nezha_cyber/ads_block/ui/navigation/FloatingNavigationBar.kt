package xin.ctkqiang.nezha_cyber.ads_block.ui.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.LiquidGlassBackdrop
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.LiquidGlassSpec
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.liquidGlass
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.rememberLiquidGlassSpec
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme

/** 按压时图标的收缩比例。底栏的触感主要来自这一点位移，数值刻意保守以保持克制。 */
private const val PRESSED_GLYPH_SCALE = 0.88f

/** 选中胶囊上的高光相对底栏玻璃高光的比例。胶囊底色是不透明的品牌色，高光过强会把颜色洗淡。 */
private const val CAPSULE_HIGHLIGHT_SCALE = 0.5f

/**
 * 悬浮底部导航栏（液态玻璃）。
 *
 * 玻璃由 `liquidGlass` 一次绘成，其中最要紧的一层是**背景取样**：底栏把自己覆盖的那块
 * 内容以模糊方式重画一遍，因此它的背后是真的糊掉了，而图标与文字保持锐利。这正是液态玻璃与
 * 「一层半透明色」的区别。
 *
 * 取样来自 [backdrop]，由外壳传入；在 API 31 以下没有 `RenderEffect`，玻璃会自动换成更不透明的
 * 底色（见 `rememberLiquidGlassSpec`），不会退化成看不清的文字压在滚动背景上。
 *
 * 选中态用品牌色胶囊托住图标、图标转为 `onBrand`、标签转为品牌色。文字始终落在玻璃或品牌底上，
 * 明暗两套主题下均不低于 WCAG AA。
 */
@Composable
fun FloatingNavigationBar(
    selectedTab: NezhaTab,
    onTabSelected: (NezhaTab) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: LiquidGlassBackdrop? = null,
) {
    val palette = NezhaTheme.palette
    val barShape = RoundedCornerShape(NezhaDimens.floatingBarCornerRadius)
    val glassSpec = rememberLiquidGlassSpec(
        shape = barShape,
        backdrop = backdrop,
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NezhaDimens.floatingBarHorizontalMargin)
            .height(NezhaDimens.floatingBarHeight)
            .liquidGlass(glassSpec)
            .padding(horizontal = NezhaDimens.floatingBarInnerPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NezhaTab.entries.forEach { tab ->
            FloatingNavigationItem(
                tab = tab,
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FloatingNavigationItem(
    tab: NezhaTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = NezhaTheme.palette
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val glyphScale by animateFloatAsState(
        targetValue = if (pressed) PRESSED_GLYPH_SCALE else 1f,
        label = "floatingNavigationGlyphScale",
    )
    Column(
        modifier = modifier
            .fillMaxHeight()
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        NavigationGlyphCapsule(tab = tab, selected = selected, glyphScale = glyphScale)
        Spacer(modifier = Modifier.height(NezhaDimens.floatingBarItemGap))
        BasicText(
            text = stringResource(tab.titleRes),
            style = NezhaTheme.typography.label.copy(
                color = if (selected) palette.brand else palette.textSecondary,
            ),
        )
    }
}

/**
 * 图标胶囊。
 *
 * 选中时是一枚品牌色玻璃片，未选中时什么都不画（只留图标本身）。
 *
 * 它**刻意不采样背景**：胶囊躺在底栏玻璃内部，如果再采一次样，取到的会是页面内容而不是底栏，
 * 于是同一块区域内出现两种不同的模糊，反而更假。它的玻璃感来自高光与底阴影两层。
 *
 * 抽成独立组件是因为它是本文件里唯一有绘制层次的部件，与布局无关，混在一起会同时拉长两边。
 * 未选中时返回空修饰符而不是画一层全透明填充：多一层全透明绘制只会让渲染器白跑一遍。
 */
@Composable
private fun NavigationGlyphCapsule(
    tab: NezhaTab,
    selected: Boolean,
    glyphScale: Float,
    modifier: Modifier = Modifier,
) {
    val palette = NezhaTheme.palette
    val capsuleShape = RoundedCornerShape(NezhaDimens.glyphCapsuleCornerRadius)
    val glassSpec = LiquidGlassSpec(
        shape = capsuleShape,
        colors = palette.glass.copy(
            fill = palette.brand,
            highlight = palette.glass.highlight.copy(alpha = palette.glass.highlight.alpha * CAPSULE_HIGHLIGHT_SCALE),
            // 品牌底上的边界由高光承担，再描一圈只会让轮廓变粗。
            edge = Color.Transparent,
        ),
    )
    Box(
        modifier = modifier
            .width(NezhaDimens.glyphCapsuleWidth)
            .height(NezhaDimens.glyphCapsuleHeight)
            .then(if (selected) Modifier.liquidGlass(glassSpec) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        NezhaTabIcon(
            tab = tab,
            tint = if (selected) palette.onBrand else palette.textSecondary,
            modifier = Modifier
                .size(NezhaDimens.glyphSize)
                .scale(glyphScale),
        )
    }
}
