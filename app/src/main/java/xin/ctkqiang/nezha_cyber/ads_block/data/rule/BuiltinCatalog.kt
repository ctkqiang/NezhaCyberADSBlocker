package xin.ctkqiang.nezha_cyber.ads_block.data.rule

/** 内置清单的加载结果：条目集合与清单版本号（第 39.3 节）。 */
internal data class BuiltinCatalog(val hosts: Set<String>, val version: Int)
