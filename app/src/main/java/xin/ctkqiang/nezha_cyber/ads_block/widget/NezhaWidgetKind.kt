package xin.ctkqiang.nezha_cyber.ads_block.widget

import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import xin.ctkqiang.nezha_cyber.ads_block.R

/**
 * 桌面小组件的种类。
 *
 * 每一种都必须有独立的 `AppWidgetProvider` 子类与一份 appwidget-provider 元数据：系统按
 * `<receiver>` 识别小组件，十种共用一条 receiver 是平台做不到的。这里把它们集中声明，
 * 是为了让「一共有哪些小组件」一眼可见，并让文案、尺寸与布局三者与种类一一对应——
 * 分散到十个文件里，很容易出现某一种忘了配布局这类只在运行时才暴露的问题。
 *
 * 十种的分工原则是**各回答一个不同的问题**，而不是把同一个数字换个样式摆十遍：
 * 保护开了吗 / 拦了多少 / 占比多少 / 最近拦了什么 / 护了几个应用 / 看了多少流量 /
 * 规则有多少条 / 谁最常被拦 / 是否在变多 / 想直接去哪里。
 *
 * 布局是复用的（[layoutRes] 有重复），不是每种一份：四种「标题 + 大数字」信息结构完全相同，
 * 十份布局只会让改动时要同步的地方变多。代价是系统选择器里的预览长得像，
 * 因此每种都配了 [descriptionRes]，在「小组件说明」里能读到它的用途。
 */
internal enum class NezhaWidgetKind(
    @StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int,
    @LayoutRes val layoutRes: Int,
) {
    ProtectionToggle(
        R.string.widget_protection_toggle_label,
        R.string.widget_protection_toggle_description,
        R.layout.nezha_widget_toggle,
    ),

    BlockedCount(
        R.string.widget_blocked_count_label,
        R.string.widget_blocked_count_description,
        R.layout.nezha_widget_card,
    ),

    BlockedRatio(
        R.string.widget_blocked_ratio_label,
        R.string.widget_blocked_ratio_description,
        R.layout.nezha_widget_ratio,
    ),

    LatestBlocked(
        R.string.widget_latest_blocked_label,
        R.string.widget_latest_blocked_description,
        R.layout.nezha_widget_list,
    ),

    ProtectedApplications(
        R.string.widget_protected_applications_label,
        R.string.widget_protected_applications_description,
        R.layout.nezha_widget_card,
    ),

    ObservedTotal(
        R.string.widget_observed_total_label,
        R.string.widget_observed_total_description,
        R.layout.nezha_widget_card,
    ),

    RuleScale(
        R.string.widget_rule_scale_label,
        R.string.widget_rule_scale_description,
        R.layout.nezha_widget_card,
    ),

    TopBlockedApplications(
        R.string.widget_top_blocked_label,
        R.string.widget_top_blocked_description,
        R.layout.nezha_widget_list,
    ),

    BlockedTrend(
        R.string.widget_blocked_trend_label,
        R.string.widget_blocked_trend_description,
        R.layout.nezha_widget_trend,
    ),

    Overview(
        R.string.widget_overview_label,
        R.string.widget_overview_description,
        R.layout.nezha_widget_overview,
    ),
}
