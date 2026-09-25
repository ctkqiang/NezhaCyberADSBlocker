package xin.ctkqiang.nezha_cyber.ads_block.network.vpn

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.core.content.getSystemService
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap

private const val IPPROTO_UDP = 17

private const val LOG_TAG = "NezhaTrafficAttributor"

/** 表示「查过但没有结果」，与「还没查过」区分开，避免对未知 UID 反复查询。 */
private const val UNKNOWN_PACKAGE = ""

/**
 * 把一次 DNS 查询归属到发起它的应用。
 *
 * 这是「哪个应用请求了哪个域名」的唯一来源。隧道本身拿不到发起方信息——TUN 里只有 IP 包，
 * 因此必须反查连接归属：用本机地址与目标地址四元组去问系统这条连接属于哪个 UID，
 * 再由 UID 映射到包名。
 *
 * 两条实现约束：
 * - `getConnectionOwnerUid` 自 API 29 起提供，更低版本没有等价接口，因此如实返回 null，
 *   界面显示「未知来源」，而不是猜一个应用出来（工程规则第 32 节）；
 * - 归属查询逐包进行，因此 UID → 包名的映射必须缓存，否则每个查询都要走一次包管理服务。
 */
internal class TrafficAttributor(context: Context) {
    private val connectivityManager = context.applicationContext.getSystemService<ConnectivityManager>()

    private val packageManager = context.applicationContext.packageManager

    private val packageCache = ConcurrentHashMap<Int, String>()

    /**
     * 系统是否提供归属查询。
     *
     * 少数定制系统声明了该接口却未实现。一旦遇到就永久关掉，否则每个 DNS 查询都要走一次
     * 抛异常的路径——异常构造与栈回溯在查询热路径上是实打实的开销。
     */
    @Volatile
    private var isSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /** 返回发起该查询的包名；无法判定时返回 null。 */
    fun ownerPackage(query: DnsQuery): String? {
        if (!isSupported) return null
        val uid = lookupUid(query) ?: return null
        return packageNameOf(uid)
    }

    private fun lookupUid(query: DnsQuery): Int? {
        val manager = connectivityManager ?: return null
        return try {
            val uid = manager.getConnectionOwnerUid(
                IPPROTO_UDP,
                InetSocketAddress(query.sourceAddress, query.sourcePort),
                InetSocketAddress(query.destinationAddress, Ipv4UdpFormat.DNS_PORT),
            )
            uid.takeIf { value -> value != Process.INVALID_UID }
        } catch (unsupported: UnsupportedOperationException) {
            isSupported = false
            Log.w(LOG_TAG, "系统未实现连接归属查询，本次运行按未知来源处理", unsupported)
            null
        }
    }

    private fun packageNameOf(uid: Int): String? {
        val cached = packageCache[uid] ?: cachePackageName(uid)
        return cached.ifEmpty { null }
    }

    private fun cachePackageName(uid: Int): String {
        val resolved = packageManager.getPackagesForUid(uid)?.firstOrNull() ?: UNKNOWN_PACKAGE
        packageCache[uid] = resolved
        return resolved
    }
}
