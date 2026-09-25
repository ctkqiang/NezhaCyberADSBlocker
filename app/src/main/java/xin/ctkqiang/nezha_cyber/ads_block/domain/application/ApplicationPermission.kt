package xin.ctkqiang.nezha_cyber.ads_block.domain.application

/**
 * 一个应用声明的一项权限，以及它此刻是否被授予。
 *
 * [name] 与 [label] 都保留：前者是平台标识（`android.permission.CAMERA`），稳定且可比对；
 * 后者是系统给出的本地化名称，可直接与系统设置里的条目对上。只有名称时无法做任何程序化判断，
 * 只有标识时用户认不出来。
 *
 * [isDangerous] 对应平台概念 `PROTECTION_DANGEROUS`，也就是系统设置里那些用户可开关的
 * 「运行时权限」（相机、位置、通讯录）。普通权限（`INTERNET`、`VIBRATE` 等）安装即授予、
 * 用户无法关闭，把它们和运行时权限混在一起会让「这个应用有多侵入」的判断失真。
 *
 * [isGranted] 是系统对包的最新一次状态快照，反映的是**此刻**是否已授予，
 * 而不是「是否曾经声明过」。
 */
data class ApplicationPermission(val name: String, val label: String, val isDangerous: Boolean, val isGranted: Boolean)
