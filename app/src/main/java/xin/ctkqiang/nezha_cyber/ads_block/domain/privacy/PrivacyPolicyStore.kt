package xin.ctkqiang.nezha_cyber.ads_block.domain.privacy

import kotlinx.coroutines.flow.StateFlow

/**
 * 端口：隐私与安全策略的读取与编辑。
 *
 * 读取方有三处，各自只取自己在意的那一项：观测存储（是否记录、保留多少）、
 * DNS 中继（阻断时回什么应答）、设置页与流水页（展示当前状态）。
 * 三者订阅的是同一个 StateFlow，因此设置页刚改的开关立即对隧道生效，不需要重启服务。
 */
interface PrivacyPolicyStore {
    val policy: StateFlow<PrivacyPolicy>

    /** 从持久层载入。文件缺失或全部键都无法识别时保持默认策略（工程规则第 29 节）。 */
    suspend fun load()

    suspend fun setObservationLoggingEnabled(enabled: Boolean)

    suspend fun setObservationRetention(retention: ObservationRetention)

    suspend fun setBlockedResponseMode(mode: BlockedResponseMode)
}
