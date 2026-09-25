package xin.ctkqiang.nezha_cyber.ads_block.ui.navigation

import androidx.annotation.StringRes
import xin.ctkqiang.nezha_cyber.ads_block.R

/**
 * 应用内的九个页面，与工程规则第 5 节列出的页面一一对应。
 *
 * 展开为平铺枚举而非嵌套结构，是因为它们都处于同一层：每个页面只依赖所在标签页，互不包含。
 * 页面归属由 [NezhaTab.sections] 单向声明，避免两个枚举互相引用造成类初始化环。
 */
enum class NezhaSection(@StringRes val titleRes: Int) {
    Home(R.string.section_home),
    ApplicationList(R.string.section_application_list),
    ApplicationDetail(R.string.section_application_detail),
    ApkAnalysis(R.string.section_apk_analysis),
    Blocklist(R.string.section_blocklist),
    Allowlist(R.string.section_allowlist),
    NotificationRules(R.string.section_notification_rules),
    Statistics(R.string.section_statistics),
    NetworkActivity(R.string.section_network_activity),
    Settings(R.string.section_settings),
}
