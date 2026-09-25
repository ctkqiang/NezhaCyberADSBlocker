package xin.ctkqiang.nezha_cyber.ads_block.data.observation

import java.io.File
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.DomainObservation
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.ObservationRetention
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.PrivacyPolicy
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleAction
import xin.ctkqiang.nezha_cyber.ads_block.domain.rule.RuleSource

private const val LOG_FILE_NAME = "observations.log"

/**
 * 观测存储的隐私行为测试。
 *
 * 锁住两条最要紧的约定：
 * 1. **关掉记录之后确实一条都不记**——这是「不收集」这个承诺的唯一凭据，界面怎么标注都不如它；
 * 2. **保留量真的限制条数**，且限制来自策略而不是写死的常量。
 *
 * 写入通道由 `backgroundScope` 承载：它是「永不结束」的循环，用 `runTest` 自己的作用域会让
 * 测试永远等不到结束。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FileObservationStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `关闭记录后既不累计统计也不留最近观测`() = runTest {
        val store = newStore(scope = backgroundScope, policy = PrivacyPolicy(isObservationLoggingEnabled = false))
        store.load()

        store.record(observation())

        assertEquals(0L, store.statistics.value.observed)
        assertEquals(0L, store.statistics.value.blocked)
        assertTrue(store.recent.value.isEmpty())
    }

    @Test
    fun `打开记录时正常累计统计与最近观测`() = runTest {
        val store = newStore(scope = backgroundScope, policy = PrivacyPolicy())
        store.load()

        store.record(observation())

        assertEquals(1L, store.statistics.value.observed)
        assertEquals(1L, store.statistics.value.blocked)
        assertEquals(1, store.recent.value.size)
    }

    @Test
    fun `保留量决定最近观测的条数上限`() = runTest {
        val retention = ObservationRetention.Minimal
        val store = newStore(scope = backgroundScope, policy = PrivacyPolicy(observationRetention = retention))
        store.load()

        repeat(retention.capacity + EXTRA_RECORDS) { store.record(observation()) }

        assertEquals(retention.capacity, store.recent.value.size)
        // 被挤掉的是最旧的观测，但累计计数只增不减——它记的是「发生过多少次」，不是「留了几条」。
        assertEquals((retention.capacity + EXTRA_RECORDS).toLong(), store.statistics.value.observed)
    }

    @Test
    fun `载入时读取磁盘上已有的观测`() = runTest {
        val directory = temporaryFolder.newFolder()
        File(directory, LOG_FILE_NAME).appendText(ObservationLogCodec.format(observation()) + "\n")
        val store = newStore(
            scope = backgroundScope,
            policy = PrivacyPolicy(),
            directory = directory,
        )

        store.load()

        assertEquals(1, store.recent.value.size)
    }

    private fun newStore(
        scope: CoroutineScope,
        policy: PrivacyPolicy,
        directory: File = temporaryFolder.newFolder(),
    ): FileObservationStore = FileObservationStore(
        storageDirectory = directory,
        scope = scope,
        privacyProvider = { policy },
    )

    private fun observation() = DomainObservation(
        at = Instant.parse("2026-09-24T10:15:30Z"),
        host = "ads.example.com",
        action = RuleAction.BLOCK,
        matchedRule = "ads.example.com",
        source = RuleSource.BUILTIN,
        packageName = "com.example.browser",
    )

    private companion object {
        /** 超出保留量多少条，用于确认「多出来的确实被挤掉了」。 */
        const val EXTRA_RECORDS = 5
    }
}
