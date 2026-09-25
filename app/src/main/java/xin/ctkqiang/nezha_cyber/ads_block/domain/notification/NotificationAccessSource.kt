package xin.ctkqiang.nezha_cyber.ads_block.domain.notification

import kotlinx.coroutines.flow.StateFlow

/**
 * 端口：通知访问权限（用户是否允许本应用读取通知）。
 *
 * 权限由用户在系统设置里授予，应用**无法自行请求**，也拿不到「申请」的对话框——
 * 只能在界面里说明用途并引导用户去设置页。因此这个端口刻意只有一个状态和一个刷新动作，
 * 没有「请求权限」这种不存在的能力。
 *
 * `refresh()` 必要是因为用户是去别的界面开权限：应用重新回到前台时状态可能已经变了，
 * 而这个变化不会有任何回调通知我们。
 */
interface NotificationAccessSource {
    val isGranted: StateFlow<Boolean>

    fun refresh()
}
