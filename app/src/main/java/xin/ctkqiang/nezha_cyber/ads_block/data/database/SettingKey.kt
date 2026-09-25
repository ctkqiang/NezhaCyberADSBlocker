package xin.ctkqiang.nezha_cyber.ads_block.data.database

/**
 * 设置键。
 *
 * 这些字符串是**持久化契约**（工程规则第 37.6 节）：改名等同于改格式，必须同时提供迁移。
 * 因此它们是显式写出的常量，而不是从某个属性名或枚举名推导出来的——推导出来的键会在
 * 重命名属性时静默失效，而那正是最难查的一类数据丢失。
 *
 * 读取方必须遵守同一套容错规则：未知键跳过；缺失或无法解析的取值退回该设置自己的默认值。
 * 这与原先文本文件的读法一致，因此升级不会因为一个坏值把整份配置丢掉。
 */
internal object SettingKey {
    const val OBSERVATION_LOGGING_ENABLED = "observation_logging_enabled"

    const val OBSERVATION_RETENTION = "observation_retention"

    const val BLOCKED_RESPONSE_MODE = "blocked_response_mode"

    const val KEYWORD_BLOCKING_ENABLED = "keyword_blocking_enabled"

    /** 已导入的内置清单版本号。与 `assets/domains.version` 比较，不同则执行增量合并（第 39.3 节）。 */
    const val BUILTIN_CATALOG_VERSION = "builtin_catalog_version"

    /** 旧的文件存储是否已完成搬家。完成后不再读旧文件，但也不删除它们。 */
    const val LEGACY_IMPORT_COMPLETED = "legacy_import_completed"
}
