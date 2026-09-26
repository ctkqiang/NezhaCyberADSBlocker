package xin.ctkqiang.nezha_cyber.ads_block.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import xin.ctkqiang.nezha_cyber.ads_block.R
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaPalette
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.darkNezhaPalette
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.lightNezhaPalette

private const val REQUEST_OPEN_APP = 4001

private const val REQUEST_TOGGLE = 4002

private const val PENDING_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

/** 列表卡的三行，与 `nezha_widget_list.xml` 里的三行一一对应。 */
private val LIST_ROWS = listOf(
    intArrayOf(R.id.widget_row_1_label, R.id.widget_row_1_value),
    intArrayOf(R.id.widget_row_2_label, R.id.widget_row_2_value),
    intArrayOf(R.id.widget_row_3_label, R.id.widget_row_3_value),
)

/** 分段条的八段，占比卡与趋势卡共用这一组 id（同一组 id 可以出现在多份布局里）。 */
private val SEGMENTS = intArrayOf(
    R.id.widget_segment_1,
    R.id.widget_segment_2,
    R.id.widget_segment_3,
    R.id.widget_segment_4,
    R.id.widget_segment_5,
    R.id.widget_segment_6,
    R.id.widget_segment_7,
    R.id.widget_segment_8,
)

/**
 * 把快照渲染成 RemoteViews。
 *
 * 配色直接取自 UI 主题的调色板，不另建一套颜色资源：桌面小组件与界面里的卡片必须是同一套颜色，
 * 各存一份迟早会不一致，而那种不一致只有用户看得出来。明暗由**应用主题偏好**决定，不是系统明暗
 * ——用户把应用锁成深色时，桌面上那张卡也该是深的。
 *
 * 十种小组件只落在六种结构上（大数字 / 开关 / 组合 / 占比 / 列表 / 趋势），因此这里的函数是
 * 按结构分的，不是按种类分的。「哪种用哪个结构、往结构里填什么」由 [NezhaWidgetKind] 与下面的
 * `when` 决定，避免十份几乎一样的渲染代码各自演化。
 */
internal class NezhaWidgetRenderer(private val context: Context) {
    private val presentation = NezhaWidgetPresentation(context)

    fun render(kind: NezhaWidgetKind, snapshot: NezhaWidgetSnapshot, darkTheme: Boolean): RemoteViews {
        val palette = if (darkTheme) darkNezhaPalette else lightNezhaPalette
        val views = RemoteViews(context.packageName, kind.layoutRes)
        views.setTextViewText(R.id.widget_title, context.getString(kind.labelRes))
        views.setTextColor(R.id.widget_title, palette.textSecondary.toArgb())
        // 用背景资源而不是背景色：设背景色会把圆角一起抹掉，卡片会变成方角。
        views.setInt(
            R.id.widget_root,
            "setBackgroundResource",
            if (darkTheme) R.drawable.nezha_widget_background_dark else R.drawable.nezha_widget_background_light,
        )
        views.setOnClickPendingIntent(R.id.widget_root, openAppIntent())
        when (kind) {
            NezhaWidgetKind.ProtectionToggle,
            NezhaWidgetKind.Overview,
            -> renderControl(views, kind, snapshot, palette)

            NezhaWidgetKind.BlockedCount,
            NezhaWidgetKind.ObservedTotal,
            NezhaWidgetKind.RuleScale,
            NezhaWidgetKind.ProtectedApplications,
            -> renderNumberCard(views, kind, snapshot, palette)

            NezhaWidgetKind.BlockedRatio -> renderRatio(views, snapshot, palette)
            NezhaWidgetKind.LatestBlocked,
            NezhaWidgetKind.TopBlockedApplications,
            -> renderList(views, kind, snapshot, palette)

            NezhaWidgetKind.BlockedTrend -> renderTrend(views, snapshot, palette)
        }
        return views
    }

    /** 开关与组合卡：状态文字 + 动作按钮；组合卡多一个大数字。点按钮才切换，点卡片打开应用。 */
    private fun renderControl(
        views: RemoteViews,
        kind: NezhaWidgetKind,
        snapshot: NezhaWidgetSnapshot,
        palette: NezhaPalette,
    ) {
        views.setTextViewText(R.id.widget_status, presentation.statusLabel(snapshot))
        views.setTextColor(R.id.widget_status, presentation.statusColor(snapshot, palette).toArgb())
        views.setTextViewText(R.id.widget_action, presentation.actionLabel(snapshot))
        // 动作是品牌色的粗体文字而不是填充按钮：填充按钮要一个带圆角的 drawable，
        // 那又得为明暗各存一份色值；文字直接用调色板，天然跟着主题走。
        views.setTextColor(R.id.widget_action, palette.brand.toArgb())
        views.setOnClickPendingIntent(R.id.widget_action, toggleIntent())
        if (kind == NezhaWidgetKind.Overview) {
            views.setTextViewText(R.id.widget_value, presentation.count(snapshot.blocked))
            views.setTextColor(R.id.widget_value, palette.textPrimary.toArgb())
        }
    }

    /** 大数字卡：四种的信息结构相同，只有标题、数值与脚注不同。 */
    private fun renderNumberCard(
        views: RemoteViews,
        kind: NezhaWidgetKind,
        snapshot: NezhaWidgetSnapshot,
        palette: NezhaPalette,
    ) {
        val totalRules = (snapshot.builtinRuleCount + snapshot.userRuleCount).toLong()
        val value = when (kind) {
            NezhaWidgetKind.ObservedTotal -> presentation.count(snapshot.observed)
            NezhaWidgetKind.RuleScale -> presentation.count(totalRules)
            NezhaWidgetKind.ProtectedApplications -> presentation.count(snapshot.protectedApplicationCount.toLong())
            else -> presentation.count(snapshot.blocked)
        }
        val detail = when (kind) {
            NezhaWidgetKind.BlockedCount ->
                context.getString(R.string.widget_blocked_count_detail, snapshot.distinctBlockedHosts)

            NezhaWidgetKind.ObservedTotal ->
                context.getString(R.string.widget_observed_total_detail, snapshot.relayed)

            NezhaWidgetKind.RuleScale ->
                context.getString(R.string.widget_rule_scale_detail, snapshot.userRuleCount)

            else -> context.getString(R.string.widget_protected_applications_detail)
        }
        views.setTextViewText(R.id.widget_value, value)
        views.setTextColor(R.id.widget_value, palette.textPrimary.toArgb())
        views.setTextViewText(R.id.widget_detail, detail)
        views.setTextColor(R.id.widget_detail, palette.textSecondary.toArgb())
    }

    /** 占比卡：已拦截与已放行两个读数，加一条按「已拦截 ÷ 已观测」点亮的段落条。 */
    private fun renderRatio(views: RemoteViews, snapshot: NezhaWidgetSnapshot, palette: NezhaPalette) {
        views.setTextViewText(R.id.widget_primary, context.getString(R.string.widget_ratio_blocked, snapshot.blocked))
        views.setTextColor(R.id.widget_primary, palette.brand.toArgb())
        views.setTextViewText(R.id.widget_secondary, context.getString(R.string.widget_ratio_relayed, snapshot.relayed))
        views.setTextColor(R.id.widget_secondary, palette.textSecondary.toArgb())
        val filled = presentation.ratioFilledSegments(snapshot)
        SEGMENTS.forEachIndexed { index, segmentId ->
            val color = if (index < filled) palette.brand else palette.surfaceElevated
            views.setInt(segmentId, "setBackgroundColor", color.toArgb())
        }
    }

    /**
     * 列表卡：最近拦截是一条，排名是多条，因此按行数组统一填。
     *
     * 没有数据时填一句实话并隐藏其余行，而不是留一张空卡：空卡会被读成「拦不到」，
     * 而真相往往是「还没开始保护」。
     */
    private fun renderList(
        views: RemoteViews,
        kind: NezhaWidgetKind,
        snapshot: NezhaWidgetSnapshot,
        palette: NezhaPalette,
    ) {
        val rows = if (kind == NezhaWidgetKind.LatestBlocked) {
            snapshot.latestBlockedHost?.let { host ->
                listOf(host to snapshot.latestBlockedApp.orEmpty())
            }.orEmpty()
        } else {
            snapshot.topBlockedApplications.map { application ->
                application.label to presentation.count(application.blocked)
            }
        }
        val emptyText = if (kind == NezhaWidgetKind.LatestBlocked) {
            context.getString(R.string.widget_latest_blocked_empty)
        } else {
            context.getString(R.string.widget_ranking_empty)
        }
        fillRows(views, rows, emptyText, palette)
    }

    /**
     * 趋势卡：八段色块，每段表示「那一段里有没有出现过拦截」。
     *
     * 只表达有无、不表达高低：RemoteViews 改不了单个子的高度（那是 API 31 才有的接口），
     * 硬凑高度只会在旧机型上变形。宁可少一个维度，也不要一个在部分机型上画错的图。
     */
    private fun renderTrend(views: RemoteViews, snapshot: NezhaWidgetSnapshot, palette: NezhaPalette) {
        val slices = snapshot.trendHasBlock
        SEGMENTS.forEachIndexed { index, segmentId ->
            val color = if (slices.getOrElse(index) { false }) palette.brand else palette.surfaceElevated
            views.setInt(segmentId, "setBackgroundColor", color.toArgb())
        }
        views.setTextViewText(
            R.id.widget_detail,
            if (slices.isEmpty()) {
                context.getString(R.string.widget_trend_empty)
            } else {
                context.getString(R.string.widget_trend_detail, slices.size)
            },
        )
        views.setTextColor(R.id.widget_detail, palette.textSecondary.toArgb())
    }

    private fun fillRows(
        views: RemoteViews,
        rows: List<Pair<String, String>>,
        emptyText: String,
        palette: NezhaPalette,
    ) {
        LIST_ROWS.forEachIndexed { index, ids ->
            val row = rows.getOrNull(index)
            views.setViewVisibility(ids[0], if (row == null) View.GONE else View.VISIBLE)
            views.setViewVisibility(ids[1], if (row == null) View.GONE else View.VISIBLE)
            if (row == null && index == 0) {
                views.setTextViewText(ids[0], emptyText)
                views.setTextColor(ids[0], palette.textSecondary.toArgb())
            } else if (row != null) {
                views.setTextViewText(ids[0], row.first)
                views.setTextColor(ids[0], palette.textPrimary.toArgb())
                views.setTextViewText(ids[1], row.second)
                views.setTextColor(ids[1], palette.textSecondary.toArgb())
            }
        }
    }

    /**
     * 点按卡片打开本应用；取不到启动入口就不给意图——好过给一个点了没反应的意图。
     */
    private fun openAppIntent(): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, REQUEST_OPEN_APP, intent, PENDING_FLAGS)
    }

    /**
     * 切换按钮的意图交给本应用自己的 receiver，而不是在这里直接拉起服务：授权被拒时需要打开界面
     * 让用户完成授权，那段流程属于界面层，小组件不该绕开它。
     *
     * 目标必须是**具体**的 receiver：抽象基类不能作为广播组件，系统无法实例化它。
     * 处理逻辑写在基类里，因此具体指向哪一个都等价，这里取名字与动作对应的那一个。
     */
    private fun toggleIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_TOGGLE,
        Intent(context, NezhaProtectionToggleWidgetProvider::class.java)
            .setAction(NezhaWidgetProvider.ACTION_TOGGLE_PROTECTION)
            .setPackage(context.packageName),
        PENDING_FLAGS,
    )
}
