package xin.ctkqiang.nezha_cyber.ads_block.ui.component

/**
 * 趋势图的一段（一个时间分桶）。
 *
 * 只保留两个计数，不存时间戳：柱子在图上按数组顺序排列，横轴的含义由调用方的说明文字给出。
 * 把时间放进模型会诱使组件去格式化时间，而「这一段时间跨度是多少」是读数口径问题，
 * 属于页面而不是组件。
 */
data class NezhaTrendBucket(val highlighted: Int, val rest: Int) {
    val total: Int
        get() = highlighted + rest
}
