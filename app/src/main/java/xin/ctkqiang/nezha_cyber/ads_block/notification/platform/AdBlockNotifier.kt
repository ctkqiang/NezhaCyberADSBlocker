package xin.ctkqiang.nezha_cyber.ads_block.notification.platform

import android.app.NotificationManager
import android.content.Context
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import xin.ctkqiang.nezha_cyber.ads_block.R
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.InstalledApplicationSource
import xin.ctkqiang.nezha_cyber.ads_block.domain.observation.DomainObservation

/** 通知栏里最多同时保留多少条拦截通知。 */
private const val MAX_LIVE_NOTIFICATIONS = 20

/** 通知统一挂在这个 tag 下，撤下时按 id 逐条取消。 */
private const val NOTIFICATION_TAG = "ad-block"

/**
 * 通知 id 的取值范围。
 *
 * 与常驻的隧道通知（1001）错开，避免互相覆盖；通知拦截那一套用的是另一个 tag，因此 tag 相同
 * 也不会撞。区间内循环复用的理由见 [AdBlockNotifier.allocateNotificationId]。
 */
private const val FIRST_NOTIFICATION_ID = 2000

private const val LAST_NOTIFICATION_ID = 2999

/**
 * 待发布队列的容量。
 *
 * 比同时保留的通知数大一倍：队列只需要吸收瞬间的突发，溢出时丢最旧的——
 * 最近发生的那次拦截比几秒前那一次更值得被看见。
 */
private const val QUEUE_CAPACITY = MAX_LIVE_NOTIFICATIONS * 2

/**
 * 拦截通知的发布器：**每拦下一条，就发一条通知**。
 *
 * 三个设计要点：
 * - **[record] 必须立刻返回**：它在中继线程上被每个「被拦下的」DNS 查询调用，只做一次入队
 *   （`trySend`），查包名与发布通知全部留给 [run] 所在的协程；
 * - **逐条发，不合并**：每条拦截各自成条、各自有 id，不再共用一个不断被覆盖的汇总通知；
 * - **通知栏有上限**：[MAX_LIVE_NOTIFICATIONS] 条之外撤掉最早的。没有上限时，一次页面加载
 *   就能产生几十条，通知栏会被这一个应用占满。撤掉的只是通知栏里的提示——
 *   「网络活动」页里的记录一条都不会少，那里才是完整流水；这一点对用户是必须说清的，
 *   否则「通知只留了 20 条」会被读成「只拦了 20 条」。
 *
 * 状态在 [stateLock] 下串行化：[run] 跑在服务的 IO 协程上，而隧道停止时 [cancel] 由主线程调用，
 * 两者会碰同一份存活集合。`ArrayDeque` 不是线程安全的，不加锁会偶发地把它改坏。
 * 锁只覆盖「分配 id → 发布 → 淘汰」这一段非挂起操作，不跨越任何挂起点。
 */
internal class AdBlockNotifier(
    applicationContext: Context,
    private val applicationSource: InstalledApplicationSource,
) {
    private val context = applicationContext.applicationContext

    private val manager: NotificationManager? = context.getSystemService(NotificationManager::class.java)

    init {
        // 频道只建一次。这里曾经在每次发布时都调一次，等于每个被拦下的请求都多做一次跨进程查询。
        AdBlockNotification.ensureChannel(context)
    }

    private val pending = Channel<DomainObservation>(
        capacity = QUEUE_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val stateLock = Any()

    /** 通知栏里当前还活着的通知 id，用于按上限淘汰。 */
    private val liveNotificationIds = ArrayDeque<Int>()

    private var nextNotificationId = FIRST_NOTIFICATION_ID

    /** 记录一条被拦下的查询。在中继线程上调用，必须立刻返回。 */
    fun record(observation: DomainObservation) {
        pending.trySend(observation)
    }

    /** 消费队列并逐条发布，直到调用方取消这个协程。 */
    suspend fun run() {
        while (true) {
            publish(pending.receive())
        }
    }

    /** 撤下本应用发出的全部拦截通知，并丢弃尚未发布完的残留。 */
    fun cancel() {
        var stale = pending.tryReceive()
        while (stale.isSuccess) {
            stale = pending.tryReceive()
        }
        val notificationManager = manager ?: return
        val ids = synchronized(stateLock) {
            val snapshot = liveNotificationIds.toList()
            liveNotificationIds.clear()
            snapshot
        }
        // 取消动作在锁外做：cancel() 由主线程在停止保护时调用，在锁里做跨进程调用，
        // 会让「停止保护」去等一个不受本应用控制的系统调用。
        ids.forEach { id -> notificationManager.cancel(NOTIFICATION_TAG, id) }
    }

    private suspend fun publish(observation: DomainObservation) {
        val notificationManager = manager ?: return
        val appLabel = applicationLabelOf(observation.packageName)
        val notification = AdBlockNotification.build(
            context = context,
            observation = observation,
            appLabel = appLabel,
        )
        val slot = synchronized(stateLock) { allocateSlot() }
        // 两个跨进程调用都在锁外。
        notificationManager.notify(NOTIFICATION_TAG, slot.id, notification)
        slot.evicted.forEach { evictedId -> notificationManager.cancel(NOTIFICATION_TAG, evictedId) }
    }

    /**
     * 分配一个通知 id，并算出因超限而需要撤下的通知。
     *
     * 只做纯记账、不做任何跨进程调用，因此可以安全地在锁里跑；真正的 notify 与 cancel
     * 由 [publish] 在锁外执行。分开的理由见 [publish] 的注释——这是本类不再卡住停止流程的关键。
     */
    private fun allocateSlot(): Slot {
        val id = allocateNotificationId()
        liveNotificationIds.addLast(id)
        val evicted = mutableListOf<Int>()
        while (liveNotificationIds.size > MAX_LIVE_NOTIFICATIONS) {
            evicted.add(liveNotificationIds.removeFirst())
        }
        return Slot(id = id, evicted = evicted)
    }

    /**
     * 分配一个通知 id。
     *
     * 在固定区间内循环而不是无限自增：id 是 `Int`，无限自增迟早绕回，那时新通知会以
     * 「覆盖」的语义顶掉一条仍在通知栏里的旧通知——用户看到的是数量莫名少了一条。
     * 区间内循环配合上限淘汰则不会：旧 id 在被复用之前早已撤下。
     */
    private fun allocateNotificationId(): Int {
        val id = nextNotificationId
        nextNotificationId = if (nextNotificationId >= LAST_NOTIFICATION_ID) {
            FIRST_NOTIFICATION_ID
        } else {
            nextNotificationId + 1
        }
        return id
    }

    /**
     * 应用显示名。
     *
     * 走既有的应用发现端口，而不是自己再查一次包管理器：解析规则与「查不到时如何降级」
     * 只应有一处实现。归属不可用时如实显示「未知来源」，不猜一个应用出来（第 32 节）。
     */
    private suspend fun applicationLabelOf(packageName: String?): String {
        if (packageName == null) return context.getString(R.string.ad_block_app_unknown)
        return applicationSource.displayNames(setOf(packageName))[packageName] ?: packageName
    }

    /**
     * 一次发布的分配结果。
     *
     * 嵌套在这里是因为它只在锁内算出、在锁外使用，没有第二个使用场景
     * （工程规则第 37.7 节：嵌套类型置于类体末尾）。
     */
    private data class Slot(val id: Int, val evicted: List<Int>)
}
