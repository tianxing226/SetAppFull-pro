package ss.colytitse.setappfull.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo

/** Xiaomi documents this runtime permission; unknown vendor permissions are never requested.
 * https://dev.mi.com/xiaomihyperos/documentation/detail?pId=1619
 */
internal const val XIAOMI_APP_LIST_PERMISSION = "com.android.permission.GET_INSTALLED_APPS"
private const val XIAOMI_PERMISSION_OWNER = "com.lbe.security.miui"

internal fun isSupportedAppListRuntimePermission(owner: String?, protection: Int): Boolean =
    owner == XIAOMI_PERMISSION_OWNER &&
        protection and PermissionInfo.PROTECTION_MASK_BASE == PermissionInfo.PROTECTION_DANGEROUS

@Suppress("DEPRECATION")
internal fun PackageManager.appListRuntimePermission(): String? {
    val info = try {
        getPermissionInfo(XIAOMI_APP_LIST_PERMISSION, 0)
    } catch (_: PackageManager.NameNotFoundException) {
        return null
    }
    return XIAOMI_APP_LIST_PERMISSION.takeIf {
        isSupportedAppListRuntimePermission(info.packageName, info.protectionLevel)
    }
}

internal fun Context.hasAppListPermissions(): Boolean {
    if (checkSelfPermission(Manifest.permission.QUERY_ALL_PACKAGES) != PackageManager.PERMISSION_GRANTED) {
        return false
    }
    val vendorPermission = packageManager.appListRuntimePermission() ?: return true
    return checkSelfPermission(vendorPermission) == PackageManager.PERMISSION_GRANTED
}
