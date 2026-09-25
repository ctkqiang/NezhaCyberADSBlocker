package xin.ctkqiang.nezha_cyber.ads_block

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import xin.ctkqiang.nezha_cyber.ads_block.domain.analysis.ApkSource
import xin.ctkqiang.nezha_cyber.ads_block.ui.analysis.ApkPicker
import xin.ctkqiang.nezha_cyber.ads_block.ui.analysis.LocalApkPicker

/**
 * 文件选择的组合根。
 *
 * 系统文件选择器只能由 Activity 拉起，因此这里把它包成 [ApkPicker] 下发给界面，
 * ViewModel 侧只负责发出「请用户选文件」这一条一次性效果。
 */
@Composable
internal fun AnalysisCompositionRoot(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val pendingCallback = remember { mutableStateOf<((ApkSource?) -> Unit)?>(null) }
    val documentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        val callback = pendingCallback.value
        pendingCallback.value = null
        callback?.invoke(uri?.let { picked -> picked.toApkSource(context) })
    }
    val picker = remember(documentLauncher) {
        ApkPicker { onPicked ->
            pendingCallback.value = onPicked
            documentLauncher.launch(APK_MIME_TYPES)
        }
    }
    CompositionLocalProvider(LocalApkPicker provides picker, content = content)
}

/**
 * 只声明通配的文件类型：APK 的 MIME 类型并非所有文件提供方都会标注，指定专有类型时
 * 可能出现「列表里什么都看不到」的情况，而这比让用户多看一眼文件类型更糟。
 */
private val APK_MIME_TYPES = arrayOf("*/*")

private fun Uri.toApkSource(context: Context): ApkSource =
    ApkSource(handle = toString(), displayName = displayNameOf(context = context, uri = this))

/**
 * 取所选文件的显示名。
 *
 * 用 `OpenableColumns` 查询而不是解析 URI 路径：内容 URI 的末段常常只是一串编号，
 * 拿它当文件名会让人以为选错了文件。查不到时退回 URI 末段，不编造一个名字。
 */
private fun displayNameOf(context: Context, uri: Uri): String {
    val resolved = context.contentResolver
        .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor -> cursor.readDisplayName() }
    return resolved ?: uri.lastPathSegment.orEmpty()
}

private fun Cursor.readDisplayName(): String? {
    val columnIndex = getColumnIndex(OpenableColumns.DISPLAY_NAME)
    return if (columnIndex < 0 || !moveToFirst()) null else getString(columnIndex)
}
