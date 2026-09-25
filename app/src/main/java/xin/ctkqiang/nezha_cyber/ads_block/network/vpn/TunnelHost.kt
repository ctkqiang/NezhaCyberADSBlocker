package xin.ctkqiang.nezha_cyber.ads_block.network.vpn

import java.net.DatagramSocket

/**
 * 隧道宿主向中继提供的能力。
 *
 * 把这两个回调收拢成一个接口，而不是让中继的构造参数继续增长：它们同属「只有服务能给、
 * 中继自己拿不到」的那一类能力。分开传会让参数表越来越长，调用点也难以核对是否漏传，
 * 而漏传其中一个（例如忘了保护 socket）正好会退化成回包自环这种很难排查的故障。
 */
internal interface TunnelHost {
    /**
     * 把转发用的 socket 绑到隧道之外。
     *
     * 返回 false 表示绑定失败，此时绝不能继续运行：未受保护的 socket 会把包重新送回隧道。
     */
    fun protect(socket: DatagramSocket): Boolean

    /** 上游持续不可达时通知宿主，由宿主决定是否断开隧道。 */
    fun onUpstreamUnreachable()
}
