package ss.colytitse.setappfull.data

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.util.Log
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ss.colytitse.setappfull.ui.AppRow
import ss.colytitse.setappfull.ui.AppListAccess
import ss.colytitse.setappfull.ui.FrameworkStatus
import ss.colytitse.setappfull.ui.UiState
import ss.colytitse.setappfull.core.RuleCodec
import ss.colytitse.setappfull.core.RuleSyncPlan

/** Application-scoped bridge. Binder and disk operations never run on the UI thread. */
class AppRepository(private val app: Application) {
    private val worker = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val operations = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private val local = app.getSharedPreferences("rules_local_v2", Context.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(UiState())
    val state = mutableState.asStateFlow()
    private var service: XposedService? = null
    private var remote: SharedPreferences? = null
    private var appCache: List<AppRow>? = null
    private var appListNeedsRefresh = true
    private var migrated = false
    private val excluded = setOf(app.packageName, "android", "com.android.systemui")

    init {
        worker.launch {
            for (operation in operations) {
                try {
                    operation()
                } catch (failure: Exception) {
                    Log.w("SetAppFull", "Operation failed: ${failure.javaClass.simpleName}")
                    mutableState.value = mutableState.value.copy(
                        loading = false,
                        error = "操作未完成（${failure.javaClass.simpleName}），请重试。未同步的设置会保留在本机。",
                    )
                }
            }
        }
        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(bound: XposedService) = enqueue {
                service = bound
                remote = null
                refreshLocked()
            }

            override fun onServiceDied(dead: XposedService) = enqueue {
                if (service === dead) {
                    service = null
                    remote = null
                    mutableState.value = mutableState.value.copy(
                        frameworkName = "", frameworkVersion = "", frameworkApi = null,
                        status = FrameworkStatus.DISCONNECTED,
                        statusMessage = "框架连接已断开，请检查模块状态后重新打开应用。",
                    )
                    rebuildRows(null)
                }
            }
        })
        refresh()
        worker.launch {
            delay(1800)
            enqueue {
                if (service == null && mutableState.value.status == FrameworkStatus.CHECKING) {
                    mutableState.value = mutableState.value.copy(
                        status = FrameworkStatus.UNAVAILABLE,
                        statusMessage = "未连接框架服务，请在 LSPosed 中启用本模块后重新打开。",
                    )
                }
            }
        }
    }

    private fun enqueue(block: suspend () -> Unit) {
        // A single consumer preserves UI call order even during rapid toggle/reset actions.
        check(operations.trySend(block).isSuccess)
    }

    fun refresh() = enqueue {
        appListNeedsRefresh = true
        refreshLocked()
    }

    /** Keep permission guidance hidden until the query after a system dialog has completed. */
    suspend fun refreshAndAwait() {
        val completed = CompletableDeferred<Unit>()
        enqueue {
            try {
                appListNeedsRefresh = true
                refreshLocked()
            } finally {
                completed.complete(Unit)
            }
        }
        completed.await()
    }

    private fun refreshLocked() {
        migrateLegacy()
        if (appListNeedsRefresh) refreshAppListLocked()
        val current = service
        if (current == null) {
            rebuildRows(null)
            return
        }
        try {
            val api = current.apiVersion
            val supported = api >= 101 &&
                current.frameworkProperties and XposedService.PROP_CAP_REMOTE != 0L
            mutableState.value = mutableState.value.copy(
                frameworkName = current.frameworkName,
                frameworkVersion = "${current.frameworkVersion} (${current.frameworkVersionCode})",
                frameworkApi = api,
                status = if (supported) FrameworkStatus.CONNECTED else FrameworkStatus.UNSUPPORTED,
                statusMessage = if (supported) "框架服务已连接；目标应用的加载与全屏效果仍需分别核验。"
                    else "当前框架不支持 API 101 或远程配置，暂不能同步规则。",
                error = null,
            )
            if (supported) {
                if (remote == null) remote = current.getRemotePreferences("rules")
                synchronizeRules()
            } else remote = null
            rebuildRows(current.scope.toSet())
        } catch (failure: Exception) {
            remote = null
            mutableState.value = mutableState.value.copy(
                frameworkName = "", frameworkVersion = "", frameworkApi = null,
                status = FrameworkStatus.DISCONNECTED,
                statusMessage = "框架通信失败，请检查模块状态后点击刷新重试。",
                error = "配置未确认同步；本地规则仍保留。",
            )
            Log.w("SetAppFull", "Framework refresh failed: ${failure.javaClass.simpleName}")
            rebuildRows(null)
        }
    }

    /** Dirty keys win over remote data, including offline disable/reset operations. */
    private fun synchronizeRules() {
        val prefs = remote ?: return
        val plan = RuleSyncPlan.create(local.all, prefs.all, local.getBoolean("reset_pending", false))
        val edit = prefs.edit()
        plan.remoteWrites.forEach { (key, value) -> edit.putInt(key, value) }
        check(edit.commit()) { "Remote commit failed" }
        val clean = local.edit()
        plan.localValues.forEach { (key, value) -> clean.putInt(key, value) }
        plan.acknowledgedDirtyKeys.forEach { clean.remove("dirty.$it") }
        clean.remove("reset_pending")
        check(clean.commit()) { "Local acknowledgement failed" }
    }

    fun toggle(packageName: String, enabled: Boolean) = enqueue {
        val previous = local.getInt(PREFIX + packageName, 0)
        writeRule(packageName, RuleCodec.withEnabled(previous, enabled))
    }

    fun setOption(packageName: String, option: Int, enabled: Boolean) = enqueue {
        require(option == 2 || option == 4 || option == 8)
        // Read inside the queue: different switches may be tapped before UI state catches up.
        val previous = local.getInt(PREFIX + packageName, 0)
        writeRule(packageName, if (enabled) previous or option else previous and option.inv())
    }

    private fun writeRule(packageName: String, flags: Int) {
        require(packageName !in excluded && PACKAGE.matches(packageName))
        val key = PREFIX + packageName
        check(local.edit().putInt(key, flags and 15).putBoolean("dirty.$key", true).commit())
        refreshLocked()
    }

    fun showSystem(show: Boolean) = enqueue {
        check(local.edit().putBoolean("show_system", show).commit())
        refreshLocked()
    }

    fun reset() = enqueue {
        val keys = local.all.keys.filter { it.startsWith(PREFIX) }.toSet() +
            (remote?.all?.keys?.filter { it.startsWith(PREFIX) } ?: emptyList())
        val edit = local.edit()
        keys.forEach { edit.putInt(it, 0).putBoolean("dirty.$it", true) }
        check(edit.putBoolean("reset_pending", true).commit())
        refreshLocked()
    }

    fun requestScope(packageName: String) = enqueue {
        require(packageName !in excluded && PACKAGE.matches(packageName))
        val current = service
        if (current == null) {
            mutableState.value = mutableState.value.copy(error = "请先在 LSPosed 中启用本模块，再申请应用作用域。")
            return@enqueue
        }
        current.requestScope(listOf(packageName), object : XposedService.OnScopeEventListener {
            override fun onScopeRequestApproved(approved: MutableList<String>) {
                refresh()
            }
            override fun onScopeRequestFailed(message: String) = enqueue {
                refreshLocked()
                mutableState.value = mutableState.value.copy(
                    error = "作用域申请未完成，请到框架管理器中勾选该应用。",
                )
            }
        })
    }

    @Suppress("DEPRECATION")
    private fun refreshAppListLocked() {
        appListNeedsRefresh = false
        mutableState.value = mutableState.value.copy(
            appListAccess = AppListAccess.CHECKING,
            appListAccessMessage = "正在检查应用列表访问权限。",
        )
        val pm = app.packageManager
        // QUERY_ALL_PACKAGES is a normal, install-time permission on Android. Calling
        // requestPermissions() cannot produce a legitimate runtime grant dialog for it.
        val result = queryAppListAccess(
            hasPermission = app::hasAppListPermissions,
            query = { pm.getInstalledApplications(0) },
        )
        result.apps?.let { infos ->
            appCache = infos.asSequence()
                .filter { it.packageName !in excluded }
                .map { info ->
                    AppRow(
                        packageName = info.packageName,
                        label = runCatching { info.loadLabel(pm).toString() }.getOrDefault(info.packageName),
                        icon = runCatching { info.loadIcon(pm) }.getOrNull(),
                        isSystem = info.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                    )
                }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label }).toList()
        }
        // Keep both cached rows and stored rules when a vendor denies or fails a query.
        // Framework synchronization below must continue independently of this result.
        mutableState.value = mutableState.value.copy(
            appListAccess = result.access,
            appListAccessMessage = result.message,
        )
    }

    private fun rebuildRows(scope: Set<String>?) {
        val show = local.getBoolean("show_system", false)
        val all = appCache.orEmpty().map { row ->
            val key = PREFIX + row.packageName
            val flags = local.getInt(key, 0)
            val dirty = local.getBoolean("dirty.$key", false)
            row.copy(
                flags = flags,
                inScope = scope?.contains(row.packageName),
                syncMessage = when {
                    dirty -> "已保存到本机，待连接框架同步"
                    flags and 1 == 0 -> "未启用全屏控制"
                    remote == null -> "规则尚未同步，等待框架连接"
                    scope == null -> "作用域与加载状态待验证"
                    row.packageName !in scope -> "尚未加入框架作用域"
                    else -> "规则已同步；重启目标应用后核验效果"
                },
            )
        }
        mutableState.value = mutableState.value.copy(
            apps = all.filter { show || !it.isSystem },
            enabledCount = if (mutableState.value.appListAccess == AppListAccess.READY) {
                all.count { it.flags and 1 != 0 }
            } else {
                local.all.count { (key, value) ->
                    val packageName = key.removePrefix(PREFIX)
                    key.startsWith(PREFIX) && packageName !in excluded &&
                        PACKAGE.matches(packageName) && value is Int && value and 1 != 0
                }
            },
            showSystem = show,
            loading = false,
        )
    }

    private fun migrateLegacy() {
        if (migrated) return
        if (!local.getBoolean("migration_complete", false)) {
            val legacy = app.getSharedPreferences("config", Context.MODE_PRIVATE)
            val rules = RuleCodec.migrateLegacy(legacy.all, local.all)
            val edit = local.edit()
            rules.forEach { (key, flags) ->
                if (key.removePrefix(PREFIX) !in excluded) {
                    edit.putInt(key, flags).putBoolean("dirty.$key", true)
                }
            }
            check(edit.putBoolean("migration_complete", true).commit())
        }
        migrated = true
    }

    companion object {
        private const val PREFIX = "rule."
        private val PACKAGE = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")
    }
}
