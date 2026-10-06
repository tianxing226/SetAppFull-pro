package ss.colytitse.setappfull.data

import ss.colytitse.setappfull.ui.AppListAccess

/** A failed query never masquerades as a successful empty application list. */
internal data class AppListQueryResult<T>(
    val access: AppListAccess,
    val apps: List<T>? = null,
    val message: String,
)

internal fun <T> queryAppListAccess(
    hasPermission: () -> Boolean,
    query: () -> List<T>,
): AppListQueryResult<T> = try {
    if (!hasPermission()) {
        AppListQueryResult(AppListAccess.DENIED,
            message = "系统尚未允许读取应用列表，请在系统应用设置中检查相关权限后重试。")
    } else {
        val apps = query()
        if (apps.isEmpty()) {
            // Even the querying application itself should be visible on stock Android.
            AppListQueryResult(AppListAccess.ERROR,
                message = "系统未返回应用列表，请检查系统应用设置后重试。")
        } else {
            AppListQueryResult(AppListAccess.READY, apps,
                "已可读取系统允许显示的应用列表。")
        }
    }
} catch (_: SecurityException) {
    AppListQueryResult(AppListAccess.DENIED,
        message = "系统拒绝读取应用列表，请在系统应用设置中检查相关权限后重试。")
} catch (failure: Exception) {
    AppListQueryResult(AppListAccess.ERROR,
        message = "读取应用列表失败（${failure.javaClass.simpleName}），请稍后重试。")
}
