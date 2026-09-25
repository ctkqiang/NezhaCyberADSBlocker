package xin.ctkqiang.nezha_cyber.ads_block.domain.application

/**
 * 一个已安装并且用户能认出来的应用。
 *
 * 只有包名与显示名，不含图标等平台资源：按工程规则第 41.8 节，图标与标签属于平台资源，
 * 既不事件溯源也不进入领域模型，界面按需自行获取。
 */
data class InstalledApplication(val packageName: String, val label: String)
