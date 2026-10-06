package ss.colytitse.setappfull.data

import android.content.pm.PermissionInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ss.colytitse.setappfull.ui.AppListAccess

@RunWith(AndroidJUnit4::class)
class AppListAccessTest {
    @Suppress("DEPRECATION")
    @Test
    fun declaredNormalPermissionAllowsActualInstalledAppQuery() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val result = queryAppListAccess(
            hasPermission = context::hasAppListPermissions,
            query = { context.packageManager.getInstalledApplications(0) },
        )
        assertEquals(result.message, AppListAccess.READY, result.access)
        assertNotNull(result.apps)
        assertTrue(result.apps!!.any { it.packageName == context.packageName })
    }

    @Test
    fun deniedPermissionDoesNotAttemptQueryOrReturnAnEmptySuccess() {
        var queried = false
        val result = queryAppListAccess(hasPermission = { false }, query = {
            queried = true
            listOf("visible.app")
        })
        assertFalse(queried)
        assertEquals(AppListAccess.DENIED, result.access)
        assertNull(result.apps)
    }

    @Test
    fun vendorSecurityExceptionIsDeniedEvenWhenPermissionReportsGranted() {
        val result = queryAppListAccess<String>(hasPermission = { true }, query = {
            throw SecurityException("vendor app-list restriction")
        })
        assertEquals(AppListAccess.DENIED, result.access)
        assertNull(result.apps)
    }

    @Test
    fun queryFailureDoesNotBecomeAnEmptySuccess() {
        val result = queryAppListAccess<String>(hasPermission = { true }, query = {
            throw IllegalStateException("package service unavailable")
        })
        assertEquals(AppListAccess.ERROR, result.access)
        assertNull(result.apps)
        assertTrue(result.message.contains("IllegalStateException"))
    }

    @Test
    fun unexpectedlyEmptySystemListOffersRecoveryInsteadOfSuccess() {
        val result = queryAppListAccess<String>(hasPermission = { true }, query = { emptyList() })
        assertEquals(AppListAccess.ERROR, result.access)
        assertNull(result.apps)
    }

    @Test
    fun vendorPermissionMustHaveDocumentedOwnerAndDangerousProtection() {
        assertTrue(isSupportedAppListRuntimePermission("com.lbe.security.miui",
            PermissionInfo.PROTECTION_DANGEROUS))
        assertTrue(isSupportedAppListRuntimePermission("com.lbe.security.miui",
            PermissionInfo.PROTECTION_DANGEROUS or PermissionInfo.PROTECTION_FLAG_APPOP))
        assertFalse(isSupportedAppListRuntimePermission("different.owner",
            PermissionInfo.PROTECTION_DANGEROUS))
        assertFalse(isSupportedAppListRuntimePermission("com.lbe.security.miui",
            PermissionInfo.PROTECTION_NORMAL))
        assertFalse(isSupportedAppListRuntimePermission("com.lbe.security.miui",
            PermissionInfo.PROTECTION_SIGNATURE))
    }
}
