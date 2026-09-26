package xin.ctkqiang.nezha_cyber.ads_block.domain.privacy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 敏感权限清单的收录边界。
 *
 * 这里锁住的不是「清单有多长」，而是它**不**收什么：清单一旦被随手扩大，这一页就会从
 * 「谁现在能看到我」退化成一份人人都有的权限列表，用户不会再打开它（工程规则第 32 节）。
 */
class SensitivePermissionCatalogTest {
    @Test
    fun `位置相机麦克风属于敏感权限`() {
        assertTrue(SensitivePermissionCatalog.isSensitive("android.permission.ACCESS_FINE_LOCATION"))
        assertTrue(SensitivePermissionCatalog.isSensitive("android.permission.ACCESS_BACKGROUND_LOCATION"))
        assertTrue(SensitivePermissionCatalog.isSensitive("android.permission.CAMERA"))
        assertTrue(SensitivePermissionCatalog.isSensitive("android.permission.RECORD_AUDIO"))
    }

    @Test
    fun `身体与运动相关权限属于敏感权限`() {
        assertTrue(SensitivePermissionCatalog.isSensitive("android.permission.BODY_SENSORS"))
        assertTrue(SensitivePermissionCatalog.isSensitive("android.permission.ACTIVITY_RECOGNITION"))
    }

    @Test
    fun `通讯录短信与媒体属于敏感权限`() {
        assertTrue(SensitivePermissionCatalog.isSensitive("android.permission.READ_CONTACTS"))
        assertTrue(SensitivePermissionCatalog.isSensitive("android.permission.READ_SMS"))
        assertTrue(SensitivePermissionCatalog.isSensitive("android.permission.READ_MEDIA_IMAGES"))
        assertTrue(SensitivePermissionCatalog.isSensitive("android.permission.READ_CALENDAR"))
    }

    /**
     * 网络、通知这类权限人人都有，列入只会稀释这一页——用户看到每个应用都在列表里，
     * 就再也不会认真看它了。
     */
    @Test
    fun `基础能力权限不算敏感`() {
        assertFalse(SensitivePermissionCatalog.isSensitive("android.permission.INTERNET"))
        assertFalse(SensitivePermissionCatalog.isSensitive("android.permission.ACCESS_NETWORK_STATE"))
        assertFalse(SensitivePermissionCatalog.isSensitive("android.permission.POST_NOTIFICATIONS"))
        assertFalse(SensitivePermissionCatalog.isSensitive("android.permission.VIBRATE"))
    }

    /** 平台不存在运动传感器的权限，因此任何形似的名字都必须判为「不敏感」，不能靠名称猜。 */
    @Test
    fun `未收录的权限一律不算敏感`() {
        assertFalse(SensitivePermissionCatalog.isSensitive("android.permission.GYROSCOPE"))
        assertFalse(SensitivePermissionCatalog.isSensitive(""))
        assertFalse(SensitivePermissionCatalog.isSensitive("CAMERA"))
    }

    @Test
    fun `清单规模保持在可读范围内`() {
        assertTrue(
            "敏感权限清单已增长到 ${SensitivePermissionCatalog.permissionNames.size} 条，需要复核是否仍然可读",
            SensitivePermissionCatalog.permissionNames.size <= SENSITIVE_PERMISSION_LIMIT,
        )
    }

    private companion object {
        private const val SENSITIVE_PERMISSION_LIMIT = 40
    }
}
