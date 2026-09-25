package xin.ctkqiang.nezha_cyber.ads_block.notification.platform

/** 登记簿的容量上限。只用于识别「刚刚自己发出去的那几条」，因此不需要很大。 */
private const val TRACKED_CAPACITY = 32

/**
 * 识别「这条通知是不是本应用自己发出去的」。
 *
 * 拦截一条通知之后，本应用会**代替原应用发布一条标记了拦截结果的通知**。这条替代通知同样会
 * 进入 `onNotificationPosted`，如果被再次判定，就会发出第三条、第四条……形成回环。
 *
 * 两道判据，缺一不可：
 * 1. **包名**——本应用发布的通知必然带本应用的包名，这是平台保证的事实，也是最可靠的判据；
 * 2. **自己登记过的 (tag, id) 组合**——覆盖包名判据失效的场景（将来若把替代通知交给独立进程
 *    发布，包名就不再是本应用了）。
 *
 * **刻意不用通知文字当判据。** 文字正是用户规则要匹配的内容：只要用户配了一条包含「哪吒反广」
 * 的规则，用文字识别自己就会立刻失效，而且失效方式恰好是死循环——最不该出错的地方。
 *
 * 登记簿有容量上限并按插入顺序淘汰：通知会持续到来，无上限地记住每一个 id 就是一处内存泄漏。
 * 被淘汰的条目仍然由包名判据兜住，因此缩小容量只会让第二道判据变弱，不会让回环重新出现。
 */
internal class SelfNotificationTracker(
    private val ownPackageName: String,
    private val capacity: Int = TRACKED_CAPACITY,
) {
    private val postedKeys = LinkedHashSet<String>()

    fun isSelfPosted(packageName: String, tag: String?, id: Int): Boolean =
        packageName == ownPackageName || postedKeys.contains(keyOf(tag = tag, id = id))

    /** 在发布替代通知之前登记，而不是之后：发布与回调之间没有先后保证，先登记才不会漏。 */
    fun rememberPosted(tag: String?, id: Int) {
        val key = keyOf(tag = tag, id = id)
        postedKeys.remove(key)
        postedKeys.add(key)
        while (postedKeys.size > capacity) {
            val oldest = postedKeys.firstOrNull() ?: break
            postedKeys.remove(oldest)
        }
    }

    /** tag 为 null 是常态（绝大多数通知不带 tag），用空串占位即可，不会与真实 tag 冲突到有意义。 */
    private fun keyOf(tag: String?, id: Int): String = "${tag.orEmpty()}#$id"
}
