package xin.ctkqiang.nezha_cyber.ads_block.network.vpn

import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.SocketTimeoutException
import java.time.Instant
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.DomainObservation
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.FilterDecision
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleAction

/** 单次查询等待上游应答的上限。超时即换下一个上游，全部失败则丢弃本次查询。 */
private const val QUERY_TIMEOUT_MILLIS = 2000

/**
 * 连续失败多少次就主动断开隧道。
 *
 * 隧道把域名解析接管在手里，一旦转发能力失效而它继续占着位置，整机解析都会停摆。
 * 因此宁可自己退出（退回系统默认解析），也不能拖住系统。任一成功都会把计数清零，
 * 所以只有「持续取不到应答」才会触发，瞬时丢包不会。
 */
private const val FAILURE_THRESHOLD = 8

private const val LOG_TAG = "NezhaDnsRelay"

/** 问题段无法解析时在观测记录里占位，避免把「解析不了」伪装成某个域名。 */
private const val UNREADABLE_HOST = "-"

/**
 * 单次转发的结果。区分「不是我们要的包」与「上游取不到应答」——只有后者才计入失败，
 * 否则任何一个往隧道地址丢垃圾包的应用都能把隧道逼停。
 */
private enum class ForwardOutcome { Ignored, Delivered, Unreachable }

/**
 * DNS 中继与过滤。
 *
 * 每个查询的处理顺序：解析域名 → 交给规则引擎 → 命中阻断则本地合成 NXDOMAIN，
 * 否则转发到上游取回应答 → 按 IPv4/UDP 头封装回写 → 记录一条观测。
 *
 * 三个设计要点：
 * - **问题段解析失败时继续转发**，不丢弃。丢弃会让某个应用的解析因为一个畸形包而停摆，
 *   而过滤的职责是「判定域名」，不是「拒绝看不懂的东西」。
 * - **拦截时不给上游发查询**：既省一次往返，也不把用户的访问意图泄露给解析服务。
 * - **引擎按需读取**：规则在运行中可能被用户修改，中继持有一个提供者而不是一份快照。
 *
 * 隧道内捕获的地址是本应用自造的，与外界无重叠，因此不存在把这些转发目标上的其它服务
 * 一并吸进隧道的可能。
 *
 * 已知边界，显式记录而不是假装不存在（第 19、29 节）：
 * - 只处理 IPv4/UDP。IPv6 与 TCP 不接管，也就不会把它们引到隧道里来。
 * - 载荷上限为单个 MTU，超大应答会被丢弃，客户端会重试。
 */
internal class DnsRelay(
    private val tunnel: ParcelFileDescriptor,
    upstreamServers: List<Inet4Address>,
    private val inputs: RelayInputs,
    private val host: TunnelHost,
) {
    private val socket = DatagramSocket().apply { soTimeout = QUERY_TIMEOUT_MILLIS }

    private val inbound = ByteArray(Ipv4UdpFormat.MAX_PACKET_SIZE)

    private val isSocketProtected = host.protect(socket)

    /** 上游 DNS。换网时由服务整体替换，读取方只会看到某一时刻的完整快照，因此无需加锁。 */
    @Volatile
    private var upstream: List<Inet4Address> = upstreamServers.toList()

    /** 只有中继线程会改这个计数，因此不需要同步。 */
    private var consecutiveFailures = 0

    /**
     * 受保护标记。未被保护的转发 socket 会把包重新送回隧道形成自环。
     * 这个值为 false 时，调用方必须放弃建立隧道，而不是继续运行。
     */
    val isUsable: Boolean
        get() = isSocketProtected

    fun runWhile(isActive: () -> Boolean) {
        if (!isUsable) return
        val deviceInput = FileInputStream(tunnel.fileDescriptor)
        val deviceOutput = FileOutputStream(tunnel.fileDescriptor)
        try {
            while (isActive()) {
                val readLength = deviceInput.read(inbound)
                if (readLength <= 0) continue
                if (relayOnce(readLength, deviceOutput)) {
                    host.onUpstreamUnreachable()
                    return
                }
            }
        } catch (failure: IOException) {
            // 停止隧道会关闭描述符，阻塞中的 read 随之抛 IOException，这是停止路径的一部分。
            // 只有在本应继续运行时抛出才是真故障，此时必须留下记录而不是静默结束。
            if (isActive()) {
                Log.w(LOG_TAG, "DNS 中继意外退出，隧道将随之停止", failure)
            }
        }
    }

    fun updateUpstream(servers: List<Inet4Address>) {
        upstream = servers.toList()
    }

    fun close() {
        socket.close()
    }

    private fun recordFailure(): Boolean {
        consecutiveFailures += 1
        return consecutiveFailures >= FAILURE_THRESHOLD
    }

    /** 处理一个入站包；返回 true 表示上游已连续失败到阈值，调用方应停止中继。 */
    private fun relayOnce(readLength: Int, deviceOutput: FileOutputStream): Boolean =
        when (handlePacket(readLength, deviceOutput)) {
            ForwardOutcome.Ignored -> false
            ForwardOutcome.Delivered -> {
                consecutiveFailures = 0
                false
            }
            ForwardOutcome.Unreachable -> recordFailure()
        }

    private fun handlePacket(readLength: Int, deviceOutput: FileOutputStream): ForwardOutcome {
        val query = readDnsQuery(inbound, readLength) ?: return ForwardOutcome.Ignored
        val question = readQuestion(query.payload, query.payload.size)
        val decision = question?.let { parsed -> inputs.ruleEngine().evaluate(parsed.name) }
            ?: FilterDecision.Unknown
        val payload = resolvePayload(query, question, decision) ?: return ForwardOutcome.Unreachable
        return deliver(query, payload, question, decision, deviceOutput)
    }

    private fun deliver(
        query: DnsQuery,
        payload: ByteArray,
        question: DnsQuestion?,
        decision: FilterDecision,
        deviceOutput: FileOutputStream,
    ): ForwardOutcome {
        val packet = buildDnsResponsePacket(
            payload = payload,
            upstreamAddress = query.destinationAddress,
            tunnelAddress = query.sourceAddress,
            upstreamPort = Ipv4UdpFormat.DNS_PORT,
            queryPort = query.sourcePort,
        ) ?: return ForwardOutcome.Ignored
        deviceOutput.write(packet)
        inputs.observations.record(toObservation(query, question, decision))
        return ForwardOutcome.Delivered
    }

    /**
     * 取出要回给客户端的 DNS 载荷。
     *
     * 命中阻断时在本机合成应答，不向上游发起查询；应答的形式由用户的隐私设置决定
     * （`BlockedResponseMode`）。放行的查询依次尝试每个上游。
     * 复用同一个 socket 时，先发出去的那个上游的迟到应答可能在下一次等待里被读到，
     * 这无害：DNS 应答带查询事务号，客户端只认自己的那次查询。
     */
    private fun resolvePayload(query: DnsQuery, question: DnsQuestion?, decision: FilterDecision): ByteArray? {
        if (question != null && decision.action == RuleAction.BLOCK) {
            return buildBlockedDnsPayload(
                request = query.payload,
                questionEndOffset = question.endOffset,
                requestLength = query.payload.size,
                mode = inputs.blockedResponseMode(),
            )
        }
        for (server in upstream) {
            val response = askUpstream(query.payload, server) ?: continue
            return response
        }
        return null
    }

    private fun toObservation(query: DnsQuery, question: DnsQuestion?, decision: FilterDecision): DomainObservation =
        DomainObservation(
            at = Instant.now(),
            host = question?.name ?: UNREADABLE_HOST,
            action = decision.action,
            matchedRule = decision.matchedRule,
            source = decision.source,
            // 归属查询是增强信息：系统在 API 29 以下没有等价接口，取不到就如实留空，
            // 界面显示「未知来源」，绝不猜一个应用出来。
            packageName = inputs.attributePackage(query),
        )

    private fun askUpstream(payload: ByteArray, server: Inet4Address): ByteArray? = try {
        socket.send(DatagramPacket(payload, payload.size, server, Ipv4UdpFormat.DNS_PORT))
        val buffer = ByteArray(Ipv4UdpFormat.MAX_PACKET_SIZE)
        val received = DatagramPacket(buffer, buffer.size)
        socket.receive(received)
        buffer.copyOf(received.length)
    } catch (timeout: SocketTimeoutException) {
        // 上游无应答属于常态（丢包、限速），逐条记录只会刷屏，因此静默换下一个。
        null
    } catch (unavailable: IOException) {
        // 网络切换或 socket 被关闭：同样换下一个，隧道是否继续由服务侧决定。
        null
    }
}
