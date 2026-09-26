package xin.ctkqiang.nezha_cyber.ads_block.ui.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xin.ctkqiang.nezha_cyber.ads_block.domain.analysis.ApkAnalysisOutcome
import xin.ctkqiang.nezha_cyber.ads_block.domain.analysis.ApkAnalysisResult
import xin.ctkqiang.nezha_cyber.ads_block.domain.analysis.ApkAnalyzer
import xin.ctkqiang.nezha_cyber.ads_block.domain.analysis.ApkSource
import xin.ctkqiang.nezha_cyber.ads_block.domain.analysis.DomainCandidate
import xin.ctkqiang.nezha_cyber.ads_block.domain.appearance.ThemePreference
import xin.ctkqiang.nezha_cyber.ads_block.domain.appearance.ThemePreferenceStore
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.ApplicationPermission
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.ApplicationPermissionSource
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.InstalledApplication
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.InstalledApplicationSource
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.ProtectedApplicationStore
import xin.ctkqiang.nezha_cyber.ads_block.domain.notification.NotificationAccessSource
import xin.ctkqiang.nezha_cyber.ads_block.domain.notification.NotificationRule
import xin.ctkqiang.nezha_cyber.ads_block.domain.notification.NotificationRuleEditResult
import xin.ctkqiang.nezha_cyber.ads_block.domain.notification.NotificationRuleId
import xin.ctkqiang.nezha_cyber.ads_block.domain.notification.NotificationRuleStore
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.DomainObservation
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.FilteringStatistics
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.ObservationStore
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.BlockedResponseMode
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.ObservationRetention
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.PrivacyPolicy
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.PrivacyPolicyStore
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.DomainRule
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.KeywordBlockingPolicy
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.KeywordPolicyStore
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleAction
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleEditResult
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleSnapshot
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleSource
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleStore
import xin.ctkqiang.nezha_cyber.ads_block.ui.application.ApplicationIconLoader
import xin.ctkqiang.nezha_cyber.ads_block.ui.application.LocalApplicationIconLoader
import xin.ctkqiang.nezha_cyber.ads_block.ui.notification.LocalNotificationAccessLauncher
import xin.ctkqiang.nezha_cyber.ads_block.ui.notification.NotificationAccessLauncher
import xin.ctkqiang.nezha_cyber.ads_block.ui.privacy.LocalSystemSettingsLauncher
import xin.ctkqiang.nezha_cyber.ads_block.ui.privacy.SystemSettingsLauncher

/**
 * 数据端口的预览装配壳。
 *
 * 把端口的替身集中在这一处，页面 @Preview 只需包一层。替身全部是内存实现，
 * 不碰文件系统、不碰包管理服务，因此预览不会因为权限或磁盘状态而变化。
 *
 * 每个替身都带少量种数据：空白页面看不出排版是否正确，而有真实形状的数据才能暴露换行、
 * 截断与对齐问题。
 *
 * 两个开关是刻意留出来的：通知使用权未开启与已开启是**两套完全不同的页面**
 * （前者多一张引导卡片），而权限状态在预览里无法靠系统设置改变，只能注入。
 * 通知规则同时给一条启用、一条禁用，这样列表里两种行的观感差异在预览中就能看出来。
 */
@Composable
internal fun NezhaDataPreviewHost(
    protectedPackages: Set<String> = setOf(PREVIEW_BROWSER_PACKAGE),
    isNotificationAccessGranted: Boolean = false,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalRuleStore provides PreviewRuleStore(),
        LocalKeywordPolicyStore provides PreviewKeywordPolicyStore(),
        LocalPrivacyPolicyStore provides PreviewPrivacyPolicyStore(),
        LocalObservationStore provides PreviewObservationStore(),
        LocalInstalledApplicationSource provides PreviewInstalledApplicationSource(),
        LocalApplicationPermissionSource provides PreviewApplicationPermissionSource(),
        LocalProtectedApplicationStore provides PreviewProtectedApplicationStore(protectedPackages),
        LocalNotificationRuleStore provides PreviewNotificationRuleStore(),
        LocalNotificationAccessSource provides PreviewNotificationAccessSource(isNotificationAccessGranted),
        LocalNotificationAccessLauncher provides PreviewNotificationAccessLauncher(),
        LocalSystemSettingsLauncher provides PreviewSystemSettingsLauncher(),
        LocalApkAnalyzer provides PreviewApkAnalyzer(),
        LocalApplicationIconLoader provides PreviewApplicationIconLoader(),
        LocalThemePreferenceStore provides PreviewThemePreferenceStore(),
        content = content,
    )
}

/**
 * 预览用的通知规则。
 *
 * 规则按 id 升序排列，与持久层的契约一致（引擎在多条命中时取第一条，顺序不能是随机的）。
 * 给一条启用、一条禁用：界面上「已启用」与「已停用」的行看起来必须能一眼分开。
 */
private class PreviewNotificationRuleStore : NotificationRuleStore {
    private val mutableRules = MutableStateFlow(
        listOf(
            NotificationRule(
                id = NotificationRuleId(1L),
                packageName = PREVIEW_SHOPPING_PACKAGE,
                matchText = "优惠",
                enabled = true,
            ),
            NotificationRule(
                id = NotificationRuleId(2L),
                packageName = PREVIEW_CHAT_PACKAGE,
                matchText = "广告",
                enabled = false,
            ),
        ),
    )

    override val rules: StateFlow<List<NotificationRule>> = mutableRules.asStateFlow()

    override suspend fun load() = Unit

    override suspend fun create(packageName: String, matchText: String): NotificationRuleEditResult =
        NotificationRuleEditResult.Applied

    override suspend fun update(
        id: NotificationRuleId,
        packageName: String,
        matchText: String,
    ): NotificationRuleEditResult = NotificationRuleEditResult.Applied

    override suspend fun setEnabled(id: NotificationRuleId, enabled: Boolean): NotificationRuleEditResult =
        NotificationRuleEditResult.Applied

    override suspend fun delete(id: NotificationRuleId): NotificationRuleEditResult = NotificationRuleEditResult.Applied
}

/** 预览用的通知使用权。真实实现要问系统，预览里只能由调用方给定。 */
private class PreviewNotificationAccessSource(isGranted: Boolean) : NotificationAccessSource {
    private val mutableIsGranted = MutableStateFlow(isGranted)

    override val isGranted: StateFlow<Boolean> = mutableIsGranted.asStateFlow()

    override fun refresh() = Unit
}

/** 预览用的设置页跳转。预览环境里没有系统设置，因此什么都不做——但它必须存在，否则预览会崩。 */
private class PreviewNotificationAccessLauncher : NotificationAccessLauncher {
    override fun launch() = Unit
}

/**
 * 预览用的系统隐私设置跳转。
 *
 * [canOpenPrivacyDashboard] 默认给 true，这样预览里看到的是带按钮的那一版；它决定了
 * 「打开隐私仪表盘」是按可用按钮呈现还是替换成一行说明，两种排版都需要能看到。
 */
private class PreviewSystemSettingsLauncher(dashboardAvailable: Boolean = true) : SystemSettingsLauncher {
    override val canOpenPrivacyDashboard: Boolean = dashboardAvailable

    override fun openApplicationSettings(packageName: String) = Unit

    override fun openPrivacyDashboard() = Unit

    override fun openDeveloperOptions() = Unit
}

/** 预览用的外观偏好。预览里改不动它，但设置页必须能渲染出当前选中的那一档。 */
private class PreviewThemePreferenceStore : ThemePreferenceStore {
    private val mutablePreference = MutableStateFlow(ThemePreference.System)

    override val preference: StateFlow<ThemePreference> = mutablePreference.asStateFlow()

    override suspend fun setPreference(preference: ThemePreference) {
        mutablePreference.value = preference
    }
}

/**
 * 预览用的图标。
 *
 * 生成一张纯色位图而不是一律返回 null：返回 null 虽然也不崩，但看不到「图标 + 文字」并排时
 * 的对齐与截断效果，而那正是需要靠预览确认的东西。颜色按包名取模，保证不同应用颜色不同，
 * 从而能看出图标列是否对齐。
 */
private class PreviewApplicationIconLoader : ApplicationIconLoader {
    private val cache = mutableMapOf<String, ImageBitmap>()

    override suspend fun load(packageName: String): ImageBitmap? = cache.getOrPut(packageName) {
        val bitmap = ImageBitmap(PREVIEW_ICON_PIXEL_SIZE, PREVIEW_ICON_PIXEL_SIZE)
        val canvas = Canvas(bitmap)
        canvas.drawRect(
            Rect(0f, 0f, PREVIEW_ICON_PIXEL_SIZE.toFloat(), PREVIEW_ICON_PIXEL_SIZE.toFloat()),
            Paint().apply { color = PREVIEW_ICON_COLORS[packageName.hashCode().mod(PREVIEW_ICON_COLORS.size)] },
        )
        bitmap
    }
}

/**
 * 预览用的分析结果。
 *
 * 返回固定数据而不是真去读文件：预览环境里没有可选择的外部文件，而这一层的目的是看排版，
 * 不是验证解析逻辑——解析逻辑由 [xin.ctkqiang.nezha_cyber.ads_block.data.analysis.DexDomainScanner]
 * 的单元测试负责。
 */
private class PreviewApkAnalyzer : ApkAnalyzer {
    override suspend fun analyze(source: ApkSource): ApkAnalysisOutcome = ApkAnalysisOutcome.Analyzed(
        ApkAnalysisResult(
            displayName = source.displayName,
            packageName = "com.example.preview",
            versionName = "1.0.0",
            dexFileCount = 2,
            candidates = listOf(
                DomainCandidate(host = "ads.example.com", occurrences = 7),
                DomainCandidate(host = "cdn.example.net", occurrences = 4),
                DomainCandidate(host = "api.example.org", occurrences = 3),
                DomainCandidate(host = "com.example.preview", occurrences = 2),
            ),
            isTruncated = false,
        ),
    )
}

private class PreviewRuleStore : RuleStore {
    private val mutableSnapshot = MutableStateFlow(
        RuleSnapshot(
            rules = listOf(
                DomainRule("doubleclick.net", RuleAction.BLOCK, RuleSource.BUILTIN),
                DomainRule("ads.example.com", RuleAction.BLOCK, RuleSource.BUILTIN),
                DomainRule("*.tracker.example.net", RuleAction.BLOCK, RuleSource.BUILTIN),
                DomainRule("api.example.org", RuleAction.ALLOW, RuleSource.USER),
                DomainRule("news.example.com", RuleAction.BLOCK, RuleSource.USER),
            ),
            builtinVersion = 1,
        ),
    )

    override val snapshot: StateFlow<RuleSnapshot> = mutableSnapshot.asStateFlow()

    override suspend fun load() = Unit

    override suspend fun applyBuiltinCatalog(hosts: Set<String>, version: Int) = Unit

    override suspend fun upsertUserRule(host: String, action: RuleAction): RuleEditResult = RuleEditResult.Applied

    override suspend fun removeUserRule(host: String, action: RuleAction): RuleEditResult = RuleEditResult.Applied

    override suspend fun setRuleEnabled(
        host: String,
        action: RuleAction,
        source: RuleSource,
        enabled: Boolean,
    ): RuleEditResult = RuleEditResult.Applied
}

private class PreviewKeywordPolicyStore : KeywordPolicyStore {
    private val mutablePolicy = MutableStateFlow(KeywordBlockingPolicy.Default)

    override val policy: StateFlow<KeywordBlockingPolicy> = mutablePolicy.asStateFlow()

    override suspend fun load() = Unit

    override suspend fun addKeyword(keyword: String): RuleEditResult {
        mutablePolicy.value = mutablePolicy.value.copy(keywords = mutablePolicy.value.keywords + keyword)
        return RuleEditResult.Applied
    }

    override suspend fun removeKeyword(keyword: String): RuleEditResult {
        mutablePolicy.value = mutablePolicy.value.copy(keywords = mutablePolicy.value.keywords - keyword)
        return RuleEditResult.Applied
    }

    override suspend fun setEnabled(enabled: Boolean) {
        mutablePolicy.value = mutablePolicy.value.copy(enabled = enabled)
    }
}

/**
 * 预览用的隐私策略。
 *
 * [initial] 可注入，这样「记录已关闭」这类状态也能被预览到——而它恰恰是最需要在界面上
 * 确认措辞的那种状态：空列表配上正确的说明才说明设计是对的。
 */
private class PreviewPrivacyPolicyStore(initial: PrivacyPolicy = PrivacyPolicy.Default) : PrivacyPolicyStore {
    private val mutablePolicy = MutableStateFlow(initial)

    override val policy: StateFlow<PrivacyPolicy> = mutablePolicy.asStateFlow()

    override suspend fun load() = Unit

    override suspend fun setObservationLoggingEnabled(enabled: Boolean) {
        mutablePolicy.value = mutablePolicy.value.copy(isObservationLoggingEnabled = enabled)
    }

    override suspend fun setObservationRetention(retention: ObservationRetention) {
        mutablePolicy.value = mutablePolicy.value.copy(observationRetention = retention)
    }

    override suspend fun setBlockedResponseMode(mode: BlockedResponseMode) {
        mutablePolicy.value = mutablePolicy.value.copy(blockedResponseMode = mode)
    }
}

private class PreviewObservationStore : ObservationStore {
    private val mutableStatistics = MutableStateFlow(
        FilteringStatistics(observed = 1_284, blocked = 96, sessionDistinctBlockedHosts = 14),
    )

    private val mutableRecent = MutableStateFlow(
        listOf(
            observation(
                host = "ads.example.com",
                action = RuleAction.BLOCK,
                matchedRule = "ads.example.com",
                source = RuleSource.BUILTIN,
                packageName = PREVIEW_BROWSER_PACKAGE,
            ),
            observation(
                host = "cdn.example.net",
                action = RuleAction.ALLOW,
                matchedRule = null,
                source = null,
                packageName = PREVIEW_BROWSER_PACKAGE,
            ),
            observation(
                host = "tracker.example.net",
                action = RuleAction.BLOCK,
                matchedRule = "ads",
                source = RuleSource.KEYWORD,
                packageName = PREVIEW_VIDEO_PACKAGE,
            ),
            observation(
                host = "api.example.org",
                action = RuleAction.ALLOW,
                matchedRule = "api.example.org",
                source = RuleSource.USER,
                packageName = PREVIEW_CHAT_PACKAGE,
            ),
            observation(
                host = "metrics.example.io",
                action = RuleAction.ALLOW,
                matchedRule = null,
                source = null,
                packageName = null,
            ),
        ),
    )

    override val statistics: StateFlow<FilteringStatistics> = mutableStatistics.asStateFlow()

    override val recent: StateFlow<List<DomainObservation>> = mutableRecent.asStateFlow()

    override suspend fun load() = Unit

    override fun record(observation: DomainObservation) = Unit

    override suspend fun clear() {
        mutableStatistics.value = FilteringStatistics()
        mutableRecent.value = emptyList()
    }

    private fun observation(
        host: String,
        action: RuleAction,
        matchedRule: String?,
        source: RuleSource?,
        packageName: String?,
    ) = DomainObservation(
        at = PREVIEW_INSTANT,
        host = host,
        action = action,
        matchedRule = matchedRule,
        source = source,
        packageName = packageName,
    )
}

private class PreviewInstalledApplicationSource : InstalledApplicationSource {
    override suspend fun listInstalledApplications(): List<InstalledApplication> = listOf(
        InstalledApplication(PREVIEW_BROWSER_PACKAGE, "示例浏览器"),
        InstalledApplication(PREVIEW_VIDEO_PACKAGE, "示例视频"),
        InstalledApplication(PREVIEW_CHAT_PACKAGE, "示例聊天"),
        InstalledApplication(PREVIEW_SHOPPING_PACKAGE, "示例购物"),
        InstalledApplication(PREVIEW_MAP_PACKAGE, "示例地图"),
        InstalledApplication(PREVIEW_NOTES_PACKAGE, "示例记事"),
    )

    override suspend fun displayNames(packageNames: Set<String>): Map<String, String> = mapOf(
        PREVIEW_BROWSER_PACKAGE to "示例浏览器",
        PREVIEW_VIDEO_PACKAGE to "示例视频",
        PREVIEW_CHAT_PACKAGE to "示例聊天",
        PREVIEW_SHOPPING_PACKAGE to "示例购物",
        PREVIEW_MAP_PACKAGE to "示例地图",
        PREVIEW_NOTES_PACKAGE to "示例记事",
    ).filterKeys { packageName -> packageName in packageNames }

    override suspend fun hasPackageVisibility(): Boolean = true
}

/**
 * 预览用的权限。
 *
 * 三个应用各给一组**形状不同**的权限：只给一个应用会让权限清单的排序与分组在预览里看不出差别，
 * 而正确性恰恰要靠「已授予的运行时权限排在前面」这类顺序来确认。
 *
 * 只包含预览需要的条目，不试图覆盖真实设备的权限全集。
 */
private class PreviewApplicationPermissionSource : ApplicationPermissionSource {
    override suspend fun permissionsOf(packageNames: Set<String>): Map<String, List<ApplicationPermission>> =
        PREVIEW_PERMISSIONS.filterKeys { packageName -> packageName in packageNames }
}

private class PreviewProtectedApplicationStore(initial: Set<String>) : ProtectedApplicationStore {
    private val mutableProtected = MutableStateFlow(initial)

    override val protectedPackages: StateFlow<Set<String>> = mutableProtected.asStateFlow()

    override suspend fun load() = Unit

    override suspend fun setProtected(packageName: String, isProtected: Boolean) {
        mutableProtected.value = if (isProtected) {
            mutableProtected.value + packageName
        } else {
            mutableProtected.value - packageName
        }
    }

    override suspend fun clearSelection() {
        mutableProtected.value = emptySet()
    }
}

private const val PREVIEW_ICON_PIXEL_SIZE = 48

private val PREVIEW_ICON_COLORS = listOf(
    Color(0xFFC62828),
    Color(0xFF1565C0),
    Color(0xFF2E7D32),
    Color(0xFF6A1B9A),
    Color(0xFFEF6C00),
)

private val PREVIEW_PERMISSIONS = mapOf(
    PREVIEW_BROWSER_PACKAGE to listOf(
        ApplicationPermission("android.permission.CAMERA", "相机", isDangerous = true, isGranted = true),
        ApplicationPermission("android.permission.INTERNET", "网络访问", isDangerous = false, isGranted = true),
        ApplicationPermission(
            "android.permission.ACCESS_FINE_LOCATION",
            "确切位置",
            isDangerous = true,
            isGranted = true,
        ),
        ApplicationPermission("android.permission.READ_CONTACTS", "通讯录", isDangerous = true, isGranted = false),
    ),
    PREVIEW_VIDEO_PACKAGE to listOf(
        ApplicationPermission("android.permission.RECORD_AUDIO", "麦克风", isDangerous = true, isGranted = true),
        ApplicationPermission(
            "android.permission.READ_MEDIA_VIDEO",
            "视频和照片",
            isDangerous = true,
            isGranted = true,
        ),
        ApplicationPermission("android.permission.INTERNET", "网络访问", isDangerous = false, isGranted = true),
    ),
    PREVIEW_CHAT_PACKAGE to listOf(
        ApplicationPermission("android.permission.READ_CONTACTS", "通讯录", isDangerous = true, isGranted = true),
        ApplicationPermission("android.permission.INTERNET", "网络访问", isDangerous = false, isGranted = true),
    ),
)

private const val PREVIEW_BROWSER_PACKAGE = "com.example.browser"

private const val PREVIEW_VIDEO_PACKAGE = "com.example.video"

private const val PREVIEW_CHAT_PACKAGE = "com.example.chat"

private const val PREVIEW_SHOPPING_PACKAGE = "com.example.shopping"

private const val PREVIEW_MAP_PACKAGE = "com.example.map"

private const val PREVIEW_NOTES_PACKAGE = "com.example.notes"

/** 预览时间固定为常量：相对时间文案（几分钟前）不应随预览时刻漂移。 */
private val PREVIEW_INSTANT: Instant = Instant.parse("2026-09-24T10:15:30Z")
