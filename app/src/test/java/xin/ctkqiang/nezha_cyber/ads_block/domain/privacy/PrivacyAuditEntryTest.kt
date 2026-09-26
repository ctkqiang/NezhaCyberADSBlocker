package xin.ctkqiang.nezha_cyber.ads_block.domain.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.ApplicationPermission
import xin.ctkqiang.nezha_cyber.ads_block.domain.application.InstalledApplication

private const val CAMERA = "android.permission.CAMERA"

private const val MICROPHONE = "android.permission.RECORD_AUDIO"

private const val LOCATION = "android.permission.ACCESS_FINE_LOCATION"

private const val INTERNET = "android.permission.INTERNET"

private val APPLICATION = InstalledApplication(packageName = "com.example.app", label = "示例应用")

/**
 * 审计汇总的筛选规则。
 *
 * 关键行为只有两条：**只算已授予的**，以及**没有暴露就不产生条目**。前者决定这一页说的是
 * 「现在能看到我」还是「想看到我」，后者决定它会不会被一屏无害的应用淹掉。
 */
class PrivacyAuditEntryTest {
    @Test
    fun `只统计已授予的敏感权限`() {
        val entry = PrivacyAuditEntry.of(
            application = APPLICATION,
            permissions = listOf(
                permission(name = CAMERA, label = "相机", granted = true),
                permission(name = MICROPHONE, label = "麦克风", granted = false),
                permission(name = INTERNET, label = "网络访问", granted = true, isDangerous = false),
            ),
        )

        assertEquals(1, entry?.grantedCount)
        assertEquals(listOf("相机"), entry?.grantedPermissions?.map { permission -> permission.label })
    }

    @Test
    fun `没有任何敏感权限时返回空`() {
        val entry = PrivacyAuditEntry.of(
            application = APPLICATION,
            permissions = listOf(
                permission(name = INTERNET, label = "网络访问", granted = true, isDangerous = false),
                permission(name = MICROPHONE, label = "麦克风", granted = false),
            ),
        )

        assertNull(entry)
    }

    @Test
    fun `权限信息完全读不到时返回空`() {
        assertNull(PrivacyAuditEntry.of(application = APPLICATION, permissions = emptyList()))
    }

    /** 排序用字母标签，避免依赖中文的排序规则——这里要锁的是「排过序」，不是具体的排序语言。 */
    @Test
    fun `条目按权限名称排序`() {
        val entry = PrivacyAuditEntry.of(
            application = APPLICATION,
            permissions = listOf(
                permission(name = LOCATION, label = "C", granted = true),
                permission(name = CAMERA, label = "A", granted = true),
                permission(name = MICROPHONE, label = "B", granted = true),
            ),
        )

        assertEquals(listOf("A", "B", "C"), entry?.grantedPermissions?.map { permission -> permission.label })
    }

    @Test
    fun `保留应用自身的标识`() {
        val entry = PrivacyAuditEntry.of(
            application = APPLICATION,
            permissions = listOf(permission(name = CAMERA, label = "相机", granted = true)),
        )

        assertEquals("com.example.app", entry?.packageName)
        assertEquals("示例应用", entry?.appLabel)
    }

    private fun permission(name: String, label: String, granted: Boolean, isDangerous: Boolean = true) =
        ApplicationPermission(name = name, label = label, isDangerous = isDangerous, isGranted = granted)
}
