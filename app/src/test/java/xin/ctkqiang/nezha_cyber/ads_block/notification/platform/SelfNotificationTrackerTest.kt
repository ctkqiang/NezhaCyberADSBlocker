package xin.ctkqiang.nezha_cyber.ads_block.notification.platform

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val OWN_PACKAGE = "xin.ctkqiang.nezha_cyber.ads_block"

private const val TAOBAO = "com.taobao.taobao"

/** 替代通知使用的 tag。与 `BlockedNotificationPoster` 里的取值保持一致。 */
private const val BLOCKED_TAG = "blocked"

private const val REPLACEMENT_ID = 9001

/**
 * 通知回环防护的测试。
 *
 * 这是整个通知功能里**唯一一个出错就会失控**的地方：判错一次，本应用就会为自己的替代通知
 * 再发一条替代通知，然后无限重复。其余判定判错只是漏拦或多拦，用户还能看出来；这里出错
 * 是持续消耗资源，且症状（通知栏被自己的通知塞满）与原因隔得很远。
 *
 * 因此这里逐条锁住两道判据，并显式复现一次真实时序。
 */
class SelfNotificationTrackerTest {
    @Test
    fun `本应用自己发出的通知一律判为自己发的`() {
        val tracker = SelfNotificationTracker(ownPackageName = OWN_PACKAGE)

        // 不做任何登记也必须认出来：包名是平台保证的事实，是最可靠的那道判据。
        assertTrue(tracker.isSelfPosted(packageName = OWN_PACKAGE, tag = null, id = 0))
        assertTrue(tracker.isSelfPosted(packageName = OWN_PACKAGE, tag = BLOCKED_TAG, id = REPLACEMENT_ID))
    }

    @Test
    fun `登记过的标识即使换个包名也判为自己发的`() {
        val tracker = SelfNotificationTracker(ownPackageName = OWN_PACKAGE)
        tracker.rememberPosted(tag = BLOCKED_TAG, id = 7)

        assertTrue(tracker.isSelfPosted(packageName = TAOBAO, tag = BLOCKED_TAG, id = 7))
    }

    @Test
    fun `别人的通知不会被误判为自己发的`() {
        val tracker = SelfNotificationTracker(ownPackageName = OWN_PACKAGE)
        tracker.rememberPosted(tag = BLOCKED_TAG, id = 7)

        assertFalse(tracker.isSelfPosted(packageName = TAOBAO, tag = null, id = 0))
        assertFalse(tracker.isSelfPosted(packageName = TAOBAO, tag = "别的", id = 7))
        assertFalse(tracker.isSelfPosted(packageName = TAOBAO, tag = BLOCKED_TAG, id = 8))
    }

    /** tag 为 null 是常态（绝大多数通知不带 tag），登记与查询必须能对上。 */
    @Test
    fun `tag 为 null 的登记与查询能对上`() {
        val tracker = SelfNotificationTracker(ownPackageName = OWN_PACKAGE)
        tracker.rememberPosted(tag = null, id = 42)

        assertTrue(tracker.isSelfPosted(packageName = TAOBAO, tag = null, id = 42))
    }

    /** 登记簿必须有界：通知会持续到来，无上限地记住每个 id 就是一处内存泄漏。 */
    @Test
    fun `登记簿超出容量后淘汰最早的条目`() {
        val tracker = SelfNotificationTracker(ownPackageName = OWN_PACKAGE, capacity = 2)
        tracker.rememberPosted(tag = BLOCKED_TAG, id = 1)
        tracker.rememberPosted(tag = BLOCKED_TAG, id = 2)
        tracker.rememberPosted(tag = BLOCKED_TAG, id = 3)

        assertFalse(tracker.isSelfPosted(packageName = TAOBAO, tag = BLOCKED_TAG, id = 1))
        assertTrue(tracker.isSelfPosted(packageName = TAOBAO, tag = BLOCKED_TAG, id = 2))
        assertTrue(tracker.isSelfPosted(packageName = TAOBAO, tag = BLOCKED_TAG, id = 3))
    }

    @Test
    fun `重复登记同一个标识不会占掉两个位置`() {
        val tracker = SelfNotificationTracker(ownPackageName = OWN_PACKAGE, capacity = 2)
        tracker.rememberPosted(tag = BLOCKED_TAG, id = 1)
        tracker.rememberPosted(tag = BLOCKED_TAG, id = 1)
        tracker.rememberPosted(tag = BLOCKED_TAG, id = 2)

        // 若重复登记占掉两个位置，容量为 2 时 id=1 早已被淘汰。
        assertTrue(tracker.isSelfPosted(packageName = TAOBAO, tag = BLOCKED_TAG, id = 1))
        assertTrue(tracker.isSelfPosted(packageName = TAOBAO, tag = BLOCKED_TAG, id = 2))
    }

    @Test
    fun `替代通知不会形成回环`() {
        val tracker = SelfNotificationTracker(ownPackageName = OWN_PACKAGE)

        // 第一轮：原应用发出的通知，不属于自己。
        assertFalse(tracker.isSelfPosted(packageName = TAOBAO, tag = null, id = 0))

        // 服务的处置：在发布替代通知**之前**先登记。
        tracker.rememberPosted(tag = BLOCKED_TAG, id = REPLACEMENT_ID)

        // 第二轮：替代通知进入同一个回调。这里必须是 true，否则会再发一条替代通知并无限重复。
        assertTrue(tracker.isSelfPosted(packageName = OWN_PACKAGE, tag = BLOCKED_TAG, id = REPLACEMENT_ID))
    }
}
