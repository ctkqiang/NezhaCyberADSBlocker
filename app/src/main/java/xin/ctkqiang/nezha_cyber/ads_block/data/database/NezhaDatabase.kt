package xin.ctkqiang.nezha_cyber.ads_block.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * 应用数据库。
 *
 * 只承载**属于我们的数据**：观测记录与计数、用户规则与内置覆写、键值设置、关键词条目、
 * 受保护应用集合。
 *
 * 已安装应用清单、应用图标与标签、VPN 实时状态都**不**在这里——它们是平台与内核的事实，
 * 只作为输入使用（工程规则第 41.8 节）。把权威来源的副本存进来，等于制造一份迟早会与
 * 真实状态不一致的影子数据。
 *
 * schema 导出是打开的，且导出目录纳入版本控制：将来改动表结构时，迁移写得对不对可以直接
 * 拿历史 schema 对照复核，而不必凭记忆重建上一版长什么样。
 *
 * 基类只声明 DAO，不负责建库：怎么开库（文件名、迁移、关闭策略）由 `NezhaDatabaseFactory` 决定，
 * 因此「库长什么样」与「库怎么被打开」各自独立。
 *
 * **关于版本号仍是 1**：这个数据库**从未在任何设备上被打开过**（本文件此前只有表结构，没有建库入口）。
 * 因此新增 `notification_rules` 表时直接把表并入 v1，而不是写一条永远不会有人执行的迁移。
 * 第一次真正发布之后，这条便利就消失了——此后**任何**实体改动都必须递增版本号并提供对应的
 * `Migration`。Room 会在编译期校验声明与代码的一致性，但不会替你想出迁移该怎么写。
 */
@Database(
    entities = [
        ObservationEntity::class,
        ObservationCounterEntity::class,
        RuleEntity::class,
        SettingEntity::class,
        KeywordEntity::class,
        ProtectedApplicationEntity::class,
        NotificationRuleEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
internal abstract class NezhaDatabase : RoomDatabase() {
    abstract fun observationDao(): ObservationDao

    abstract fun ruleDao(): RuleDao

    abstract fun settingDao(): SettingDao

    abstract fun keywordDao(): KeywordDao

    abstract fun protectedApplicationDao(): ProtectedApplicationDao

    abstract fun notificationRuleDao(): NotificationRuleDao
}
