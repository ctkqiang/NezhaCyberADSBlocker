package xin.ctkqiang.nezha_cyber.ads_block

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import xin.ctkqiang.nezha_cyber.ads_block.network.vpn.AndroidVpnController
import xin.ctkqiang.nezha_cyber.ads_block.ui.vpn.LocalVpnAuthorizationRequester
import xin.ctkqiang.nezha_cyber.ads_block.ui.vpn.LocalVpnController
import xin.ctkqiang.nezha_cyber.ads_block.ui.vpn.VpnAuthorizationRequester

/**
 * VPN 相关的组合根。
 *
 * 应用模块是唯一允许把领域端口与平台实现装配起来的地方（工程规则第 38.1 节），这里就是那个点：
 * - [LocalVpnController] 注入网络层实现；
 * - [LocalVpnAuthorizationRequester] 把「拉起系统授权对话框」这件事封装成界面层的回调。
 *
 * 授权流程分两步，顺序不能颠倒：
 * 1. Android 13 起 POST_NOTIFICATIONS 是运行时权限，未授予时前台服务的通知不会显示，
 *    用户将看不到 VPN 正在运行——对隧道类应用这不可接受，因此先请求它；
 * 2. 再用 `VpnService.prepare` 拉起系统 VPN 授权对话框。
 * 用户拒绝通知权限不阻断隧道：隧道本身可用，只是通知不可见，因此第 2 步照常执行。
 */
@Composable
internal fun VpnCompositionRoot(vpnController: AndroidVpnController, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val pendingResult = remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        pendingResult.value?.invoke(result.resultCode == Activity.RESULT_OK)
        pendingResult.value = null
    }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        requestVpnConsent(context, consentLauncher, pendingResult)
    }
    val authorizationRequester = remember {
        VpnAuthorizationRequester { onResult ->
            pendingResult.value = onResult
            if (requiresNotificationPermission(context)) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                requestVpnConsent(context, consentLauncher, pendingResult)
            }
        }
    }
    CompositionLocalProvider(
        LocalVpnController provides vpnController,
        LocalVpnAuthorizationRequester provides authorizationRequester,
        content = content,
    )
}

private fun requiresNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
    return granted != PackageManager.PERMISSION_GRANTED
}

/**
 * `VpnService.prepare` 返回 null 表示已经授权，此时直接回调成功；
 * 否则把系统对话框交给 launcher，结果由它回填。
 */
private fun requestVpnConsent(
    context: Context,
    launcher: ActivityResultLauncher<Intent>,
    pendingResult: MutableState<((Boolean) -> Unit)?>,
) {
    val consentIntent = VpnService.prepare(context)
    if (consentIntent != null) {
        launcher.launch(consentIntent)
        return
    }
    pendingResult.value?.invoke(true)
    pendingResult.value = null
}
