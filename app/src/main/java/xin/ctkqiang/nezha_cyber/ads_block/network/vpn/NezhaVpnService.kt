package xin.ctkqiang.nezha_cyber.ads_block.network.vpn

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import java.net.DatagramSocket
import java.net.Inet4Address
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import xin.ctkqiang.nezha_cyber.ads_block.AppContainer
import xin.ctkqiang.nezha_cyber.ads_block.R
import xin.ctkqiang.nezha_cyber.ads_block.domain.vpn.VpnFailureReason
import xin.ctkqiang.nezha_cyber.ads_block.domain.vpn.VpnSessionState
import xin.ctkqiang.nezha_cyber.ads_block.notification.platform.AdBlockNotifier
import xin.ctkqiang.nezha_cyber.ads_block.requireAppContainer

/** 隧道 MTU。取以太网标准值，避免为 DNS 这种小包做分片。 */
private const val TUN_MTU = 1500

/** 隧道自身的地址。 */
private const val TUN_IPV4_ADDRESS = "10.111.222.1"

/**
 * 隧道内自造的 DNS 地址。
 *
 * 这是整个设计里最关键的一处：系统被配置成把域名解析发给这个地址，而它**只存在于本隧道内**，
 * 外界没有任何主机使用它。因此隧道捕获的流量在结构上不可能与其它应用发生重叠——
 * 不会像「路由真实 DNS 服务器」那样，把发往那台机器的其它协议（TCP 53、NTP、门户认证）
 * 一并吸进隧道再丢掉。
 *
 * 上游解析由 [DnsRelay] 用受保护 socket 转发给隧道之外那张网的 DNS 服务器。
 */
private const val TUN_DNS_ADDRESS = "10.111.222.2"

/** 单主机前缀长度：隧道地址、DNS 地址与路由都是「一个地址」，因此共用同一个值。 */
private const val IPV4_HOST_PREFIX = 32

/**
 * DNS 隧道服务。
 *
 * 路由策略（工程规则第 6 节要求把策略写进代码）：
 * 本阶段尚未提供逐应用选择，因此隧道只接管**一个自造的 DNS 地址**的 /32 路由，此外不放行任何路由。
 * 后果有两层：其它应用的正常网络流量完全不经过隧道；即使解析链路出问题，能受影响的也只是
 * 域名解析，而不是全部连接。
 *
 * 逐应用选择落地后，这里改为对选中的包名调用 `addAllowedApplication`，且**不同时**使用
 * `addDisallowedApplication`：两者混用会让「选中」与「排除」互相覆盖，行为不可预测，
 * 因此策略只走一条路径。
 */
class NezhaVpnService : VpnService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var container: AppContainer

    private lateinit var trafficAttributor: TrafficAttributor

    private lateinit var adBlockNotifier: AdBlockNotifier

    private var dnsServerMonitor: DnsServerMonitor? = null
    private var tunnel: ParcelFileDescriptor? = null
    private var relay: DnsRelay? = null
    private var relayJob: Job? = null
    private var notifierJob: Job? = null

    @Volatile
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        container = requireAppContainer(this)
        trafficAttributor = TrafficAttributor(this)
        adBlockNotifier = AdBlockNotifier(this, container.installedApplicationSource)
        VpnNotification.ensureChannel(this)
        // 换网时只把新的上游地址推给中继，不重建隧道：隧道里唯一被捕获的地址是本应用自造的，
        // 与具体网络无关，因此没有任何理由为了 DNS 变化去拆掉隧道。
        dnsServerMonitor = DnsServerMonitor(this) { servers -> relay?.updateUpstream(servers) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSession()
        } else {
            startSession()
        }
        // 不自动重启：系统不能在用户关掉之后把隧道拉回来（工程规则第 7 节）。
        return START_NOT_STICKY
    }

    override fun onRevoke() {
        // 另一条 VPN 接管或用户撤销授权时系统会调用这里，必须干净退出。
        stopSession()
        super.onRevoke()
    }

    override fun onDestroy() {
        // 被系统直接销毁而隧道仍在时把状态归位；失败态必须保留，否则用户看不到失败原因。
        if (isRunning) {
            tearDown()
            VpnSessionRegistry.publish(VpnSessionState.Stopped)
        } else {
            tearDown()
        }
        dnsServerMonitor = null
        serviceScope.cancel()
        super.onDestroy()
    }

    @Synchronized
    private fun startSession() {
        if (isRunning) return
        VpnSessionRegistry.publish(VpnSessionState.Starting)
        promoteToForeground()
        if (establishTunnel(dnsServerMonitor?.current().orEmpty())) {
            dnsServerMonitor?.start()
            VpnSessionRegistry.publish(VpnSessionState.Running(Instant.now()))
        } else {
            tearDown()
            stopForeground(STOP_FOREGROUND_REMOVE)
            VpnSessionRegistry.publish(VpnSessionState.Failed(VpnFailureReason.TunnelEstablishmentFailed))
            stopSelf()
        }
    }

    @Synchronized
    private fun stopSession() {
        if (!isRunning && tunnel == null) {
            VpnSessionRegistry.publish(VpnSessionState.Stopped)
            return
        }
        VpnSessionRegistry.publish(VpnSessionState.Stopping)
        tearDown()
        stopForeground(STOP_FOREGROUND_REMOVE)
        VpnSessionRegistry.publish(VpnSessionState.Stopped)
    }

    /**
     * 中继连续多次取不到上游应答时调用。
     *
     * 此时隧道已经无法履行转发职责，继续持有只会让整机解析停摆。主动断开并把原因如实上报，
     * 让系统退回默认解析——先保住正常上网，再让用户决定是否重试。
     */
    @Synchronized
    private fun handleUpstreamUnreachable() {
        stopSession()
        VpnSessionRegistry.publish(VpnSessionState.Failed(VpnFailureReason.UpstreamUnreachable))
        stopSelf()
    }

    private fun promoteToForeground() {
        val notification = VpnNotification.build(
            context = this,
            contentIntent = VpnNotification.contentIntent(this),
            stopIntent = VpnNotification.stopIntent(this),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                VpnNotification.notificationId(),
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED,
            )
        } else {
            startForeground(VpnNotification.notificationId(), notification)
        }
    }

    private fun establishTunnel(upstream: List<Inet4Address>): Boolean {
        val builder = Builder()
            .setSession(getString(R.string.app_name))
            .setMtu(TUN_MTU)
            .addAddress(TUN_IPV4_ADDRESS, IPV4_HOST_PREFIX)
            .addDnsServer(TUN_DNS_ADDRESS)
            .addRoute(TUN_DNS_ADDRESS, IPV4_HOST_PREFIX)
        builder.applyProtectedApplications(container.protectedApplicationStore.protectedPackages.value)
        val descriptor = builder.establish()
        val dnsRelay = descriptor?.let { opened ->
            DnsRelay(
                tunnel = opened,
                upstreamServers = upstream,
                inputs = RelayInputs(
                    ruleEngine = { container.ruleEngine.value },
                    blockedResponseMode = { container.privacyPolicyStore.policy.value.blockedResponseMode },
                    observations = container.observationStore,
                    attributePackage = trafficAttributor::ownerPackage,
                    onBlocked = adBlockNotifier::record,
                ),
                host = object : TunnelHost {
                    override fun protect(socket: DatagramSocket): Boolean = this@NezhaVpnService.protect(socket)

                    override fun onUpstreamUnreachable() = handleUpstreamUnreachable()
                },
            )
        }
        if (dnsRelay == null || !dnsRelay.isUsable) {
            // 未受保护的转发 socket 会把包送回隧道形成自环，必须放弃而不是继续运行。
            dnsRelay?.close()
            descriptor?.close()
            return false
        }
        tunnel = descriptor
        relay = dnsRelay
        isRunning = true
        relayJob = serviceScope.launch { dnsRelay.runWhile { isRunning } }
        // 拦截通知跑在独立协程里：中继线程只负责把结果塞进去，发布节奏由这里控制。
        notifierJob = serviceScope.launch { adBlockNotifier.runWhile { isRunning } }
        return true
    }

    @Synchronized
    private fun tearDown() {
        isRunning = false
        dnsServerMonitor?.stop()
        relay?.close()
        relay = null
        relayJob?.cancel()
        relayJob = null
        notifierJob?.cancel()
        notifierJob = null
        adBlockNotifier.cancel()
        tunnel?.close()
        tunnel = null
    }

    companion object {
        /** 由通知动作或控制器发出的停止指令。 */
        const val ACTION_STOP = "xin.ctkqiang.nezha_cyber.ads_block.action.STOP_VPN"
    }
}

private const val LOG_TAG = "NezhaVpnService"

/**
 * 逐应用路由。
 *
 * 空集合表示**不做逐应用过滤**，即接管全部应用的域名解析，这是默认行为；集合非空表示
 * 只接管选中的应用，其余应用完全不经过隧道（工程规则第 6 节）。
 *
 * 只使用 `addAllowedApplication`，不与 `addDisallowedApplication` 混用：两者同时生效时
 * 「允许」与「排除」会互相覆盖，行为不可预测，因此策略只走一条路径。
 *
 * 个别包名可能在用户选择之后被卸载，此时 `addAllowedApplication` 抛 `NameNotFoundException`。
 * 必须逐条跳过而不是让整个隧道建立失败——一个被卸载的应用不该让保护整体失效。
 */
private fun VpnService.Builder.applyProtectedApplications(protectedPackages: Set<String>) {
    protectedPackages.forEach { packageName ->
        try {
            addAllowedApplication(packageName)
        } catch (missing: PackageManager.NameNotFoundException) {
            Log.w(LOG_TAG, "受保护应用已不存在，跳过：$packageName", missing)
        }
    }
}
