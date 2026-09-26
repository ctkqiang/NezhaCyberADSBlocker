package xin.ctkqiang.nezha_cyber.ads_block.network.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import xin.ctkqiang.nezha_cyber.ads_block.domain.vpn.VpnController
import xin.ctkqiang.nezha_cyber.ads_block.domain.vpn.VpnFailureReason
import xin.ctkqiang.nezha_cyber.ads_block.domain.vpn.VpnSessionState
import xin.ctkqiang.nezha_cyber.ads_block.domain.vpn.VpnStartResult

/** 等待隧道进入稳定状态的上限。超过则视为建立失败，而不是让界面无限等待。 */
private const val TUNNEL_SETTLE_TIMEOUT_MILLIS = 10_000L

private const val LOG_TAG = "NezhaVpnController"

/**
 * [VpnController] 的 Android 适配器。
 *
 * 平台细节全部收在这里：授权检查用 `VpnService.prepare`，启动与停止通过显式 Intent 驱动
 * [NezhaVpnService]。领域层因此看不到 Context 与 Intent（第 38.3 节）。
 *
 * 启动是异步的：`startForegroundService` 只把请求交给系统，隧道是否建立成功由服务稍后写入状态。
 * 所以 [start] 会等待会话进入稳定态再返回结果，避免界面在隧道尚未建成时就显示「已开启」。
 */
class AndroidVpnController(context: Context) : VpnController {
    private val applicationContext = context.applicationContext

    override val session: StateFlow<VpnSessionState> = VpnSessionRegistry.state

    override suspend fun isAuthorized(): Boolean = withContext(Dispatchers.IO) {
        VpnService.prepare(applicationContext) == null
    }

    override suspend fun start(): VpnStartResult = withContext(Dispatchers.IO) {
        if (!isAuthorized()) {
            return@withContext VpnStartResult.PermissionDenied
        }
        val previousState = session.value
        ContextCompat.startForegroundService(applicationContext, serviceIntent())
        awaitSettledOutcome(previousState)
    }

    /**
     * 停止保护。
     *
     * 两步都做，顺序不能反：
     * 1. 先发一条显式的 [NezhaVpnService.ACTION_STOP]——由服务在主线程上处理，隧道在它返回之前
     *    就被关闭，用户按下停止与隧道真正关闭之间没有空窗；
     * 2. 再 [Context.stopService]——让服务本身退出、前台通知消失。
     *
     * 只做第二步是不够可靠的：`stopService` 是异步的，隧道何时关闭取决于系统什么时候回调
     * `onDestroy`，那个时机本应用控制不了。而隧道只要还开着，整机解析就仍然被引到本应用，
     * 用户看到的就是「按了停止，保护还开着、网也不通」。
     *
     * 第一步在后台可能被系统拒绝启动（Android 8 起限制后台启动服务，桌面小组件就属于这种情况）。
     * 这里不把它当作失败：拒绝时记一笔，第二步仍然是有效的停止手段，只是回到异步时序。
     */
    override suspend fun stop() {
        withContext(Dispatchers.IO) {
            val stopIntent = serviceIntent().setAction(NezhaVpnService.ACTION_STOP)
            runCatching { applicationContext.startService(stopIntent) }
                .onFailure { failure -> Log.w(LOG_TAG, "停止指令未能直接送达，改由 stopService 兜底", failure) }
            applicationContext.stopService(serviceIntent())
        }
    }

    /**
     * 等待本次启动产生结果。
     *
     * 必须排除「启动前的那个状态」：StateFlow 会先把当前值发给新订阅者，若上一次是失败态，
     * 直接取第一个 Running/Failed 会立刻返回那条旧失败，用户第二次点按钮就会立刻被告知失败，
     * 而隧道其实正在建立。因此这里等的是「相比启动前发生了变化」的那个稳定态。
     */
    private suspend fun awaitSettledOutcome(previousState: VpnSessionState): VpnStartResult {
        val settled = withTimeoutOrNull(TUNNEL_SETTLE_TIMEOUT_MILLIS) {
            session.first { state ->
                state != previousState &&
                    (state is VpnSessionState.Running || state is VpnSessionState.Failed)
            }
        }
        return when (settled) {
            is VpnSessionState.Running -> VpnStartResult.Started
            is VpnSessionState.Failed -> VpnStartResult.Failed(settled.reason)
            else -> VpnStartResult.Failed(VpnFailureReason.TunnelEstablishmentFailed)
        }
    }

    private fun serviceIntent(): Intent = Intent(applicationContext, NezhaVpnService::class.java)
}
