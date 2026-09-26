package xin.ctkqiang.nezha_cyber.ads_block.ui.component

/**
 * 两段式占比：把总量分成「被强调的一段」与「其余」。
 *
 * 刻意只支持两段。三段以上需要为每一段单独配色，而本工程的调色板只有品牌色与中性色两档
 * （规则第 0.2 节的极简要求）；硬凑第三色要么破坏基调，要么让读者分不清哪段是哪段。
 * 需要三段时，正确的做法是拆成两张两段图，而不是往一张图里塞颜色。
 *
 * [centerValue] 与 [centerCaption] 由调用方格式化：环心要显示什么（百分比、总数、比率）
 * 取决于这一张图想说什么，组件不猜。
 */
data class NezhaProportion(
    val highlighted: Long,
    val rest: Long,
    val centerValue: String,
    val centerCaption: String,
    val highlightedLabel: String,
    val restLabel: String,
) {
    val total: Long
        get() = highlighted + rest

    /** 被强调部分占总量的比例，范围 0..1；总量为 0 时返回 0，避免除零。 */
    val highlightedFraction: Float
        get() = if (total <= 0L) 0f else highlighted.toFloat() / total.toFloat()
}
