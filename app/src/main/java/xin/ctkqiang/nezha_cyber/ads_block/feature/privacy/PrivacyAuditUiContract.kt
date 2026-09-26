package xin.ctkqiang.nezha_cyber.ads_block.feature.privacy

/**
 * 隐私与传感器页的状态、行模型与意图。
 *
 * 三者属于同一个封闭层次，因此共用一个文件（工程规则第 40.2 节，与 `ApkAnalysisUiContract`
 * 同样的处理）。本页没有一次性效果：跳转系统设置由界面直接调用注入的端口完成，
 * 与「通知拦截规则」页一致，不经过状态也不经过效果——把它塞进状态会在每次重组时重复拉起。
 *
 * [unreadableApplicationCount] 与 [hasPackageVisibility] 是刻意保留的：
 * 权限读不到、或应用列表被包可见性过滤时，本页的结果是**不完整**的。不把这件事说出来，
 * 用户会把「只列了三个应用」当成「只有三个应用有问题」——那正是这一页最容易给出的假安心。
 */
data class PrivacyAuditUiState(
    val isScanning: Boolean = true,
    val entries: List<PrivacyAuditRow> = emptyList(),
    val scannedApplicationCount: Int = 0,
    val unreadableApplicationCount: Int = 0,
    val hasPackageVisibility: Boolean = true,
)

/**
 * 一行审计结果。
 *
 * [permissionLabels] 用的是系统给出的权限名称（与系统设置里的条目同名），不是我们自己起的
 * 名字：这一页的作用是把用户送到系统设置去关掉它，两边叫法一致才不会找错条目。
 * 标签去重后展示，而 [grantedCount] 保留真实条数——同一个标签可能对应多项权限
 * （例如读写通讯录），去重会让计数偏小，二者必须分开。
 */
data class PrivacyAuditRow(
    val packageName: String,
    val appLabel: String,
    val grantedCount: Int,
    val permissionLabels: List<String>,
)

/** 本页唯一需要经过 ViewModel 的意图：重扫。跳转系统设置由界面直接调用端口。 */
sealed interface PrivacyAuditUiIntent {
    data object Rescan : PrivacyAuditUiIntent
}
