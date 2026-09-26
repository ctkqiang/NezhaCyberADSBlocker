package xin.ctkqiang.nezha_cyber.ads_block.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import xin.ctkqiang.nezha_cyber.ads_block.R
import xin.ctkqiang.nezha_cyber.ads_block.domain.vpn.VpnStartResult
import xin.ctkqiang.nezha_cyber.ads_block.network.vpn.AndroidVpnController
import xin.ctkqiang.nezha_cyber.ads_block.requireAppContainer

private const val LOG_TAG = "NezhaWidget"

/**
 * 一次小组件任务允许占用的最长时间。
 *
 * `goAsync()` 给的安全窗口约 10 秒，这里留一半余量：触到上限说明某个端口不正常，
 * 此时宁可放弃这一次（保留上一次的画面），也不要卡住广播，让系统以为这个应用没响应。
 */
private const val TASK_TIMEOUT_MILLIS = 5000L

/**
 * 全部桌面小组件的公共实现。
 *
 * 十个具体小组件各自只有一个构造函数，其余全部继承自这里——十份重复的刷新逻辑，
 * 迟早会出现某一种忘了处理某个动作。
 *
 * ## 不卡手机的三条硬约束
 *
 * 1. **不在主线程干活**：`onUpdate` 与 `onReceive` 都由系统在主线程回调，这里立刻交出去
 *    （[runWidgetTask]），因此既不会 ANR，也不会被系统提前回收进程——`goAsync()` 把广播的
 *    存活期交给了这次任务。
 * 2. **有超时**：[TASK_TIMEOUT_MILLIS] 之后放弃，保住上一次的画面。
 * 3. **不轮询、不唤醒**：不使用 `AlarmManager`、不申请 `WakeLock`、不在刷新里做网络或磁盘 I/O。
 *    周期性刷新交给系统的 `updatePeriodMillis`（元数据里是 30 分钟，平台允许的最小值），
 *    由系统统一批量调度。
 *
 * 刷新只读各端口的 `StateFlow.value`，不触发载入：小组件不该为了刷一个数字去读一次磁盘。
 */
internal abstract class NezhaWidgetProvider(private val kind: NezhaWidgetKind) : AppWidgetProvider() {
    final override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val ids = appWidgetIds.toList()
        runWidgetTask { updateViews(context, manager, ids) }
    }

    final override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE_PROTECTION -> runWidgetTask { toggleProtection(context) }

            // 应用内的改动（例如在设置里切了主题、开了保护）也要让桌面跟上。
            // 作用范围只限本应用，因此用 setPackage 定向发送：Android 8 起不再接收隐式广播，
            // 定向的仍然收得到。
            ACTION_REFRESH_ALL -> {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, javaClass)).toList()
                runWidgetTask { updateViews(context, manager, ids) }
            }

            else -> super.onReceive(context, intent)
        }
    }

    /**
     * 把一次任务交给后台协程，并接管广播的存活期。
     *
     * 必须由主线程调用（`goAsync()` 的要求），因此只从 `onUpdate` / `onReceive` 进入。
     * 异常统一走 [CoroutineExceptionHandler] 而不是 `catch (Exception)`：小组件刷新失败绝不能让
     * 宿主崩溃——Launcher 的一个广播不该掀掉整个应用——但也确实没有任何一类异常是这里能
     * 「处理」的，能做的只有记下来并保留上一次的画面。
     */
    private fun runWidgetTask(block: suspend () -> Unit) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(
            SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
                Log.w(LOG_TAG, "小组件任务失败", throwable)
            },
        )
        scope.launch {
            try {
                withTimeout(TASK_TIMEOUT_MILLIS) { block() }
            } catch (timedOut: TimeoutCancellationException) {
                Log.w(LOG_TAG, "小组件任务超时，保留上一次的画面", timedOut)
            } finally {
                pendingResult.finish()
                scope.cancel()
            }
        }
    }

    private suspend fun updateViews(context: Context, manager: AppWidgetManager, appWidgetIds: List<Int>) {
        if (appWidgetIds.isEmpty()) return
        val views = NezhaWidgetRenderer(context).render(
            kind = kind,
            snapshot = snapshotLoader(context).load(),
            darkTheme = isDarkTheme(context),
        )
        appWidgetIds.forEach { appWidgetId -> manager.updateAppWidget(appWidgetId, views) }
    }

    /**
     * 切换保护开关。
     *
     * 授权被拒时不在这里重试：从桌面发起的切换没有 Activity 可以承接授权结果，
     * 因此打开应用让用户在同一处完成授权，而不是在桌面反复弹一个永远失败的动作。
     */
    private suspend fun toggleProtection(context: Context) {
        val controller = AndroidVpnController(context)
        if (controller.session.value.isActive) {
            controller.stop()
        } else if (controller.start() is VpnStartResult.PermissionDenied) {
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { intent ->
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }
    }

    private fun isDarkTheme(context: Context): Boolean {
        val preference = requireAppContainer(context).themePreferenceStore.preference.value
        val isSystemDark = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
        return preference.isDark(isSystemDark)
    }

    /**
     * 构建快照加载器。
     *
     * 端口全部取自 `AppContainer`：桌面小组件与前台服务、界面读的必须是同一份数据，
     * 各建一份实现会出现「界面拦了 100 条、桌面显示 3 条」这种让人无法信任的差异。
     */
    private fun snapshotLoader(context: Context): NezhaWidgetSnapshotLoader {
        val container = requireAppContainer(context)
        return NezhaWidgetSnapshotLoader(
            observationStore = container.observationStore,
            protectedApplicationStore = container.protectedApplicationStore,
            ruleStore = container.ruleStore,
            vpnController = AndroidVpnController(context),
            unknownAppLabel = context.getString(R.string.ad_block_app_unknown),
            applicationSource = container.installedApplicationSource,
        )
    }

    companion object {
        /** 桌面按钮发来的切换指令。 */
        const val ACTION_TOGGLE_PROTECTION = "xin.ctkqiang.nezha_cyber.ads_block.action.WIDGET_TOGGLE"

        /** 应用内的改动需要让桌面跟上时发送。 */
        const val ACTION_REFRESH_ALL = "xin.ctkqiang.nezha_cyber.ads_block.action.WIDGET_REFRESH"

        /**
         * 让桌面上所有小组件重新取一次数据。
         *
         * 用定向广播而不是逐个组件刷新：小组件种类会增删，逐个刷新的写法每加一种都要改一处，
         * 而广播由系统分发给当前真实存在的那些 receiver，不受种类数量影响。
         */
        fun refreshAll(context: Context) {
            context.sendBroadcast(Intent(ACTION_REFRESH_ALL).setPackage(context.packageName))
        }
    }
}
