package xin.ctkqiang.nezha_cyber.ads_block.domain.privacy

/**
 * 需要用户特别关注的权限清单。
 *
 * 「敏感」在这里有明确定义：一旦授予，应用就能拿到**关于用户本人或其所处环境**的信息，
 * 而不只是访问自己的沙箱。它不等于平台的 `PROTECTION_DANGEROUS`——后者是机制分类、
 * 会随版本调整，而这里是**面向用户的风险分类**，因此单独维护一份显式清单。
 *
 * 刻意只收高置信度的一小撮，而不是把危险权限全量搬进来：清单越长，用户越不会看，
 * 最终等于没有。
 *
 * 一处必须写清的边界：**陀螺仪与加速度计不在这份清单里，也不在任何权限里。**
 * 平台没有为运动传感器设权限，因此它既无法被授予、也无法被撤销——既不是本应用的缺陷，
 * 也不是本清单的遗漏。与运动有关、且真正可撤销的只有 [BODY_SENSORS] 与 [ACTIVITY_RECOGNITION]。
 * 这条边界由 `PrivacyAuditScreen` 如实讲给用户，不允许含糊成「已支持关闭传感器」。
 */
object SensitivePermissionCatalog {
    private val LOCATION_PERMISSIONS = setOf(
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
    )

    private val CAMERA_PERMISSIONS = setOf("android.permission.CAMERA")

    private val MICROPHONE_PERMISSIONS = setOf("android.permission.RECORD_AUDIO")

    /** 身体与运动。注意这里没有陀螺仪相关的条目——平台根本没有为它设权限。 */
    private val BODY_AND_MOTION_PERMISSIONS = setOf(
        "android.permission.BODY_SENSORS",
        "android.permission.ACTIVITY_RECOGNITION",
    )

    private val CONTACT_PERMISSIONS = setOf(
        "android.permission.READ_CONTACTS",
        "android.permission.WRITE_CONTACTS",
        "android.permission.GET_ACCOUNTS",
    )

    private val MESSAGE_AND_CALL_PERMISSIONS = setOf(
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.SEND_SMS",
        "android.permission.READ_CALL_LOG",
        "android.permission.WRITE_CALL_LOG",
        "android.permission.CALL_PHONE",
    )

    private val MEDIA_PERMISSIONS = setOf(
        "android.permission.READ_MEDIA_IMAGES",
        "android.permission.READ_MEDIA_VIDEO",
        "android.permission.READ_MEDIA_AUDIO",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
    )

    private val CALENDAR_PERMISSIONS = setOf(
        "android.permission.READ_CALENDAR",
        "android.permission.WRITE_CALENDAR",
    )

    /** 全部条目。用集合而不是列表：这里唯一的用法就是「在不在里面」。 */
    val permissionNames: Set<String> = buildSet {
        addAll(LOCATION_PERMISSIONS)
        addAll(CAMERA_PERMISSIONS)
        addAll(MICROPHONE_PERMISSIONS)
        addAll(BODY_AND_MOTION_PERMISSIONS)
        addAll(CONTACT_PERMISSIONS)
        addAll(MESSAGE_AND_CALL_PERMISSIONS)
        addAll(MEDIA_PERMISSIONS)
        addAll(CALENDAR_PERMISSIONS)
    }

    /** 是否属于需要特别关注的权限。未收录的一律返回 false。 */
    fun isSensitive(permissionName: String): Boolean = permissionName in permissionNames
}
