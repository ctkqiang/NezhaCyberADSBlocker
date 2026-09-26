package xin.ctkqiang.nezha_cyber.ads_block.ui.navigation

import androidx.annotation.StringRes
import xin.ctkqiang.nezha_cyber.ads_block.BuildConfig
import xin.ctkqiang.nezha_cyber.ads_block.R

/**
 * 悬浮底栏上的五个一级入口。
 *
 * 底栏容量有限，因此把十个页面归到五个入口下：多个页面共用入口时由分段控件切换，
 * 只有一个页面时直接把页面标题作为页头。
 *
 * 实时活动（网络活动页）归在「应用」而不是「统计」下：它回答的是「哪个应用在请求什么」，
 * 与同一标签下的应用列表、应用详情构成同一条从「谁在联网」到「它拿了什么权限」的线索；
 * 统计标签则只留聚合读数。这一处归组是刻意的，不是历史遗留。
 *
 * 隐私与传感器页归在「设置」下：它读的是系统权限，与本应用自己的过滤配置不是一回事，
 * 但用户的寻找路径是一致的——「我要调隐私相关的开关」时先点开设置。
 *
 * 声明与可见分成两层：[declaredSections] 是该入口应有的页面全集，[sections] 再按当前构建变体的
 * 能力过滤。两者不合并成一份常量列表，是因为分变体后可见页面会随能力变化，而枚举常量必须在两个
 * 变体里是同一份定义——把过滤留到读取时做，同一处声明才能同时服务 standard 与 full 两个包。
 *
 * @param declaredSections 该入口声明的全部页面，声明顺序的第一项即默认页。
 */
enum class NezhaTab(@StringRes val titleRes: Int, private val declaredSections: List<NezhaSection>) {
    Home(R.string.tab_home, listOf(NezhaSection.Home)),
    Applications(
        R.string.tab_applications,
        listOf(
            NezhaSection.NetworkActivity,
            NezhaSection.ApplicationList,
            NezhaSection.ApplicationDetail,
            NezhaSection.ApkAnalysis,
        ),
    ),
    Rules(
        R.string.tab_rules,
        listOf(
            NezhaSection.Blocklist,
            NezhaSection.Allowlist,
            NezhaSection.NotificationRules,
        ),
    ),
    Statistics(R.string.tab_statistics, listOf(NezhaSection.Statistics)),
    Settings(R.string.tab_settings, listOf(NezhaSection.Settings, NezhaSection.PrivacyAudit)),
    ;

    /** 当前变体下该入口真正可见的页面，第一个为默认页。 */
    val sections: List<NezhaSection>
        get() = declaredSections.filter { it.isAvailableInCurrentVariant() }

    val defaultSection: NezhaSection
        get() = sections.first()
}

/**
 * 页面在当前构建变体下是否可用。
 *
 * 通知规则页依赖通知监听服务，而 standard 变体不声明该服务（见 `app/build.gradle.kts` 的 capability
 * 维度）。此时若仍列出入口，用户点进去看到的会是一个永远无法生效的授权开关——按工程规则第 45.3 节，
 * 这比没有这个入口更有害，因为它会让人以为自己已经被保护。因此把「能力缺失就没有入口」固化成一条
 * 规则，而不是留给每个界面自己判断。
 */
private fun NezhaSection.isAvailableInCurrentVariant(): Boolean =
    this != NezhaSection.NotificationRules || BuildConfig.NOTIFICATION_INTERCEPTION
