package xin.ctkqiang.nezha_cyber.ads_block.network.vpn

import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.ObservationStore
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.BlockedResponseMode
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleEngine

/**
 * 中继在运行期间读取的四项外部输入。
 *
 * 打包成一个参数而不是让构造函数收下四项：它们属于同一类东西——**每个包都要读一次，
 * 而且会随用户操作变化**。规则、隐私策略、归属反查都可能在隧道运行中被改动，
 * 因此一律用提供者按需读取，绝不在建立隧道时快照。
 *
 * 快照的后果是「刚改的设置要重启才生效」，那是最难向用户解释的一类问题；
 * 第 9、15 节也正是要求过滤决策与观测都在运行时取自当前状态。
 */
internal class RelayInputs(
    val ruleEngine: () -> RuleEngine,
    val blockedResponseMode: () -> BlockedResponseMode,
    val observations: ObservationStore,
    val attributePackage: (DnsQuery) -> String?,
)
