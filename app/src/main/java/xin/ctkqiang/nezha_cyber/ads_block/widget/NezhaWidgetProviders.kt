package xin.ctkqiang.nezha_cyber.ads_block.widget

/**
 * 十种桌面小组件的入口类。
 *
 * 每个类只有一行：声明自己是哪一种。系统按 `<receiver>` 识别小组件，因此一种必须一个类；
 * 而所有行为都在 [NezhaWidgetProvider] 里，这里不该再出现第二份逻辑。
 *
 * 这些类**必须是 `internal` 之外可被反射实例化的**：Kotlin 的 `internal` 在字节码层面仍是 public，
 * 因此系统能正常构造它们；同时它们被清单引用，R8 会按清单保留，不会被裁剪掉。
 *
 * 集中放在一个文件里，是因为它们构成同一个封闭层次、且每个都只有一行
 * （工程规则第 37.1 节对这种情况的例外）：分成十个文件，反而看不出「一共就这些」。
 */

internal class NezhaProtectionToggleWidgetProvider : NezhaWidgetProvider(NezhaWidgetKind.ProtectionToggle)

internal class NezhaBlockedCountWidgetProvider : NezhaWidgetProvider(NezhaWidgetKind.BlockedCount)

internal class NezhaBlockedRatioWidgetProvider : NezhaWidgetProvider(NezhaWidgetKind.BlockedRatio)

internal class NezhaLatestBlockedWidgetProvider : NezhaWidgetProvider(NezhaWidgetKind.LatestBlocked)

internal class NezhaProtectedApplicationsWidgetProvider : NezhaWidgetProvider(NezhaWidgetKind.ProtectedApplications)

internal class NezhaObservedTotalWidgetProvider : NezhaWidgetProvider(NezhaWidgetKind.ObservedTotal)

internal class NezhaRuleScaleWidgetProvider : NezhaWidgetProvider(NezhaWidgetKind.RuleScale)

internal class NezhaTopBlockedApplicationsWidgetProvider : NezhaWidgetProvider(NezhaWidgetKind.TopBlockedApplications)

internal class NezhaBlockedTrendWidgetProvider : NezhaWidgetProvider(NezhaWidgetKind.BlockedTrend)

internal class NezhaOverviewWidgetProvider : NezhaWidgetProvider(NezhaWidgetKind.Overview)
