package xin.ctkqiang.nezha_cyber.ads_block.network.vpn

import java.net.Inet4Address
import java.net.InetAddress

/**
 * 从隧道里读出的一个 DNS 查询。
 *
 * 载荷已经拷出为独立数组，下游不必再关心原始报文里的偏移，也就不会出现偏移算错却仍能跑通的情况。
 */
internal class DnsQuery(
    val sourceAddress: Inet4Address,
    val destinationAddress: Inet4Address,
    val sourcePort: Int,
    val payload: ByteArray,
)

/**
 * 解析一个隧道入站包。
 *
 * 只接受 IPv4/UDP 且目标端口为 53 的包，其余一律返回 null 交由调用方丢弃。
 * 所有边界都显式校验：报文长度、版本、协议、UDP 长度与载荷范围（工程规则第 21 节要求把网络数据
 * 当作不可信输入，畸形包不得导致崩溃，也不得触发无界分配）。
 *
 * 这里豁免 ReturnCount：逐字段校验必须能在任意一步拒绝，用单个出口会引入多层嵌套反而更难审查，
 * 因此除最后构造结果外全部采用守卫式早返回。
 */
@Suppress("ReturnCount")
internal fun readDnsQuery(packet: ByteArray, length: Int): DnsQuery? {
    if (length < Ipv4UdpFormat.IPV4_HEADER_SIZE + Ipv4UdpFormat.UDP_HEADER_SIZE) return null
    val versionAndHeaderLength = packet[0].toInt() and Ipv4UdpFormat.BYTE_MASK
    if (versionAndHeaderLength ushr Ipv4UdpFormat.VERSION_SHIFT != Ipv4UdpFormat.IPV4_VERSION) return null
    val protocol = packet[Ipv4UdpFormat.IPV4_PROTOCOL_OFFSET].toInt() and Ipv4UdpFormat.BYTE_MASK
    if (protocol != Ipv4UdpFormat.IPV4_PROTOCOL_UDP) return null
    val headerLength = (versionAndHeaderLength and Ipv4UdpFormat.IPV4_VERSION_MASK) * Ipv4UdpFormat.IPV4_WORD_SIZE
    if (headerLength < Ipv4UdpFormat.IPV4_HEADER_SIZE) return null
    if (length < headerLength + Ipv4UdpFormat.UDP_HEADER_SIZE) return null
    val destinationPort = Ipv4UdpFormat.readUnsignedShort(
        packet,
        headerLength + Ipv4UdpFormat.UDP_DESTINATION_PORT_OFFSET,
    )
    if (destinationPort != Ipv4UdpFormat.DNS_PORT) return null
    val udpLength = Ipv4UdpFormat.readUnsignedShort(packet, headerLength + Ipv4UdpFormat.UDP_LENGTH_OFFSET)
    if (udpLength < Ipv4UdpFormat.UDP_HEADER_SIZE) return null
    val payloadOffset = headerLength + Ipv4UdpFormat.UDP_HEADER_SIZE
    val available = length - payloadOffset
    val payloadLength = minOf(udpLength - Ipv4UdpFormat.UDP_HEADER_SIZE, available)
    if (payloadLength <= 0) return null
    val sourceAddress = packet.readAddress(Ipv4UdpFormat.IPV4_SOURCE_ADDRESS_OFFSET) ?: return null
    val destinationAddress = packet.readAddress(Ipv4UdpFormat.IPV4_DESTINATION_ADDRESS_OFFSET) ?: return null
    return DnsQuery(
        sourceAddress = sourceAddress,
        destinationAddress = destinationAddress,
        sourcePort = Ipv4UdpFormat.readUnsignedShort(packet, headerLength + Ipv4UdpFormat.UDP_SOURCE_PORT_OFFSET),
        payload = packet.copyOfRange(payloadOffset, payloadOffset + payloadLength),
    )
}

private fun ByteArray.readAddress(offset: Int): Inet4Address? {
    val bytes = copyOfRange(offset, offset + Ipv4UdpFormat.IPV4_ADDRESS_SIZE)
    return InetAddress.getByAddress(bytes) as? Inet4Address
}
