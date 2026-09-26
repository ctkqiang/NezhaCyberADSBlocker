package xin.ctkqiang.nezha_cyber.ads_block.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 尺寸与圆角令牌。
 *
 * 这些数值不随明暗主题变化，因此不放进 CompositionLocal，直接作为具名常量使用，
 * 以满足工程规则第 42.4 节「禁止魔法数值」。
 *
 * 交互元素按「不小于 48dp 的可点区域」设定：底栏单项高度 = 栏高 - 内边距，主按钮与开关同理。
 * 调整这些数值时先复核这条下限，触控面积不是纯视觉参数。
 */
object NezhaDimens {
    val screenHorizontalPadding = 20.dp
    val screenTopPadding = 20.dp
    val screenBottomGap = 24.dp
    val hairline = 1.dp
    val blockGap = 12.dp
    val sectionGap = 24.dp

    /** 同一行内的双行文字间距：比 [blockGap] 更紧，避免把两行看成两个独立区块。 */
    val tightGap = 4.dp

    /**
     * 页头固定高度（不含状态栏）。
     *
     * 必须固定：单页面标签显示 26sp 标题、多页面标签显示 40dp 分段控件，两者若按内容撑开，
     * 切换标签时内容会上下跳一段。固定高度后两种页头占位一致。
     */
    val headerHeight = 68.dp

    val segmentedTrackPadding = 3.dp
    val segmentedThumbHeight = 34.dp
    val segmentedThumbCornerRadius = 15.dp
    val segmentedCornerRadius = 18.dp

    val floatingBarHeight = 72.dp

    /**
     * 与 [screenHorizontalPadding] 保持一致：底栏左右边缘和页面内容在同一条竖直线上，
     * 两者不等时看起来像是底栏歪了，而不是有意内缩。
     */
    val floatingBarHorizontalMargin = 20.dp
    val floatingBarBottomMargin = 12.dp
    val floatingBarCornerRadius = 36.dp
    val floatingBarInnerPadding = 6.dp
    val floatingBarItemGap = 3.dp

    /**
     * 玻璃外缘柔光预留。
     *
     * 本工程 UI 不使用阴影，柔光已移除，因此该值为 0。保留字段是为了不改动 `contentBottomReserve`
     * 与底栏布局的计算式——将来若重新引入柔光，只需改这一处。
     */
    val glassGlowReserve = 0.dp

    /**
     * 玻璃取样的模糊半径。
     *
     * 只作用于玻璃背后的内容，玻璃自身的图标与文字保持锐利——这正是液态玻璃与「一层半透明色」
     * 的区别所在。24dp 足以把背景文字糊成色块，又不至于让背后的结构完全消失。
     * API 31 以下没有 `RenderEffect`，该值会被降级逻辑忽略。
     */
    val glassBlurRadius = 24.dp

    val glyphSize = 24.dp

    /** 应用图标。列表行用 36dp，实时流水为了容纳更多行用更小的 [appIconCompactSize]。 */
    val appIconSize = 36.dp

    val appIconCompactSize = 22.dp

    /** 选中胶囊只比图标大一点：四周各留 2dp 上下、8dp 左右，贴合而不是罩住。 */
    val glyphCapsuleWidth = 40.dp
    val glyphCapsuleHeight = 28.dp
    val glyphCapsuleCornerRadius = 14.dp

    val emptyStateTopGap = 56.dp
    val tagPillHorizontalPadding = 12.dp
    val tagPillVerticalPadding = 5.dp
    val tagPillCornerRadius = 12.dp

    val protectionRingSize = 188.dp
    val protectionRingStroke = 10.dp

    /**
     * 统计页图表的尺寸令牌。
     *
     * 三张图各自固定高度，而不是按内容撑开：读数在隧道运行时每秒都在变，若高度随数据变化，
     * 整页会在用户眼皮底下不停上下跳。
     */
    val chartBarHeight = 10.dp
    val chartBarCornerRadius = 5.dp
    val chartRowGap = 14.dp
    val chartLegendDotSize = 10.dp
    val chartLegendDotCornerRadius = 5.dp
    val proportionRingSize = 148.dp
    val proportionRingStroke = 14.dp
    val proportionRingGap = 20.dp
    val trendChartHeight = 96.dp
    val trendBarCornerRadius = 3.dp
    val trendBarGap = 4.dp

    /** 列表行内的占比条。比图表里的条更细：它是行内的辅助信息，不该抢走行主体的注意力。 */
    val ratioBarHeight = 6.dp
    val ratioBarCornerRadius = 3.dp

    val primaryButtonHeight = 56.dp
    val primaryButtonCornerRadius = 28.dp

    val pillButtonCornerRadius = 18.dp
    val pillButtonHorizontalPadding = 16.dp
    val pillButtonVerticalPadding = 9.dp

    /** 输入框高度 48dp：既满足可点面积下限，也与开关、胶囊按钮处在同一视觉网格上。 */
    val textFieldMinHeight = 48.dp
    val textFieldCornerRadius = 14.dp
    val textFieldHorizontalPadding = 14.dp

    val cardCornerRadius = 20.dp

    val cardPadding = 18.dp

    /**
     * 可点区域下限。
     *
     * 开关轨道本身只有 30dp 高，直接让它承担点击会低于无障碍指南的 48dp 下限，
     * 因此外层容器固定为这个高度，轨道居中显示。
     */
    val minTouchTarget = 48.dp

    val switchTrackWidth = 52.dp
    val switchTrackHeight = 30.dp
    val switchThumbSize = 24.dp
    val switchThumbPadding = 3.dp

    /** 24dp 滑块在 52dp 轨道内左右各留 3dp 内边距后的可移动距离。 */
    val switchThumbTravel = switchTrackWidth - switchThumbSize - switchThumbPadding * 2

    /** 列表行最小高度：图标与两行文字也能容纳，同时保证可点面积。 */
    val listRowMinHeight = 56.dp

    /**
     * 实时流水的行间距。
     *
     * 比 [cardPadding] 小得多：流水一行只需要「读出三行信息」，不需要卡片那种呼吸空间，
     * 而一屏能多看到几条直接决定它能不能当日志用。
     */
    val traceRowVerticalPadding = 10.dp

    val contentBottomReserve = floatingBarHeight + floatingBarBottomMargin + glassGlowReserve + screenBottomGap
}
