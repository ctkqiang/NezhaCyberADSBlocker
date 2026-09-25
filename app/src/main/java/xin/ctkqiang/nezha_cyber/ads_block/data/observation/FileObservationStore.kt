package xin.ctkqiang.nezha_cyber.ads_block.data.observation

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xin.ctkqiang.nezha_cyber.ads_block.data.writeLinesAtomically
import xin.ctkqiang.nezha_cyber.ads_block.data.writeTextAtomically
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.DomainObservation
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.FilteringStatistics
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.ObservationStore
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.ObservationRetention
import xin.ctkqiang.nezha_cyber.ads_block.domain.privacy.PrivacyPolicy

private const val LOG_FILE_NAME = "observations.log"

private const val COUNTERS_FILE_NAME = "statistics.txt"

/** 待写入队列容量。满了丢弃最旧的：观测记录是诊断信息，不该阻塞数据包处理。 */
private const val PENDING_LINE_CAPACITY = 512

/** 每累计这么多条观测就把计数落盘一次，把进程被杀时的丢失量限制在可接受范围。 */
private const val COUNTERS_FLUSH_INTERVAL = 64

/** 每累计这么多条就刷一次缓冲，把崩溃时的日志丢失量限制在可接受范围。 */
private const val LOG_FLUSH_INTERVAL = 32

private const val COUNTER_SEPARATOR = '\t'

private const val OBSERVED_COLUMN = 0

private const val BLOCKED_COLUMN = 1

/**
 * 观测记录与统计的文件实现。
 *
 * 设计取舍：
 * - [record] 只改内存并投递一行文本到有界队列，绝不在中继线程上做磁盘 I/O；
 * - **累计计数**（观测数、拦截数）落盘，跨会话保留；**本次会话去重域名数**只在内存里算，
 *   因为跨会话去重需要长期持有全部域名集合，会随使用时间无界增长；
 * - 日志是滚动窗口：超过上限时按最近若干行重写，因此它只用于「最近活动」，
 *   不是完整历史。完整的追加式事件日志与检查点属于后续阶段（第 41 节）。
 *
 * 队列满时丢弃最旧的一行：宁可少一条诊断记录，也不能让数据包处理被磁盘速度拖住。
 *
 * 是否记录、保留多少条都来自 [PrivacyPolicy]（工程规则第 20 节）。把闸门放在这里而不是
 * 让每个调用方自己判断：中继的调用点只有一个，但记录与保留是同一件事的两半，
 * 分开实现必然出现「关了记录却仍在往磁盘写」的裂缝。
 *
 * 写入通道刻意不是 `Channel<String>` 而是 [LogRecord]：清空日志必须关闭并重新打开文件，
 * 而这件事只有在写入循环内部才能安全完成——在循环外截断文件会让仍然打开的写指针
 * 落在文件末尾之外，写出一个前面全是空洞的文件。
 */
internal class FileObservationStore(
    private val storageDirectory: File,
    scope: CoroutineScope,
    private val privacyProvider: () -> PrivacyPolicy,
) : ObservationStore {
    private val logFile = File(storageDirectory, LOG_FILE_NAME)

    private val countersFile = File(storageDirectory, COUNTERS_FILE_NAME)

    private val pendingRecords = Channel<LogRecord>(
        capacity = PENDING_LINE_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val sessionBlockedHosts = HashSet<String>()

    /**
     * 保护 [sessionBlockedHosts] 与两个可观察状态之间的读改写序列。
     *
     * [record] 由中继线程调用，[clear] 由界面线程调用，两者都可能同时改这三个值。
     * 没有这把锁时，一次清空与一次记录交错会让计数回到清空前、或让去重集合丢掉刚加入的域名。
     * 因 [record] 不能挂起，这里用监视器而不是 `Mutex`；临界区里没有任何挂起调用。
     */
    private val countersLock = Any()

    private val mutableStatistics = MutableStateFlow(FilteringStatistics())

    private val mutableRecent = MutableStateFlow<List<DomainObservation>>(emptyList())

    private var linesSinceFlush = 0

    private var linesSinceCountersFlush = 0

    override val statistics: StateFlow<FilteringStatistics> = mutableStatistics.asStateFlow()

    override val recent: StateFlow<List<DomainObservation>> = mutableRecent.asStateFlow()

    init {
        scope.launch { drainPendingRecords() }
    }

    override suspend fun load() {
        withContext(Dispatchers.IO) {
            storageDirectory.mkdirs()
            mutableStatistics.value = readCounters()
            val retention = privacyProvider().observationRetention
            val lines = if (logFile.exists()) logFile.readLines() else emptyList()
            mutableRecent.value = lines.asReversed()
                .asSequence()
                .mapNotNull { line -> ObservationLogCodec.parse(line) }
                .take(retention.capacity)
                .toList()
            rotateIfOversized(lines = lines, retention = retention)
        }
    }

    override fun record(observation: DomainObservation) {
        val policy = privacyProvider()
        // 关掉记录是完全合法的选择：隧道照常过滤与拦截，只是不再留下观测与计数。
        if (!policy.isObservationLoggingEnabled) return

        val capacity = policy.observationRetention.capacity
        synchronized(countersLock) {
            val current = mutableStatistics.value
            if (observation.isBlocked) {
                sessionBlockedHosts.add(observation.host)
            }
            mutableStatistics.value = current.copy(
                observed = current.observed + 1,
                blocked = current.blocked + if (observation.isBlocked) 1 else 0,
                sessionDistinctBlockedHosts = sessionBlockedHosts.size,
            )
            mutableRecent.value = (listOf(observation) + mutableRecent.value).take(capacity)
        }
        pendingRecords.trySend(LogRecord.Append(ObservationLogCodec.format(observation)))
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) {
            // 顺序不可颠倒。先把计数落盘为 0，此时内存还没变，写失败就抛出去、什么也没改；
            // 再投递截断指令（用 send 而不是 trySend：逐条追加可以丢，清空指令不能丢）。
            // 最后才重置内存是在下面做的，截断指令先入队保证其后的记录不会被一起清掉。
            writeCounters(FilteringStatistics())
            pendingRecords.send(LogRecord.Truncate)
        }
        synchronized(countersLock) {
            sessionBlockedHosts.clear()
            mutableStatistics.value = FilteringStatistics()
            mutableRecent.value = emptyList()
        }
    }

    /**
     * 唯一持有日志写指针的循环。
     *
     * 收到截断指令时关掉当前写指针、清空文件、再开一个继续写。截断指令之后才投递的记录
     * 仍在通道里，会在新一轮循环中被写入，因此清空不会连带丢掉清空之后产生的记录。
     */
    private suspend fun drainPendingRecords() {
        storageDirectory.mkdirs()
        while (true) {
            val truncated = openLogWriter(append = true).use { output -> appendRecords(output) }
            if (!truncated) return
            openLogWriter(append = false).close()
        }
    }

    /** 返回 true 表示读到截断指令，调用方必须重新打开文件。 */
    private suspend fun appendRecords(output: BufferedWriter): Boolean {
        for (record in pendingRecords) {
            when (record) {
                LogRecord.Truncate -> return true
                is LogRecord.Append -> append(output, record.line)
            }
        }
        return false
    }

    private fun append(output: BufferedWriter, line: String) {
        output.appendLine(line)
        linesSinceFlush += 1
        linesSinceCountersFlush += 1
        if (linesSinceFlush >= LOG_FLUSH_INTERVAL) {
            linesSinceFlush = 0
            output.flush()
        }
        if (linesSinceCountersFlush >= COUNTERS_FLUSH_INTERVAL) {
            linesSinceCountersFlush = 0
            output.flush()
            writeCounters(mutableStatistics.value)
        }
    }

    /**
     * 用 [FileOutputStream] 而不是 `File.bufferedWriter()`。
     *
     * 后者以**截断**方式打开文件，会在每次启动时把上一次的日志抹掉，让「网络活动」在重启后
     * 永远只剩本次会话的记录。这里的两个分支分别对应「续写」与「清空」。
     */
    private fun openLogWriter(append: Boolean): BufferedWriter =
        OutputStreamWriter(FileOutputStream(logFile, append), Charsets.UTF_8).buffered()

    private fun rotateIfOversized(lines: List<String>, retention: ObservationRetention) {
        if (lines.size <= retention.logLineLimit) return
        // 只重写文件；内存里的 recent 本来就取自同一批行，不需要跟着动。
        logFile.writeLinesAtomically(lines.takeLast(retention.capacity))
    }

    private fun readCounters(): FilteringStatistics {
        if (!countersFile.exists()) return FilteringStatistics()
        val columns = countersFile.readText().trim().split(COUNTER_SEPARATOR)
        val observed = columns.getOrNull(OBSERVED_COLUMN)?.toLongOrNull() ?: 0L
        val blocked = columns.getOrNull(BLOCKED_COLUMN)?.toLongOrNull() ?: 0L
        return FilteringStatistics(observed = observed, blocked = blocked)
    }

    /** 计数文件的唯一写入点。清空与定期落盘共用它，两者的格式因此不可能分叉。 */
    private fun writeCounters(statistics: FilteringStatistics) {
        storageDirectory.mkdirs()
        countersFile.writeTextAtomically("${statistics.observed}$COUNTER_SEPARATOR${statistics.blocked}")
    }

    /** 写入通道上的两种指令。 */
    private sealed interface LogRecord {
        data class Append(val line: String) : LogRecord

        data object Truncate : LogRecord
    }
}
