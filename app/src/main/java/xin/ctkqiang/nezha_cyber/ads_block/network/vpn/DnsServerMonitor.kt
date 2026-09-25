package xin.ctkqiang.nezha_cyber.ads_block.network.vpn

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import java.net.Inet4Address

/**
 * 隧道**之下**那张网的 DNS 服务器发现与变化监听。
 *
 * 这些地址是转发目标（上游），**不是**隧道要捕获的地址——隧道只捕获一个自造的隧道内地址。
 * 两者必须分清：若按上游地址加路由，发往那台机器的其它协议会被一并吸进隧道再丢掉；
 * 只把它当作转发目标，则它对其它应用完全不受影响。
 *
 * 关键约束：必须排除 VPN 网络本身，否则隧道建立后会读到自己，把上游指向一个无效目标。
 * 因此这里遍历全部非 VPN 网络取其 DNS 服务器并集：
 * - 天然不受隧道建立与拆除的影响；
 * - 多张网（Wi-Fi 与蜂窝同时在线）时取并集，系统实际用哪一张都能被接住。
 */
internal class DnsServerMonitor(context: Context, private val onUpstreamChanged: (List<Inet4Address>) -> Unit) {
    private val connectivityManager = context.applicationContext.getSystemService<ConnectivityManager>()

    private var callback: ConnectivityManager.NetworkCallback? = null

    fun current(): List<Inet4Address> {
        val manager = connectivityManager ?: return emptyList()
        return manager.allNetworks
            .filter { network -> isUnderlay(manager, network) }
            .flatMap { network -> manager.getLinkProperties(network)?.dnsServers.orEmpty() }
            .filterIsInstance<Inet4Address>()
            .distinct()
    }

    /**
     * 注册默认网络回调。
     *
     * 这里不按网络过滤回调：隧道自身的上下线也会触发，但 [current] 永远不包含 VPN 网络，
     * 因此最多只是把同一份上游列表再推一次。把过滤集中在一处，比在两处各写一遍更难出错。
     */
    fun start() {
        val manager = connectivityManager ?: return
        if (callback != null) return
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = notifyCurrent()

            override fun onLost(network: Network) = notifyCurrent()

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) = notifyCurrent()
        }
        manager.registerDefaultNetworkCallback(networkCallback)
        callback = networkCallback
    }

    fun stop() {
        val networkCallback = callback ?: return
        connectivityManager?.unregisterNetworkCallback(networkCallback)
        callback = null
    }

    private fun notifyCurrent() {
        onUpstreamChanged(current())
    }

    private fun isUnderlay(manager: ConnectivityManager, network: Network): Boolean {
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }
}
