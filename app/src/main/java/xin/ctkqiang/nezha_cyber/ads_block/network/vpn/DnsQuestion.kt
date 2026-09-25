package xin.ctkqiang.nezha_cyber.ads_block.network.vpn

private const val DNS_HEADER_SIZE = 12

private const val DNS_COMPRESSION_MASK = 0xC0

/** QTYPE(2 字节) + QCLASS(2 字节)。 */
private const val QUESTION_TAIL_SIZE = 4

private const val MAX_DOMAIN_LENGTH = 253

private const val LABEL_SEPARATOR = '.'

/**
 * DNS 问题段。
 *
 * [endOffset] 是问题段结束的位置（QNAME + QTYPE + QCLASS），构造应答时只需要复制这么多字节。
 */
internal class DnsQuestion(val name: String, val endOffset: Int)

/**
 * 从 DNS 载荷里读出第一个问题的域名。
 *
 * 这是不可信输入（工程规则第 21 节）：长度前缀可能超出载荷、标签可能超长、名称可能无限长。
 * 任何一条不满足就返回 null，由调用方按「无法判定」处理——**继续转发**，而不是丢弃。
 * 丢弃是这里最危险的选择：一个解析不了的畸形包不该让某个应用的解析停摆。
 *
 * 压缩指针（0xC0 前缀）在问题段本不合法，出现即判为畸形，不做指针跳转：
 * 一旦允许跳转就必须防环，而问题段永远不需要它。
 *
 * 这里豁免 ReturnCount：逐字节解析必须能在任意一步拒绝，单个出口的写法会引入多层嵌套反而更难审，
 * 因此除最后构造结果外全部采用守卫式早返回。
 */
@Suppress("ReturnCount")
internal fun readQuestion(payload: ByteArray, length: Int): DnsQuestion? {
    if (length <= DNS_HEADER_SIZE) return null
    val builder = StringBuilder()
    var offset = DNS_HEADER_SIZE
    while (true) {
        if (offset >= length) return null
        val labelLength = payload[offset].toInt() and Ipv4UdpFormat.BYTE_MASK
        if (labelLength and DNS_COMPRESSION_MASK == DNS_COMPRESSION_MASK) return null
        offset += 1
        if (labelLength == 0) break
        if (offset + labelLength > length) return null
        if (builder.isNotEmpty()) builder.append(LABEL_SEPARATOR)
        for (index in 0 until labelLength) {
            builder.append((payload[offset + index].toInt() and Ipv4UdpFormat.BYTE_MASK).toChar())
        }
        offset += labelLength
        if (builder.length > MAX_DOMAIN_LENGTH) return null
    }
    val endOffset = offset + QUESTION_TAIL_SIZE
    if (endOffset > length) return null
    if (builder.isEmpty()) return null
    return DnsQuestion(name = builder.toString(), endOffset = endOffset)
}
