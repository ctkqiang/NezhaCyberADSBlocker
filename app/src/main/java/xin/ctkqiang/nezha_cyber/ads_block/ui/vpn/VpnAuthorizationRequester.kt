package xin.ctkqiang.nezha_cyber.ads_block.ui.vpn

/**
 * 系统 VPN 授权对话框的请求桥。
 *
 * 授权对话框只能由持有 Activity 的组件拉起，ViewModel 与领域层都拿不到这个能力，因此把它建模成
 * 回调式函数接口，由组合根（MainActivity）提供实现。界面层因此既不依赖 Context，也不需要
 * 在 Composable 里拼装平台 Intent（工程规则第 40.3、40.4 节）。
 */
fun interface VpnAuthorizationRequester {
    /**
     * 拉起授权流程。
     *
     * @param onResult 用户完成授权流程后回调；true 表示已获得授权。回调保证在授权流程结束后
     * 恰好触发一次，界面据此继续或中止启动。
     */
    fun request(onResult: (Boolean) -> Unit)
}
