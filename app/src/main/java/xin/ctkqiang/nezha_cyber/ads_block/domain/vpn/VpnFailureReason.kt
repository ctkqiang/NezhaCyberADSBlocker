package xin.ctkqiang.nezha_cyber.ads_block.domain.vpn

/**
 * VPN 启动失败的领域原因。
 *
 * 只描述「为什么失败」。平台异常、错误码与描述符状态属于适配器细节，不得泄漏到领域层
 * （工程规则第 38.3 节），因此这里不出现任何平台类型。
 */
enum class VpnFailureReason {
    /** 隧道建立失败：地址、路由或描述符不可用。 */
    TunnelEstablishmentFailed,

    /** 系统不允许启动前台服务，通常是通知不可见或服务类型不被允许。 */
    ForegroundServiceUnavailable,

    /**
     * 连续多次无法从上游取回解析结果，隧道已主动断开。
     *
     * 这不是普通失败，而是保护机制生效：隧道把域名解析接管在手里，一旦转发能力失效，
     * 继续持有会让整机解析停摆。断开即恢复系统默认解析，正常上网不受影响。
     */
    UpstreamUnreachable,
}
