package xin.ctkqiang.nezha_cyber.ads_block

import android.content.Context
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xin.ctkqiang.nezha_cyber.ads_block.data.analysis.AndroidApkAnalyzer
import xin.ctkqiang.nezha_cyber.ads_block.data.application.FileProtectedApplicationStore
import xin.ctkqiang.nezha_cyber.ads_block.data.application.PackageManagerApplicationSource
import xin.ctkqiang.nezha_cyber.ads_block.data.application.PackageManagerPermissionSource
import xin.ctkqiang.nezha_cyber.ads_block.data.database.NezhaDatabase
import xin.ctkqiang.nezha_cyber.ads_block.data.database.NezhaDatabaseFactory
import xin.ctkqiang.nezha_cyber.ads_block.data.notification.RoomNotificationRuleStore
import xin.ctkqiang.nezha_cyber.ads_block.data.observation.FileObservationStore
import xin.ctkqiang.nezha_cyber.ads_block.data.privacy.FilePrivacyPolicyStore
import xin.ctkqiang.nezha_cyber.ads_block.data.rule.AppAdCatalogReader
import xin.ctkqiang.nezha_cyber.ads_block.data.rule.BuiltinDomainCatalog
import xin.ctkqiang.nezha_cyber.ads_block.data.rule.FileKeywordPolicyStore
import xin.ctkqiang.nezha_cyber.ads_block.data.rule.FileRuleStore
import xin.ctkqiang.nezha_cyber.ads_block.domain.analysis.ApkAnalyzer
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.ApplicationPermissionSource
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.InstalledApplicationSource
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.ProtectedApplicationStore
import xin.ctkqiang.nezha_cyber.ads_block.domain.notification.NotificationAccessSource
import xin.ctkqiang.nezha_cyber.ads_block.domain.notification.NotificationRuleEngine
import xin.ctkqiang.nezha_cyber.ads_block.domain.notification.NotificationRuleStore
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.ObservationStore
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.PrivacyPolicyStore
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.AppAdRule
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.DomainRuleEngine
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.KeywordBlockingPolicy
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.KeywordPolicyStore
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleEngine
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleStore
import xin.ctkqiang.nezha_cyber.ads_block.notification.platform.AndroidNotificationAccessSource
import xin.ctkqiang.nezha_cyber.ads_block.ui.application.ApplicationIconLoader
import xin.ctkqiang.nezha_cyber.ads_block.ui.application.PackageManagerIconLoader

private const val STORAGE_DIRECTORY_NAME = "filtering"

/**
 * 应用级容器：唯一允许把端口与适配器装配起来的地方（工程规则第 38.1 节）。
 *
 * 它是普通类而不是单例对象，由 [NezhaApplication] 持有一份，因此不存在静态可变状态；
 * 服务与界面都从 Application 取同一个实例，天然共享同一份存储与规则索引。
 *
 * 生命周期归 Application 所有：进程结束即结束，所以 [containerScope] 不需要取消。
 */
class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext

    private val storageDirectory = File(applicationContext.filesDir, STORAGE_DIRECTORY_NAME)

    private val containerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val ruleStore: RuleStore = FileRuleStore(storageDirectory)

    /**
     * 隐私与安全策略。声明在观测存储之前：观测存储要用它决定「是否记录」与「保留多少」，
     * 而这个引用发生在属性初始化阶段，顺序写反就是空指针。
     */
    val privacyPolicyStore: PrivacyPolicyStore = FilePrivacyPolicyStore(storageDirectory)

    val observationStore: ObservationStore = FileObservationStore(
        storageDirectory = storageDirectory,
        scope = containerScope,
        privacyProvider = { privacyPolicyStore.policy.value },
    )

    val installedApplicationSource: InstalledApplicationSource = PackageManagerApplicationSource(applicationContext)

    val protectedApplicationStore: ProtectedApplicationStore = FileProtectedApplicationStore(storageDirectory)

    val applicationPermissionSource: ApplicationPermissionSource = PackageManagerPermissionSource(applicationContext)

    val apkAnalyzer: ApkAnalyzer = AndroidApkAnalyzer(applicationContext)

    /**
     * 应用图标加载器。
     *
     * 图标是界面资源而不是领域概念（第 41.8 节），因此它对应的是界面层端口；但它的实现需要
     * `PackageManager`，装配点只能是应用模块，所以由容器创建并在组合根下发给界面。
     */
    val applicationIconLoader: ApplicationIconLoader = PackageManagerIconLoader(applicationContext)

    /**
     * 关键词兜底策略。
     *
     * 它有一份独立的持久化存储，因为「开关 + 一组标签」不是域名规则能表达的形态。
     * 用户改动后立即写盘，因此不需要在这里再做一次同步。
     */
    val keywordPolicyStore: KeywordPolicyStore = FileKeywordPolicyStore(storageDirectory)

    /** 数据库用 `by lazy` 打开：打开它要读磁盘，不该发生在应用冷启动的路径上。 */
    private val database: NezhaDatabase by lazy { NezhaDatabaseFactory.create(applicationContext) }

    /**
     * 通知拦截规则的存储。
     *
     * 这是**第一个接数据库的存储**，其余存储仍在读文本文件；把它们搬进数据库是既定的后续工作，
     * 数据库本身（表结构、DAO、schema 导出）已经就位。
     */
    val notificationRuleStore: NotificationRuleStore by lazy { RoomNotificationRuleStore(database) }

    /** 通知访问权限。用户只能在系统设置里授予，因此这里只有查询与刷新。 */
    val notificationAccessSource: NotificationAccessSource = AndroidNotificationAccessSource(applicationContext)

    /**
     * 通知规则引擎随规则快照重建。
     *
     * 与域名引擎同样的理由用 `stateIn`：建索引要按包名分组，绝不能每来一条通知重建一次。
     * 通知监听服务只读 `value`，因此始终拿到一个完整索引；预热完成前它是空引擎（一律放行），
     * 这个中间态是安全的——宁可少拦，不可误拦。
     */
    val notificationRuleEngine: StateFlow<NotificationRuleEngine> = notificationRuleStore.rules
        .map { rules -> NotificationRuleEngine.from(rules) }
        .stateIn(
            scope = containerScope,
            started = SharingStarted.Eagerly,
            initialValue = NotificationRuleEngine.Empty,
        )

    /**
     * 应用专属广告清单（assets/app_ads.txt，第 44 节）。
     *
     * 它不落持久层、也没有用户覆写，因此只是一个由 [warmUp] 一次性填充的内存状态：
     * 与全局内置清单不同，这里不需要版本号，也不需要合并语义。
     */
    private val appAdRules = MutableStateFlow<List<AppAdRule>>(emptyList())

    private val appAdCatalogReader = AppAdCatalogReader(applicationContext.assets)

    /**
     * 规则引擎随规则快照、关键词策略或应用专属清单变化重建。
     *
     * 用 `stateIn` 而不是每次查询现建：清单有数万条，重建索引是毫秒级的，可以接受；
     * 但绝不能在每个 DNS 查询上重建。中继只读 [StateFlow.value]，因此始终拿到一个完整索引。
     *
     * 三个上游都必须合并进来：只订阅其中一部分，就会让另一处的改动在下一次重建之前不生效，
     * 表现为「刚加的关键词不管用，直到重启」。
     */
    val ruleEngine: StateFlow<RuleEngine> = combine(
        ruleStore.snapshot,
        keywordPolicyStore.policy,
        appAdRules,
    ) { snapshot, keywordPolicy, appAdCatalog ->
        DomainRuleEngine(snapshot.rules, keywordPolicy, appAdCatalog)
    }.stateIn(
        scope = containerScope,
        started = SharingStarted.Eagerly,
        initialValue = DomainRuleEngine(emptyList(), KeywordBlockingPolicy.Default, emptyList()),
    )

    /**
     * 后台预热。
     *
     * 不阻塞启动路径：预热完成前中继拿到的是空规则集，也就是「不拦任何东西」——
     * 这个中间态是安全的（宁可少拦，不可误拦）。预热完成后引擎自然切换过去。
     */
    fun warmUp() {
        containerScope.launch {
            ruleStore.load()
            keywordPolicyStore.load()
            privacyPolicyStore.load()
            val catalog = BuiltinDomainCatalog(applicationContext.assets).readCatalog()
            ruleStore.applyBuiltinCatalog(catalog.hosts, catalog.version)
            appAdRules.value = appAdCatalogReader.readCatalog()
            protectedApplicationStore.load()
            observationStore.load()
            notificationRuleStore.load()
        }
    }
}
