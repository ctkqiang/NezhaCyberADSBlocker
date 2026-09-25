package xin.ctkqiang.nezha_cyber.ads_block.data.privacy

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
import xin.ctkqiang.nezha_cyber.ads_block.data.writeLinesAtomically
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.BlockedResponseMode
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.ObservationRetention
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.PrivacyPolicy
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.PrivacyPolicyStore

private const val FILE_NAME = "privacy-policy.txt"

private const val LOG_TAG = "NezhaPrivacyPolicy"

/**
 * 隐私策略的文件实现。
 *
 * 载入时**不写回**：用户从未改过设置时文件保持不存在，默认值仍来自 [PrivacyPolicy.Default]。
 * 把默认值固化成一份看起来像「用户配置」的副本，会让日后调整默认值在已安装设备上永不生效。
 * 用户改动任意一项后，文件会带着全部三个键一起写出，因此它从第一次落盘起就是完整的、可读的。
 *
 * 载入时文件不可读、或内容部分损坏，都退回默认策略而不是让应用起不来（工程规则第 29 节）。
 */
internal class FilePrivacyPolicyStore(private val storageDirectory: File) : PrivacyPolicyStore {
    private val mutex = Mutex()

    private val file = File(storageDirectory, FILE_NAME)

    private val mutablePolicy = MutableStateFlow(PrivacyPolicy.Default)

    override val policy: StateFlow<PrivacyPolicy> = mutablePolicy.asStateFlow()

    override suspend fun load() {
        withContext(Dispatchers.IO) {
            mutex.withLock { mutablePolicy.value = readPolicy() }
        }
    }

    override suspend fun setObservationLoggingEnabled(enabled: Boolean) {
        mutate { policy -> policy.copy(isObservationLoggingEnabled = enabled) }
    }

    override suspend fun setObservationRetention(retention: ObservationRetention) {
        mutate { policy -> policy.copy(observationRetention = retention) }
    }

    override suspend fun setBlockedResponseMode(mode: BlockedResponseMode) {
        mutate { policy -> policy.copy(blockedResponseMode = mode) }
    }

    private suspend fun mutate(transform: (PrivacyPolicy) -> PrivacyPolicy) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val updated = transform(mutablePolicy.value)
                persist(updated)
                mutablePolicy.value = updated
            }
        }
    }

    /** 文件不存在或读不出来时返回默认策略：读取失败的处置与「用户没配过」是同一个结果。 */
    private fun readPolicy(): PrivacyPolicy = try {
        if (file.exists()) PrivacyPolicyCodec.parse(file.readLines()) else PrivacyPolicy.Default
    } catch (unreadable: IOException) {
        Log.w(LOG_TAG, "隐私策略文件不可读，本次使用默认策略", unreadable)
        PrivacyPolicy.Default
    }

    private fun persist(policy: PrivacyPolicy) {
        storageDirectory.mkdirs()
        file.writeLinesAtomically(PrivacyPolicyCodec.format(policy))
    }
}
