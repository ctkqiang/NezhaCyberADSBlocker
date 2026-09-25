package xin.ctkqiang.nezha_cyber.ads_block.network.vpn

/**
 * IPv4/UDP 报文的结构常量与字段编解码。
 *
 * 读写两侧共用同一份定义：字段偏移是协议事实，一旦在两个文件里各写一份，改动时必然会漏掉一边。
 * 所有常量都以字节为单位。
 */
internal object Ipv4UdpFormat {
    const val MAX_PACKET_SIZE = 1500

    const val IPV4_HEADER_SIZE = 20
    const val IPV4_ADDRESS_SIZE = 4
    const val IPV4_VERSION = 4
    const val IPV4_WORD_SIZE = 4
    const val IPV4_PROTOCOL_UDP = 0x11
    const val IPV4_DEFAULT_TTL = 64
    const val IPV4_FLAG_DONT_FRAGMENT = 0x4000
    const val IPV4_VERSION_MASK = 0x0F

    const val IPV4_TOTAL_LENGTH_OFFSET = 2
    const val IPV4_IDENTIFICATION_OFFSET = 4
    const val IPV4_FLAGS_OFFSET = 6
    const val IPV4_TTL_OFFSET = 8
    const val IPV4_PROTOCOL_OFFSET = 9
    const val IPV4_CHECKSUM_OFFSET = 10
    const val IPV4_SOURCE_ADDRESS_OFFSET = 12
    const val IPV4_DESTINATION_ADDRESS_OFFSET = 16
    const val IPV4_CHECKSUM_STRIDE = 2

    const val UDP_HEADER_SIZE = 8
    const val UDP_SOURCE_PORT_OFFSET = 0
    const val UDP_DESTINATION_PORT_OFFSET = 2
    const val UDP_LENGTH_OFFSET = 4
    const val UDP_CHECKSUM_OFFSET = 6

    const val DNS_PORT = 53

    const val BYTE_MASK = 0xFF
    const val SHORT_MASK = 0xFFFF
    const val BYTE_BITS = 8
    const val INT_BYTES = 4
    const val INT_HIGH_SHIFT = 24
    const val VERSION_SHIFT = 4
    const val CHECKSUM_FOLD_SHIFT = 16

    fun readUnsignedShort(packet: ByteArray, offset: Int): Int =
        ((packet[offset].toInt() and BYTE_MASK) shl BYTE_BITS) or (packet[offset + 1].toInt() and BYTE_MASK)

    fun writeUnsignedShort(packet: ByteArray, offset: Int, value: Int) {
        packet[offset] = (value ushr BYTE_BITS).toByte()
        packet[offset + 1] = value.toByte()
    }

    /**
     * 写一个 32 位无符号量，大端序。
     *
     * 目前只有「合成 0.0.0.0 的 A 记录」用得到它（写 TTL 字段）。放进这里而不是写在使用处：
     * 字节序是协议事实，两边各写一份移位逻辑必然会有一天对不上。
     *
     * 用循环而不是逐字节写出：网络字节序是**从高位到低位**，这个规则由「每前进一个字节就少移
     * 8 位」表达一次即可，写成四行手算移位反而容易在第几行移多少上出错。
     */
    fun writeInt(packet: ByteArray, offset: Int, value: Int) {
        repeat(INT_BYTES) { index ->
            packet[offset + index] = (value ushr (INT_HIGH_SHIFT - index * BYTE_BITS)).toByte()
        }
    }
}
