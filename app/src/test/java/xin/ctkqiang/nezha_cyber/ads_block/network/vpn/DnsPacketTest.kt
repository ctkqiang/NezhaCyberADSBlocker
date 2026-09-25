package xin.ctkqiang.nezha_cyber.ads_block.network.vpn

import java.net.Inet4Address
import java.net.InetAddress
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.BlockedResponseMode

/**
 * DNS 报文编解码的测试。
 *
 * 这段代码是整条链路里最可能出错、且出错代价最高的一环：解析失败意味着查询被丢弃，
 * 而隧道已经把域名解析接管在手里，表现就是整机解析停摆。因此边界必须逐条锁住
 * （工程规则第 21、30 节：网络数据一律视为不可信输入）。
 *
 * 断言里刻意使用字面量与独立计算，不复用产线常量：测试要对照协议本身，而不是对照实现自己。
 */
private const val ANSWER_RECORD_SIZE = 16

class DnsPacketTest {
    private val tunnelAddress = InetAddress.getByName("10.111.222.1") as Inet4Address
    private val dnsAddress = InetAddress.getByName("10.111.222.2") as Inet4Address

    @Test
    fun `解析合法的 IPv4 UDP DNS 查询`() {
        val query = readDnsQuery(buildQueryPacket(sourcePort = 40000, payloadSize = 12), QUERY_PACKET_SIZE)

        assertNotNull(query)
        requireNotNull(query)
        assertEquals(40000, query.sourcePort)
        assertEquals(dnsAddress, query.destinationAddress)
        assertEquals(tunnelAddress, query.sourceAddress)
        assertEquals(12, query.payload.size)
    }

    @Test
    fun `目标端口不是 53 时丢弃`() {
        val packet = buildQueryPacket(destinationPort = 443)

        assertNull(readDnsQuery(packet, packet.size))
    }

    @Test
    fun `协议不是 UDP 时丢弃`() {
        val packet = buildQueryPacket(protocol = 6)

        assertNull(readDnsQuery(packet, packet.size))
    }

    @Test
    fun `版本不是 IPv4 时丢弃`() {
        val packet = buildQueryPacket()
        packet[0] = 0x65.toByte()

        assertNull(readDnsQuery(packet, packet.size))
    }

    @Test
    fun `长度不足以容纳两个头时丢弃`() {
        val packet = buildQueryPacket()

        assertNull(readDnsQuery(packet, 24))
    }

    @Test
    fun `载荷为空时丢弃`() {
        val packet = buildQueryPacket(payloadSize = 0)

        assertNull(readDnsQuery(packet, packet.size))
    }

    @Test
    fun `UDP 长度字段超过实际报文时按实际长度截断而不是越界`() {
        val packet = buildQueryPacket(payloadSize = 8)
        writeUnsignedShort(packet, 24, 4096)

        val query = readDnsQuery(packet, packet.size)

        assertNotNull(query)
        requireNotNull(query)
        assertEquals(8, query.payload.size)
    }

    @Test
    fun `构造的应答包字段与校验和正确`() {
        val payload = byteArrayOf(0x12, 0x34, 0x56, 0x78)
        val response = requireNotNull(
            buildDnsResponsePacket(
                payload = payload,
                upstreamAddress = dnsAddress,
                tunnelAddress = tunnelAddress,
                upstreamPort = 53,
                queryPort = 40000,
            ),
        )

        assertEquals(20 + 8 + payload.size, response.size)
        assertEquals(20 + 8 + payload.size, readUnsignedShort(response, 2))
        assertEquals(0x45, response[0].toInt() and 0xFF)
        assertEquals(17, response[9].toInt() and 0xFF)
        assertArrayEquals(dnsAddress.address, response.copyOfRange(12, 16))
        assertArrayEquals(tunnelAddress.address, response.copyOfRange(16, 20))
        assertEquals(53, readUnsignedShort(response, 20))
        assertEquals(40000, readUnsignedShort(response, 22))
        assertEquals(8 + payload.size, readUnsignedShort(response, 24))
        assertArrayEquals(payload, response.copyOfRange(28, response.size))
        assertEquals(0xFFFF, headerChecksum(response))
    }

    @Test
    fun `应答超过单个 MTU 时返回 null 而不是截断`() {
        val oversized = ByteArray(1500)

        assertNull(
            buildDnsResponsePacket(
                payload = oversized,
                upstreamAddress = dnsAddress,
                tunnelAddress = tunnelAddress,
                upstreamPort = 53,
                queryPort = 40000,
            ),
        )
    }

    @Test
    fun `解析问题段得到域名与结束位置`() {
        val payload = buildDnsPayload("Ads.Example.com")

        val question = readQuestion(payload, payload.size)

        assertNotNull(question)
        requireNotNull(question)
        assertEquals("Ads.Example.com", question.name)
        assertEquals(payload.size, question.endOffset)
    }

    @Test
    fun `问题段出现压缩指针时拒绝`() {
        val payload = buildDnsPayload("ads.example.com")
        payload[12] = 0xC0.toByte()

        assertNull(readQuestion(payload, payload.size))
    }

    @Test
    fun `标签长度超出载荷时拒绝`() {
        val payload = buildDnsPayload("ads.example.com")
        payload[12] = 60

        assertNull(readQuestion(payload, payload.size))
    }

    @Test
    fun `头部不完整时拒绝`() {
        val payload = buildDnsPayload("ads.example.com")

        assertNull(readQuestion(payload, 12))
    }

    @Test
    fun `被拦截的应答是 NXDOMAIN 且各段计数清零`() {
        val payload = buildDnsPayload("ads.example.com")
        val question = requireNotNull(readQuestion(payload, payload.size))
        payload[2] = 0x01.toByte()
        payload[3] = 0x00.toByte()
        writeUnsignedShort(payload, 10, 1)

        val blocked = requireNotNull(
            buildBlockedDnsPayload(
                request = payload,
                questionEndOffset = question.endOffset,
                requestLength = payload.size,
                mode = BlockedResponseMode.NxDomain,
            ),
        )

        assertEquals(question.endOffset, blocked.size)
        assertEquals(0x8000 or 0x0100 or 0x0080 or 3, readUnsignedShort(blocked, 2))
        assertEquals(1, readUnsignedShort(blocked, 4))
        assertEquals(0, readUnsignedShort(blocked, 6))
        assertEquals(0, readUnsignedShort(blocked, 8))
        assertEquals(0, readUnsignedShort(blocked, 10))
        assertArrayEquals(
            payload.copyOfRange(12, question.endOffset),
            blocked.copyOfRange(12, question.endOffset),
        )
    }

    @Test
    fun `拒绝解析模式回 RCODE 5 且不加应答段`() {
        val payload = buildDnsPayload("ads.example.com")
        val question = requireNotNull(readQuestion(payload, payload.size))
        setRecursionDesired(payload)

        val blocked = requireNotNull(
            buildBlockedDnsPayload(
                request = payload,
                questionEndOffset = question.endOffset,
                requestLength = payload.size,
                mode = BlockedResponseMode.Refused,
            ),
        )

        assertEquals(question.endOffset, blocked.size)
        assertEquals(0x8000 or 0x0100 or 0x0080 or 5, readUnsignedShort(blocked, 2))
        assertEquals(0, readUnsignedShort(blocked, 6))
    }

    @Test
    fun `零地址模式补一条指向问题段的 A 记录且 RCODE 为 0`() {
        val payload = buildDnsPayload("ads.example.com")
        val question = requireNotNull(readQuestion(payload, payload.size))
        setRecursionDesired(payload)

        val blocked = requireNotNull(
            buildBlockedDnsPayload(
                request = payload,
                questionEndOffset = question.endOffset,
                requestLength = payload.size,
                mode = BlockedResponseMode.ZeroAddress,
            ),
        )

        // RCODE 必须是 0：NXDOMAIN 会让客户端忽略应答段里的 A 记录。
        assertEquals(0x8000 or 0x0100 or 0x0080, readUnsignedShort(blocked, 2))
        assertEquals(1, readUnsignedShort(blocked, 6))
        assertEquals(question.endOffset + ANSWER_RECORD_SIZE, blocked.size)

        val answer = question.endOffset
        assertEquals(0xC00C, readUnsignedShort(blocked, answer))
        assertEquals(1, readUnsignedShort(blocked, answer + 2))
        assertEquals(1, readUnsignedShort(blocked, answer + 4))
        // TTL 为 0：屏蔽结果必须能立刻改回去，不能被缓存。
        assertEquals(0, blocked[answer + 6].toInt())
        assertEquals(4, readUnsignedShort(blocked, answer + 10))
        assertArrayEquals(ByteArray(4), blocked.copyOfRange(answer + 12, answer + 16))
    }

    @Test
    fun `问题段结束位置越界时拒绝合成应答`() {
        val payload = buildDnsPayload("ads.example.com")

        assertNull(
            buildBlockedDnsPayload(
                request = payload,
                questionEndOffset = payload.size + 1,
                requestLength = payload.size,
                mode = BlockedResponseMode.NxDomain,
            ),
        )
    }

    /** 置上 RD 位（flags 高字节的 0x01），让被测载荷像一次真实的递归查询。 */
    private fun setRecursionDesired(payload: ByteArray) {
        payload[2] = 0x01.toByte()
        payload[3] = 0x00.toByte()
    }

    /** 构造一个只含头部与问题段的 DNS 载荷。 */
    private fun buildDnsPayload(name: String): ByteArray {
        val labels = name.split('.')
        val questionSize = labels.sumOf { label -> label.length + 1 } + 1 + 4
        val payload = ByteArray(12 + questionSize)
        writeUnsignedShort(payload, 4, 1)
        var offset = 12
        labels.forEach { label ->
            payload[offset] = label.length.toByte()
            offset += 1
            label.forEach { character ->
                payload[offset] = character.code.toByte()
                offset += 1
            }
        }
        payload[offset] = 0
        writeUnsignedShort(payload, offset + 1, 1)
        writeUnsignedShort(payload, offset + 3, 1)
        return payload
    }

    /** 构造一个结构完整的 IPv4/UDP 查询包，供各条边界用例按需篡改单个字段。 */
    private fun buildQueryPacket(
        sourcePort: Int = 40000,
        destinationPort: Int = 53,
        protocol: Int = 17,
        payloadSize: Int = 16,
    ): ByteArray {
        val packet = ByteArray(20 + 8 + payloadSize)
        packet[0] = 0x45
        packet[9] = protocol.toByte()
        System.arraycopy(tunnelAddress.address, 0, packet, 12, 4)
        System.arraycopy(dnsAddress.address, 0, packet, 16, 4)
        writeUnsignedShort(packet, 2, packet.size)
        writeUnsignedShort(packet, 20, sourcePort)
        writeUnsignedShort(packet, 22, destinationPort)
        writeUnsignedShort(packet, 24, 8 + payloadSize)
        for (index in 28 until packet.size) {
            packet[index] = index.toByte()
        }
        return packet
    }

    /** 路由器校验：把首部按 16 位求和并折叠，正确时总和应为 0xFFFF。 */
    private fun headerChecksum(packet: ByteArray): Int {
        var sum = 0
        var index = 0
        while (index < 20) {
            sum += readUnsignedShort(packet, index)
            index += 2
        }
        while (sum ushr 16 != 0) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        return sum
    }

    private fun readUnsignedShort(packet: ByteArray, offset: Int): Int =
        ((packet[offset].toInt() and 0xFF) shl 8) or (packet[offset + 1].toInt() and 0xFF)

    private fun writeUnsignedShort(packet: ByteArray, offset: Int, value: Int) {
        packet[offset] = (value ushr 8).toByte()
        packet[offset + 1] = value.toByte()
    }

    private companion object {
        const val QUERY_PACKET_SIZE = 20 + 8 + 12
    }
}
