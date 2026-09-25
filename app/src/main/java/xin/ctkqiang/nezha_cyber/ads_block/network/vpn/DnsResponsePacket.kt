package xin.ctkqiang.nezha_cyber.ads_block.network.vpn

import java.net.Inet4Address

/**
 * 用上游应答拼出一个可以直接写回隧道的 IPv4/UDP 包。
 *
 * 返回 null 表示应答超过单个 MTU，本次查询只能丢弃、由客户端重试；不在这里做分片，
 * 因为分片状态需要跨包维护，代价远高于收益。
 *
 * UDP 校验和按惯例写 0（IPv4 允许不校验），避免为此引入伪首部计算。
 */
internal fun buildDnsResponsePacket(
    payload: ByteArray,
    upstreamAddress: Inet4Address,
    tunnelAddress: Inet4Address,
    upstreamPort: Int,
    queryPort: Int,
): ByteArray? {
    val totalLength = Ipv4UdpFormat.IPV4_HEADER_SIZE + Ipv4UdpFormat.UDP_HEADER_SIZE + payload.size
    if (totalLength > Ipv4UdpFormat.MAX_PACKET_SIZE) return null
    val packet = ByteArray(totalLength)
    packet.writeIpv4Header(totalLength, upstreamAddress, tunnelAddress)
    packet.writeUdpHeader(payload.size, upstreamPort, queryPort)
    System.arraycopy(
        payload,
        0,
        packet,
        Ipv4UdpFormat.IPV4_HEADER_SIZE + Ipv4UdpFormat.UDP_HEADER_SIZE,
        payload.size,
    )
    packet.writeIpv4Checksum()
    return packet
}

private fun ByteArray.writeIpv4Header(
    totalLength: Int,
    sourceAddress: Inet4Address,
    destinationAddress: Inet4Address,
) {
    val versionAndHeaderLength = (Ipv4UdpFormat.IPV4_VERSION shl Ipv4UdpFormat.VERSION_SHIFT) or
        (Ipv4UdpFormat.IPV4_HEADER_SIZE / Ipv4UdpFormat.IPV4_WORD_SIZE)
    this[0] = versionAndHeaderLength.toByte()
    this[1] = 0
    Ipv4UdpFormat.writeUnsignedShort(this, Ipv4UdpFormat.IPV4_TOTAL_LENGTH_OFFSET, totalLength)
    Ipv4UdpFormat.writeUnsignedShort(this, Ipv4UdpFormat.IPV4_IDENTIFICATION_OFFSET, 0)
    Ipv4UdpFormat.writeUnsignedShort(this, Ipv4UdpFormat.IPV4_FLAGS_OFFSET, Ipv4UdpFormat.IPV4_FLAG_DONT_FRAGMENT)
    this[Ipv4UdpFormat.IPV4_TTL_OFFSET] = Ipv4UdpFormat.IPV4_DEFAULT_TTL.toByte()
    this[Ipv4UdpFormat.IPV4_PROTOCOL_OFFSET] = Ipv4UdpFormat.IPV4_PROTOCOL_UDP.toByte()
    Ipv4UdpFormat.writeUnsignedShort(this, Ipv4UdpFormat.IPV4_CHECKSUM_OFFSET, 0)
    System.arraycopy(
        sourceAddress.address,
        0,
        this,
        Ipv4UdpFormat.IPV4_SOURCE_ADDRESS_OFFSET,
        Ipv4UdpFormat.IPV4_ADDRESS_SIZE,
    )
    System.arraycopy(
        destinationAddress.address,
        0,
        this,
        Ipv4UdpFormat.IPV4_DESTINATION_ADDRESS_OFFSET,
        Ipv4UdpFormat.IPV4_ADDRESS_SIZE,
    )
}

private fun ByteArray.writeUdpHeader(payloadSize: Int, sourcePort: Int, destinationPort: Int) {
    val udpLength = Ipv4UdpFormat.UDP_HEADER_SIZE + payloadSize
    Ipv4UdpFormat.writeUnsignedShort(
        this,
        Ipv4UdpFormat.IPV4_HEADER_SIZE + Ipv4UdpFormat.UDP_SOURCE_PORT_OFFSET,
        sourcePort,
    )
    Ipv4UdpFormat.writeUnsignedShort(
        this,
        Ipv4UdpFormat.IPV4_HEADER_SIZE + Ipv4UdpFormat.UDP_DESTINATION_PORT_OFFSET,
        destinationPort,
    )
    Ipv4UdpFormat.writeUnsignedShort(
        this,
        Ipv4UdpFormat.IPV4_HEADER_SIZE + Ipv4UdpFormat.UDP_LENGTH_OFFSET,
        udpLength,
    )
    Ipv4UdpFormat.writeUnsignedShort(
        this,
        Ipv4UdpFormat.IPV4_HEADER_SIZE + Ipv4UdpFormat.UDP_CHECKSUM_OFFSET,
        0,
    )
}

private fun ByteArray.writeIpv4Checksum() {
    var sum = 0
    var index = 0
    while (index < Ipv4UdpFormat.IPV4_HEADER_SIZE) {
        sum += Ipv4UdpFormat.readUnsignedShort(this, index)
        index += Ipv4UdpFormat.IPV4_CHECKSUM_STRIDE
    }
    while (sum ushr Ipv4UdpFormat.CHECKSUM_FOLD_SHIFT != 0) {
        sum = (sum and Ipv4UdpFormat.SHORT_MASK) + (sum ushr Ipv4UdpFormat.CHECKSUM_FOLD_SHIFT)
    }
    Ipv4UdpFormat.writeUnsignedShort(this, Ipv4UdpFormat.IPV4_CHECKSUM_OFFSET, sum.inv() and Ipv4UdpFormat.SHORT_MASK)
}
