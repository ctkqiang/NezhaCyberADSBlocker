package xin.ctkqiang.nezha_cyber.ads_block.ui.analysis

import xin.ctkqiang.nezha_cyber.ads_block.domain.analysis.ApkSource

/**
 * 让用户挑一个 APK 文件。
 *
 * 与 VPN 授权桥同样的理由：文件选择器只能由 Activity 拉起，而 ViewModel 拿不到 Activity，
 * 因此把这件事封装成界面层的回调，ViewModel 只发一次性效果（工程规则第 40.1、40.4 节）。
 *
 * 回调参数为 null 表示用户取消了选择——取消是正常路径，不是失败。
 */
fun interface ApkPicker {
    fun pick(onPicked: (ApkSource?) -> Unit)
}
