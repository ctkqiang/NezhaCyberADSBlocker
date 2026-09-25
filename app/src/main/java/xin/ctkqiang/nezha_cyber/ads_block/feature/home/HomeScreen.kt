package xin.ctkqiang.nezha_cyber.ads_block.feature.home

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import xin.ctkqiang.nezha_cyber.ads_block.R
import xin.ctkqiang.nezha_cyber.ads_block.domain.vpn.VpnFailureReason
import xin.ctkqiang.nezha_cyber.ads_block.domain.vpn.VpnSessionState
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaPrimaryButton
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaProtectionRing
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaScreenScaffold
import xin.ctkqiang.nezha_cyber.ads_block.ui.component.NezhaSurfaceCard
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaDimens
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaTheme
import xin.ctkqiang.nezha_cyber.ads_block.ui.theme.NezhaThemePreview
import xin.ctkqiang.nezha_cyber.ads_block.ui.vpn.LocalVpnAuthorizationRequester
import xin.ctkqiang.nezha_cyber.ads_block.ui.vpn.LocalVpnController
import xin.ctkqiang.nezha_cyber.ads_block.ui.vpn.NezhaVpnPreviewHost

/**
 * 首页：隧道状态与启停。
 *
 * 页面本身不持有状态，只渲染 [HomeUiState] 并派发意图。授权属于一次性副作用，通过
 * [HomeUiEffect] 交给组合根提供的授权桥执行，因此本文件不出现任何平台类型。
 */
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val vpnController = LocalVpnController.current
    val authorizationRequester = LocalVpnAuthorizationRequester.current
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(vpnController))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                HomeUiEffect.RequestVpnAuthorization ->
                    authorizationRequester.request { granted ->
                        viewModel.dispatch(HomeUiIntent.AuthorizationResult(granted))
                    }
            }
        }
    }
    NezhaScreenScaffold(modifier = modifier) {
        HomeContent(uiState = uiState, onIntent = viewModel::dispatch)
    }
}

@Composable
private fun ColumnScope.HomeContent(uiState: HomeUiState, onIntent: (HomeUiIntent) -> Unit) {
    val palette = NezhaTheme.palette
    Spacer(modifier = Modifier.height(NezhaDimens.sectionGap))
    NezhaProtectionRing(
        active = uiState.session.isActive,
        title = statusTitle(uiState.session),
        subtitle = statusCaption(uiState.session),
        modifier = Modifier.align(Alignment.CenterHorizontally),
    )
    Spacer(modifier = Modifier.height(NezhaDimens.sectionGap))
    NezhaPrimaryButton(
        text = actionLabel(uiState.session),
        enabled = uiState.session.isSettled,
        onClick = { onIntent(HomeUiIntent.ToggleProtection) },
    )
    hintText(uiState)?.let { hint ->
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        BasicText(
            text = hint,
            style = NezhaTheme.typography.caption.copy(color = palette.brand),
        )
    }
    Spacer(modifier = Modifier.height(NezhaDimens.sectionGap))
    HomeScopeCard()
}

@Composable
private fun HomeScopeCard() {
    val palette = NezhaTheme.palette
    NezhaSurfaceCard {
        BasicText(
            text = stringResource(R.string.home_scope_title),
            style = NezhaTheme.typography.title.copy(color = palette.textPrimary),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        BasicText(
            text = stringResource(R.string.home_scope_value),
            style = NezhaTheme.typography.label.copy(color = palette.brand),
        )
        Spacer(modifier = Modifier.height(NezhaDimens.blockGap))
        BasicText(
            text = stringResource(R.string.home_scope_note),
            style = NezhaTheme.typography.caption.copy(color = palette.textSecondary),
        )
    }
}

/** 过渡态不允许再次点按，否则会出现「停止中又请求启动」这类互相打断的指令。 */
private val VpnSessionState.isSettled: Boolean
    get() = this !is VpnSessionState.Starting && this !is VpnSessionState.Stopping

@Composable
private fun statusTitle(session: VpnSessionState): String = when (session) {
    VpnSessionState.Stopped -> stringResource(R.string.home_status_unprotected)
    VpnSessionState.Starting -> stringResource(R.string.home_status_starting)
    is VpnSessionState.Running -> stringResource(R.string.home_status_protected)
    VpnSessionState.Stopping -> stringResource(R.string.home_status_stopping)
    is VpnSessionState.Failed -> stringResource(R.string.home_status_failed)
}

@Composable
private fun statusCaption(session: VpnSessionState): String = when (session) {
    VpnSessionState.Stopped -> stringResource(R.string.home_caption_unprotected)
    VpnSessionState.Starting -> stringResource(R.string.home_caption_starting)
    is VpnSessionState.Running -> stringResource(R.string.home_caption_protected)
    VpnSessionState.Stopping -> stringResource(R.string.home_caption_stopping)
    is VpnSessionState.Failed -> stringResource(R.string.home_caption_failed)
}

@Composable
private fun actionLabel(session: VpnSessionState): String = when (session) {
    VpnSessionState.Stopped, is VpnSessionState.Failed -> stringResource(R.string.home_action_start)
    is VpnSessionState.Running -> stringResource(R.string.home_action_stop)
    VpnSessionState.Starting, VpnSessionState.Stopping -> stringResource(R.string.home_action_busy)
}

@Composable
private fun hintText(uiState: HomeUiState): String? {
    if (uiState.authorizationRequired) {
        return stringResource(R.string.home_hint_authorization)
    }
    return when (val session = uiState.session) {
        is VpnSessionState.Failed -> when (session.reason) {
            VpnFailureReason.TunnelEstablishmentFailed -> stringResource(R.string.home_hint_tunnel_failed)
            VpnFailureReason.ForegroundServiceUnavailable ->
                stringResource(R.string.home_hint_foreground_unavailable)
            VpnFailureReason.UpstreamUnreachable -> stringResource(R.string.home_hint_upstream_unreachable)
        }
        else -> null
    }
}

@Preview(name = "首页 · 未保护", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun HomeScreenIdlePreview() {
    NezhaThemePreview {
        NezhaVpnPreviewHost(state = VpnSessionState.Stopped) {
            HomeScreen()
        }
    }
}

@Preview(name = "首页 · 已保护", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun HomeScreenProtectedPreview() {
    NezhaThemePreview {
        NezhaVpnPreviewHost(state = VpnSessionState.Running(Instant.EPOCH)) {
            HomeScreen()
        }
    }
}
