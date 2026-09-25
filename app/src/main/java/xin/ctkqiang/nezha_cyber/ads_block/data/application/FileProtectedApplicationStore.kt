package xin.ctkqiang.nezha_cyber.ads_block.data.application

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import xin.ctkqiang.nezha_cyber.ads_block.data.writeLinesAtomically
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.ProtectedApplicationStore

private const val FILE_NAME = "protected-applications.txt"

/**
 * 受保护应用集合的文件实现。
 *
 * 每行一个包名，写入同样是「先写临时文件再改名」：这份文件决定隧道接管哪些应用，
 * 半截内容会让保护范围无声地缩小或扩大。
 *
 * 空集合是有语义的（= 不做逐应用过滤），因此**不能**把「文件不存在」当成「用户什么都没选」
 * 之外的含义——两者恰好一致，这正是默认行为。
 */
internal class FileProtectedApplicationStore(private val storageDirectory: File) : ProtectedApplicationStore {
    private val mutex = Mutex()

    private val file = File(storageDirectory, FILE_NAME)

    private val mutableProtected = MutableStateFlow<Set<String>>(emptySet())

    override val protectedPackages: StateFlow<Set<String>> = mutableProtected.asStateFlow()

    override suspend fun load() {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                mutableProtected.value = if (file.exists()) readPackages() else emptySet()
            }
        }
    }

    override suspend fun setProtected(packageName: String, isProtected: Boolean) {
        mutate { current -> if (isProtected) current + packageName else current - packageName }
    }

    override suspend fun clearSelection() {
        mutate { emptySet() }
    }

    private suspend fun mutate(transform: (Set<String>) -> Set<String>) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val updated = transform(mutableProtected.value)
                storageDirectory.mkdirs()
                file.writeLinesAtomically(updated.sorted())
                mutableProtected.value = updated
            }
        }
    }

    private fun readPackages(): Set<String> =
        file.readLines().map { line -> line.trim() }.filter { line -> line.isNotEmpty() }.toSet()
}
