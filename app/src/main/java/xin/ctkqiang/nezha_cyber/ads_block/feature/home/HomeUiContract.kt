package xin.ctkqiang.nezha_cyber.ads_block.feature.home

import androidx.compose.runtime.Immutable
import xin.ctkqiang.nezha_cyber.ads_block.domain.vpn.VpnSessionState

/**
 * 首页状态。
 *
 * 会话状态直接复用领域模型，不在这里再翻译成布尔开关：界面需要区分「建立中」「停止中」
 * 与「失败」，压成 isEnabled 会丢掉这些信息，用户就看不懂为什么点不动按钮。
 *
 * [authorizationRequired] 是界面态而非领域态：它记录的是「上一次启动被系统授权拦下」，
 * 属于这次交互的结果，因此不进领域模型。
 */
@Immutable
data class HomeUiState(
    val session: VpnSessionState = VpnSessionState.Stopped,
    val authorizationRequired: Boolean = false,
    val latestBlocked: LatestBlocked? = null,
)

/**
 * 最近一次被拦下的查询。
 *
 * 只在首页显示**一条**：首页回答的是「现在有没有在保护我」，完整流水属于「网络活动」页。
 * 把最后一条摆在首页是让它有实感——一个只显示开关状态的安全工具，用户无从判断它到底在不在干活。
 *
 * 不带时间戳：首页不做相对时间格式化（那需要一套本地化规则），精确时刻在网络活动页里有。
 * 少了时间也就少了「这是刚才还是昨天」的信息，因此卡片必须写明它只是最近一条、
 * 完整流水在别处，不能让用户把它当成实时读数。
 */
@Immutable
data class LatestBlocked(val host: String, val appLabel: String?)

/** 首页可派发的用户意图。 */
sealed interface HomeUiIntent {
    /** 点按主按钮：未开启则请求开启，已开启则请求停止。 */
    data object ToggleProtection : HomeUiIntent

    /** 系统授权流程返回。 */
    data class AuthorizationResult(val granted: Boolean) : HomeUiIntent
}

/**
 * 一次性效果。
 *
 * 拉起系统授权对话框是副作用而不是状态：它只该发生一次，放进 state 会导致每次重组都重新弹出。
 */
sealed interface HomeUiEffect {
    data object RequestVpnAuthorization : HomeUiEffect
}
