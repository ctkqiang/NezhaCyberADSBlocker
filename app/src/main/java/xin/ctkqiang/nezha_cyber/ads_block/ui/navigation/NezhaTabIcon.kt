// 图形的几何坐标天然是数值。把它们抽成具名常量后，读者反而需要在常量与视图框之间来回对照，
// 可读性更差；因此本文件豁免 MagicNumber，其余规则照常生效。
@file:Suppress("MagicNumber")

package xin.ctkqiang.nezha_cyber.ads_block.ui.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale

/**
 * 图标坐标系边长。所有笔画都按 24×24 的正方形设计，再等比缩放到实际尺寸。
 *
 * 五个图标共用同一条不变式：**光学包围盒必须是以 (12, 12) 为中心的 16×16 正方形**（即 4..20）。
 * 手绘图标最容易出的问题不是画错，而是各自大小与重心不同——在底栏里并排显示时，
 * 某几个会显得又小又偏。描边会在路径外侧再扩张半个线宽，因此路径取 4..20、
 * 线宽 1.8 时，实际光学边界正好是 3.1..20.9，重心仍在 (12, 12)。
 *
 * 新增图标前先按这条不变式量一遍包围盒，不要凭手感写坐标。
 */
private const val GLYPH_VIEWPORT = 24f

private const val GLYPH_OPTICAL_MIN = 4f

private const val GLYPH_OPTICAL_MAX = 20f

private const val GLYPH_CENTER = 12f

/** 五个图标共用同一个线宽，避免某一枚因笔画更粗而显得更重。 */
private const val GLYPH_STROKE_WIDTH = 1.8f

/**
 * 自绘标签页图标。
 *
 * 不使用 Material 图标库，也不为五个图标各自建一份矢量资源：它们的几何形状足够简单，
 * 直接按 24×24 坐标系描边绘制，既省掉资源文件，也便于随主题着色。
 *
 * 图标是装饰性的：底栏每一项都已经有文字标签，因此不提供 contentDescription，
 * 无障碍信息由承载它的可选中项表达。
 */
@Composable
fun NezhaTabIcon(tab: NezhaTab, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        // 设计坐标是 24 单位的正方形，而 Canvas 的 size 是像素：把单位空间等比放大到画布。
        //
        // 变换轴必须是原点。以画布中心为轴会把 (12,12) 这类设计坐标当成像素参与运算：
        // (12,12) → center + ((12,12) - center) × factor，在 density 3 下得到 (-36,-36)，
        // 整枚图标被推到画布之外，真机上完全看不见（density 1 时 factor=1、变换恒等，反而正常）。
        val factor = size.minDimension / GLYPH_VIEWPORT
        scale(scale = factor, pivot = Offset.Zero) {
            drawTabGlyph(tab = tab, tint = tint)
        }
    }
}

private fun DrawScope.drawTabGlyph(tab: NezhaTab, tint: Color) {
    val stroke = Stroke(
        width = GLYPH_STROKE_WIDTH,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round,
    )
    when (tab) {
        NezhaTab.Home -> drawHomeGlyph(tint, stroke)
        NezhaTab.Applications -> drawApplicationsGlyph(tint, stroke)
        NezhaTab.Rules -> drawRulesGlyph(tint, stroke)
        NezhaTab.Statistics -> drawStatisticsGlyph(tint)
        NezhaTab.Settings -> drawSettingsGlyph(tint, stroke)
    }
}

/** 屋顶 + 墙体。屋脊在 y=4，墙体下沿在 y=20。 */
private fun DrawScope.drawHomeGlyph(tint: Color, stroke: Stroke) {
    val roof = Path().apply {
        moveTo(GLYPH_OPTICAL_MIN, 11.2f)
        lineTo(GLYPH_CENTER, GLYPH_OPTICAL_MIN)
        lineTo(GLYPH_OPTICAL_MAX, 11.2f)
    }
    drawPath(roof, tint, style = stroke)
    val walls = Path().apply {
        moveTo(6.6f, 10.4f)
        lineTo(6.6f, GLYPH_OPTICAL_MAX)
        lineTo(17.4f, GLYPH_OPTICAL_MAX)
        lineTo(17.4f, 10.4f)
    }
    drawPath(walls, tint, style = stroke)
}

/** 2×2 圆角方块网格。边长 7 加间隙 2，正好铺满 4..20。 */
private fun DrawScope.drawApplicationsGlyph(tint: Color, stroke: Stroke) {
    val side = 7f
    val farEdge = GLYPH_OPTICAL_MIN + side + 2f
    val corners = listOf(
        Offset(GLYPH_OPTICAL_MIN, GLYPH_OPTICAL_MIN),
        Offset(farEdge, GLYPH_OPTICAL_MIN),
        Offset(GLYPH_OPTICAL_MIN, farEdge),
        Offset(farEdge, farEdge),
    )
    corners.forEach { topLeft ->
        drawRoundRect(
            color = tint,
            topLeft = topLeft,
            size = Size(side, side),
            cornerRadius = CornerRadius(2.2f, 2.2f),
            style = stroke,
        )
    }
}

/** 漏斗。上沿撑满 4..20，收口到中间的把手。 */
private fun DrawScope.drawRulesGlyph(tint: Color, stroke: Stroke) {
    val funnel = Path().apply {
        moveTo(GLYPH_OPTICAL_MIN, GLYPH_OPTICAL_MIN)
        lineTo(GLYPH_OPTICAL_MAX, GLYPH_OPTICAL_MIN)
        lineTo(14.4f, 11.8f)
        lineTo(14.4f, GLYPH_OPTICAL_MAX)
        lineTo(9.6f, 16.8f)
        lineTo(9.6f, 11.8f)
        close()
    }
    drawPath(funnel, tint, style = stroke)
}

/**
 * 三根柱子。
 *
 * 圆头笔帽会在端点外再延伸半个线宽，所以端点取 4.9 / 19.1，实际光学边界才是 4..20。
 */
private fun DrawScope.drawStatisticsGlyph(tint: Color) {
    val baseline = 19.1f
    val columns = listOf(
        Offset(4.9f, 11.6f),
        Offset(GLYPH_CENTER, 4.9f),
        Offset(19.1f, 15.4f),
    )
    columns.forEach { top ->
        drawLine(
            color = tint,
            start = Offset(top.x, top.y),
            end = Offset(top.x, baseline),
            strokeWidth = GLYPH_STROKE_WIDTH,
            cap = StrokeCap.Round,
        )
    }
}

/** 两条滑轨。旋钮的圆环半径 2.8，因此轨心取 y=7 与 y=17，光学边界为 4.2..19.8。 */
private fun DrawScope.drawSettingsGlyph(tint: Color, stroke: Stroke) {
    val knobRadius = 2.8f
    val rows = listOf(
        Offset(14.8f, 7f),
        Offset(9.2f, 17f),
    )
    rows.forEach { knob ->
        drawLine(
            color = tint,
            start = Offset(GLYPH_OPTICAL_MIN, knob.y),
            end = Offset(GLYPH_OPTICAL_MAX, knob.y),
            strokeWidth = GLYPH_STROKE_WIDTH,
            cap = StrokeCap.Round,
        )
        drawCircle(color = tint, radius = knobRadius, center = knob, style = stroke)
    }
}
