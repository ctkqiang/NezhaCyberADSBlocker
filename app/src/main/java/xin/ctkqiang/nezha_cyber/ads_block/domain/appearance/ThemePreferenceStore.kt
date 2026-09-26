package xin.ctkqiang.nezha_cyber.ads_block.domain.appearance

import kotlinx.coroutines.flow.StateFlow

/**
 * 端口：外观主题偏好的读取与写入。
 *
 * 刻意**没有** `load()`。其余存储都把载入放在后台协程里，是为了不阻塞冷启动；但主题不一样：
 * 它在第一帧就要用上，晚一步读出来就会先按跟随系统画一帧、再跳成用户选的那一档——
 * 每次启动都闪一下。因此实现约定在构造时就同步读出初值，代价是启动路径上多一次几十字节的
 * 文件读，换掉一次可见的闪烁。
 *
 * 这是本工程唯一允许在构造时读盘的存储，理由写在这里以免后来者「顺手统一」掉它。
 */
interface ThemePreferenceStore {
    /** 当前偏好。状态端口，用 StateFlow（工程规则第 37.5 节）。 */
    val preference: StateFlow<ThemePreference>

    /** 写入偏好并立即生效。 */
    suspend fun setPreference(preference: ThemePreference)
}
