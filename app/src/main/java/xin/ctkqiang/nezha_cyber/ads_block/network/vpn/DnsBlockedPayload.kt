package xin.ctkqiang.nezha_cyber.ads_block.network.vpn

import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.BlockedResponseMode

private const val DNS_HEADER_SIZE = 12

private const val FLAG_MASK_RESPONSE = 0x8000

private const val FLAG_MASK_RECURSION_DESIRED = 0x0100

private const val FLAG_MASK_RECURSION_AVAILABLE = 0x0080

/** RCODE：没有错误。返回 0.0.0.0 时用它——这次解析在协议意义上是成功的。 */
private const val RESPONSE_CODE_NO_ERROR = 0

/** RCODE：域名不存在。 */
private const val RESPONSE_CODE_NXDOMAIN = 3

/** RCODE：解析器拒绝执行本次查询。 */
private const val RESPONSE_CODE_REFUSED = 5

private const val FLAGS_OFFSET = 2

private const val ANSWER_COUNT_OFFSET = 6

private const val AUTHORITY_COUNT_OFFSET = 8

private const val ADDITIONAL_COUNT_OFFSET = 10

/** 应答记录内各字段相对记录起点的偏移。 */
private const val ANSWER_NAME_OFFSET = 0

private const val ANSWER_TYPE_OFFSET = 2

private const val ANSWER_CLASS_OFFSET = 4

private const val ANSWER_TTL_OFFSET = 6

private const val ANSWER_RDLENGTH_OFFSET = 10

/** 一条 IPv4 A 记录的长度：NAME(2) + TYPE(2) + CLASS(2) + TTL(4) + RDLENGTH(2) + RDATA(4)。 */
private const val ANSWER_SIZE = 16

/**
 * 指向报文偏移 12（问题段起点）的压缩指针。
 *
 * 用它就不必把域名再拼一遍：问题段已经被完整复制过来，应答记录的名字直接指回去即可。
 */
private const val NAME_POINTER_TO_QUESTION = 0xC00C

private const val TYPE_A = 1

private const val CLASS_IN = 1

/**
 * 被拦截域名的 TTL 固定为 0，表示「不要把这条记录缓存起来」。
 *
 * 屏蔽结果必须能被立刻改回去：用户随时可能把域名加进白名单，若客户端缓存了一条 0.0.0.0，
 * 改完之后它仍会继续失败一段时间，而用户只会得出「白名单不管用」的结论。
 */
private const val BLOCKED_TTL_SECONDS = 0

private const val ANSWER_COUNT_WITH_RECORD = 1

private const val ANSWER_COUNT_WITHOUT_RECORD = 0

/**
 * 为被拦截的域名合成应答。
 *
 * 拦截时**不向上游发起查询**：既省一次往返，也让上游看不到这次查询——这既是过滤生效的直接证据，
 * 也避免把用户访问了什么泄露给解析服务。
 *
 * 合成方法是复用请求的头部与问题段，只改标志位与各段计数：
 * - QR 置 1（这是应答）、保留客户端的 RD、置 RA（本应答来自一个可递归的解析器）；
 * - RCODE 与 AN 计数按 [mode] 决定，见 [BlockedResponseMode]；
 * - NS/AR 计数一律清零——请求里可能带 EDNS0 的 OPT 记录，计数不清零就会产生
 *   「计数说有、载荷里没有」的畸形应答，客户端会直接解析失败；
 * - QDCOUNT 原样保留，问题段本来就被完整复制过来了。
 *
 * 参数非法（问题段越界）时返回 null，由调用方按「本次查询处理失败」处理，
 * 绝不返回一个半成品的应答。
 */
internal fun buildBlockedDnsPayload(
    request: ByteArray,
    questionEndOffset: Int,
    requestLength: Int,
    mode: BlockedResponseMode,
): ByteArray? {
    if (questionEndOffset < DNS_HEADER_SIZE || requestLength < questionEndOffset) return null
    val appendsAnswer = mode == BlockedResponseMode.ZeroAddress
    val payload = request.copyOf(questionEndOffset + if (appendsAnswer) ANSWER_SIZE else 0)
    writeResponseHeader(
        payload = payload,
        mode = mode,
        answerCount = if (appendsAnswer) ANSWER_COUNT_WITH_RECORD else ANSWER_COUNT_WITHOUT_RECORD,
    )
    if (appendsAnswer) {
        writeZeroAddressAnswer(payload = payload, offset = questionEndOffset)
    }
    return payload
}

private fun writeResponseHeader(payload: ByteArray, mode: BlockedResponseMode, answerCount: Int) {
    val requestFlags = Ipv4UdpFormat.readUnsignedShort(payload, FLAGS_OFFSET)
    val responseFlags = FLAG_MASK_RESPONSE or
        (requestFlags and FLAG_MASK_RECURSION_DESIRED) or
        FLAG_MASK_RECURSION_AVAILABLE or
        mode.responseCode()
    Ipv4UdpFormat.writeUnsignedShort(payload, FLAGS_OFFSET, responseFlags)
    Ipv4UdpFormat.writeUnsignedShort(payload, ANSWER_COUNT_OFFSET, answerCount)
    Ipv4UdpFormat.writeUnsignedShort(payload, AUTHORITY_COUNT_OFFSET, 0)
    Ipv4UdpFormat.writeUnsignedShort(payload, ADDITIONAL_COUNT_OFFSET, 0)
}

/**
 * 写一条 `0.0.0.0` 的 A 记录。
 *
 * RDATA 的四个字节保持数组初值 0，因此不需要显式写入——`0.0.0.0` 就是四个零字节。
 */
private fun writeZeroAddressAnswer(payload: ByteArray, offset: Int) {
    Ipv4UdpFormat.writeUnsignedShort(payload, offset + ANSWER_NAME_OFFSET, NAME_POINTER_TO_QUESTION)
    Ipv4UdpFormat.writeUnsignedShort(payload, offset + ANSWER_TYPE_OFFSET, TYPE_A)
    Ipv4UdpFormat.writeUnsignedShort(payload, offset + ANSWER_CLASS_OFFSET, CLASS_IN)
    Ipv4UdpFormat.writeInt(payload, offset + ANSWER_TTL_OFFSET, BLOCKED_TTL_SECONDS)
    Ipv4UdpFormat.writeUnsignedShort(
        payload,
        offset + ANSWER_RDLENGTH_OFFSET,
        Ipv4UdpFormat.IPV4_ADDRESS_SIZE,
    )
}

/**
 * 本次应答的 RCODE。
 *
 * `ZeroAddress` 用的是 NOERROR 而不是 NXDOMAIN：它要表达的是「解析成功，但地址不可用」。
 * 若配上 NXDOMAIN，客户端会忽略应答段里的 A 记录，那样这条记录就白写了。
 */
private fun BlockedResponseMode.responseCode(): Int = when (this) {
    BlockedResponseMode.NxDomain -> RESPONSE_CODE_NXDOMAIN

    BlockedResponseMode.Refused -> RESPONSE_CODE_REFUSED

    BlockedResponseMode.ZeroAddress -> RESPONSE_CODE_NO_ERROR
}
