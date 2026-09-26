package xin.ctkqiang.nezha_cyber.ads_block.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import java.text.NumberFormat
import xin.ctkqiang.nezha_cyber.ads_block.R
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaPalette

/**
 * 占比条的段数，与 `nezha_widget_segment_1..8` 的段数一致。
 *
 * 用分段条而不是 `ProgressBar`：后者的着色要靠 `setProgressTintList` 之类的接口，而 RemoteViews
 * 只允许调用白名单里的方法，不在白名单上的会直接被拒绝；分段条的每一段都是普通 View，
 * 颜色由调色板直接给出，没有这层不确定性。
 */
internal const val WIDGET_RATIO_SEGMENTS = 8

/**
 * 快照到「文字与颜色」的映射。
 *
 * 单独成类而不是混在渲染里，有两个原因：一是渲染类已经接近函数数量上限，再堆会触发静态检查；
 * 二是这一层是纯映射（同样的状态永远得到同样的结果），可以脱离 RemoteViews 单测——
 * 而 RemoteViews 在单元测试里没有实现，混进去就没法验证了。
 *
 * 所有判断都在这里收口：状态文案、状态颜色、按钮文案、计数格式、进度条刻度。
 * 分散到各分支里，迟早出现「开关卡说已开启、组合卡说已关闭」这种自相矛盾的画面。
 */
internal class NezhaWidgetPresentation(private val context: Context) {
    fun statusLabel(snapshot: NezhaWidgetSnapshot): String = when {
        snapshot.hasFailed -> context.getString(R.string.widget_status_failed)
        snapshot.isTransitioning -> context.getString(R.string.widget_status_transitioning)
        snapshot.isRunning -> context.getString(R.string.widget_status_running)
        else -> context.getString(R.string.widget_status_stopped)
    }

    /**
     * 状态色。
     *
     * 失败与运行中都取品牌色：它们都是「有事情正在发生」，需要与「已停止」的灰区分开。
     * 具体是哪种由 [statusLabel] 的文字说明，不靠颜色区分——颜色对色觉障碍用户不可靠。
     */
    fun statusColor(snapshot: NezhaWidgetSnapshot, palette: NezhaPalette): Color =
        if (snapshot.isRunning || snapshot.hasFailed) palette.brand else palette.textSecondary

    fun actionLabel(snapshot: NezhaWidgetSnapshot): String = if (snapshot.isRunning) {
        context.getString(R.string.widget_action_stop)
    } else {
        context.getString(R.string.widget_action_start)
    }

    fun count(value: Long): String = NumberFormat.getIntegerInstance().format(value)

    /** 占比条要填亮几段。没有观测时返回 0，不返回满格——满格会被读成「全都拦住了」。 */
    fun ratioFilledSegments(snapshot: NezhaWidgetSnapshot): Int = if (snapshot.observed <= 0) {
        0
    } else {
        (snapshot.blockedFraction * WIDGET_RATIO_SEGMENTS).toInt().coerceIn(0, WIDGET_RATIO_SEGMENTS)
    }
}
