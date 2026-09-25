package xin.ctkqiang.nezha_cyber.ads_block.domain.observation

import kotlinx.coroutines.flow.StateFlow

/**
 * 端口：域名观测记录与统计。
 *
 * [record] 刻意不是 suspend 函数：它在中继线程上被每个 DNS 查询调用，
 * 必须立刻返回。实现应把写入排入队列、把统计放在内存里累计，落盘异步完成。
 * 若把它做成 suspend，等于要求调用方在每个数据包上等待一次磁盘写入。
 */
interface ObservationStore {
    /** 累计统计。状态端口，用 StateFlow（工程规则第 37.5 节）。 */
    val statistics: StateFlow<FilteringStatistics>

    /** 最近若干条观测，最新的在前。用于「网络活动」页。 */
    val recent: StateFlow<List<DomainObservation>>

    /** 从持久层重建统计与最近记录。 */
    suspend fun load()

    /** 记录一次观测。必须快速返回。 */
    fun record(observation: DomainObservation)

    /**
     * 清空累计统计与最近观测。
     *
     * 只动观测数据：规则、关键词策略与逐应用选择都不受影响，它们各有自己的存储。
     * 清空是用户明确要求才发生的动作，因此允许它是挂起函数、允许等一次磁盘写入。
     */
    suspend fun clear()
}
