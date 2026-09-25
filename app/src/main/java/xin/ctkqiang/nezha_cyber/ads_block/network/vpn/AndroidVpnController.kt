package xin.ctkqiang.nezha_cyber.ads_block.network.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
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
     * 停止走 [Context.stopService]：它是任何前后台状态下都允许的直接操作，
     * 不需要先启动服务再让它自杀，也就不会触发前台服务的启动时限要求。
     */
    override suspend fun stop() {
        withContext(Dispatchers.IO) {
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
