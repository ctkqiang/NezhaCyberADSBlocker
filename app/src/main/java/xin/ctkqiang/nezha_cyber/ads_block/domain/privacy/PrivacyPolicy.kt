package xin.ctkqiang.nezha_cyber.ads_block.domain.privacy

/**
 * 隐私与安全策略。用户在设置页里改的就是它。
 *
 * [isObservationLoggingEnabled] 是这一组里唯一一个「不收集」的开关（工程规则第 20 节要求
 * 不做不必要的收集）：关掉之后不再记录任何域名观测与累计计数，但**隧道照常过滤与拦截**——
 * 过滤只依赖规则引擎，与是否留记录完全无关。这一点必须成立，否则关掉隐私开关就等于
 * 关掉了整个保护能力，用户只能二选一。
 *
 * 关掉记录**不会删除已有记录**：删除是另一个明确动作（统计页的「清除统计」）。
 * 把「不再记录」和「删掉旧的」合成一个动作，会让用户在只想停止收集时丢掉已有的诊断数据。
 *
 * 三个字段都有默认值，因此解析一份只有部分键的配置文件时，缺失项各自退回默认值。
 */
data class PrivacyPolicy(
    val isObservationLoggingEnabled: Boolean = true,
    val observationRetention: ObservationRetention = ObservationRetention.Standard,
    val blockedResponseMode: BlockedResponseMode = BlockedResponseMode.NxDomain,
) {
    companion object {
        val Default = PrivacyPolicy()
    }
}
