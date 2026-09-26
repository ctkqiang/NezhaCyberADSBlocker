package xin.ctkqiang.nezha_cyber.ads_block.data.appearance

import android.util.Log
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import xin.ctkqiang.nezha_cyber.ads_block.data.writeTextAtomically
import xin.ctkqiang.nezha_cyber.ads_block.domain.appearance.ThemePreference
import xin.ctkqiang.nezha_cyber.ads_block.domain.appearance.ThemePreferenceStore

private const val FILE_NAME = "theme-preference.txt"

private const val LOG_TAG = "NezhaThemePreference"

/**
 * 外观偏好的文件实现。
 *
 * 只在构造时同步读一次（见 [ThemePreferenceStore] 的说明），因此它没有 `load()`。
 *
 * **载入时绝不写回**：用户没改过设置时文件不存在，默认值来自 [ThemePreference.System]。
 * 把默认值固化成一份看起来像「用户配置」的副本，会让日后调整默认值在已安装设备上永不生效——
 * 与 `FilePrivacyPolicyStore` 同一条理由。
 *
 * 文件不可读或内容无法解析都退回默认值，绝不让应用起不来（工程规则第 29 节）。
 */
internal class FileThemePreferenceStore(private val storageDirectory: File) : ThemePreferenceStore {
    private val mutex = Mutex()

    private val file = File(storageDirectory, FILE_NAME)

    private val mutablePreference = MutableStateFlow(readInitialPreference())

    override val preference: StateFlow<ThemePreference> = mutablePreference.asStateFlow()

    override suspend fun setPreference(preference: ThemePreference) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                storageDirectory.mkdirs()
                file.writeTextAtomically(preference.name)
                mutablePreference.value = preference
            }
        }
    }

    private fun readInitialPreference(): ThemePreference = try {
        if (file.exists()) parse(file.readText()) else ThemePreference.System
    } catch (unreadable: IOException) {
        Log.w(LOG_TAG, "外观偏好文件不可读，本次按跟随系统处理", unreadable)
        ThemePreference.System
    }

    /** 无法识别的取值退回默认值：一个坏字符串不应该让用户卡在不知哪一档上。 */
    private fun parse(raw: String): ThemePreference =
        ThemePreference.entries.firstOrNull { entry -> entry.name.equals(raw.trim(), ignoreCase = true) }
            ?: ThemePreference.System
}
