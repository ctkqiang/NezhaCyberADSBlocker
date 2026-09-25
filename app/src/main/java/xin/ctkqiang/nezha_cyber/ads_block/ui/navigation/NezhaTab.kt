package xin.ctkqiang.nezha_cyber.ads_block.ui.navigation

import androidx.annotation.StringRes
import xin.ctkqiang.nezha_cyber.ads_block.R

/**
 * 悬浮底栏上的五个一级入口。
 *
 * 底栏容量有限，因此把九个页面归到五个入口下：多个页面共用入口时由分段控件切换，
 * 只有一个页面时直接把页面标题作为页头。
 *
 * 实时活动（网络活动页）归在「应用」而不是「统计」下：它回答的是「哪个应用在请求什么」，
 * 与同一标签下的应用列表、应用详情构成同一条从「谁在联网」到「它拿了什么权限」的线索；
 * 统计标签则只留聚合读数。这一处归组是刻意的，不是历史遗留。
 *
 * @param sections 该入口下的全部页面，第一个为默认页。
 */
enum class NezhaTab(@StringRes val titleRes: Int, val sections: List<NezhaSection>) {
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
    Settings(R.string.tab_settings, listOf(NezhaSection.Settings)),
    ;

    val defaultSection: NezhaSection
        get() = sections.first()
}
