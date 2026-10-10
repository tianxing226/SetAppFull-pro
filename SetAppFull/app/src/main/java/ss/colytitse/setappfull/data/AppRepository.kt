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
import ss.colytitse.setappfull.core.ScopeRequestPolicy

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
    private var confirmedScope: Set<String>? = null
    private val requestTokens = mutableMapOf<String, Any>()

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
                    confirmedScope = null
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
        restorePendingScopeTimeouts()
        if (appListNeedsRefresh) refreshAppListLocked()
        val current = service
        if (current == null) {
            confirmedScope = null
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
            // Query scope before acknowledging a reset: implicit scoped defaults also need off
            // records, even if this installation never downloaded an explicit rule for them.
            val scope = current.scope.filter { it !in excluded && PACKAGE.matches(it) }.toSet()
            confirmedScope = scope
            check(local.edit().putStringSet(LAST_SCOPE, scope).commit())
            reconcileScopeRequests(scope)
            if (supported) {
                if (remote == null) remote = current.getRemotePreferences("rules")
                synchronizeRules(scope)
            } else remote = null
            if (supported) dispatchQueuedScopeRequests(current, scope)
            rebuildRows(scope)
        } catch (failure: Exception) {
            remote = null
            confirmedScope = null
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
    private fun synchronizeRules(scope: Set<String>) {
        val prefs = remote ?: return
        val plan = RuleSyncPlan.create(local.all, prefs.all, local.getBoolean("reset_pending", false), scope)
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
        val previous = currentFlags(packageName)
        writeRule(packageName, RuleCodec.withEnabled(previous, enabled), requestScope = enabled)
    }

    fun setOption(packageName: String, option: Int, enabled: Boolean) = enqueue {
        require(option == RuleCodec.HIDE_STATUS || option == RuleCodec.HIDE_NAVIGATION ||
            option == RuleCodec.ALLOW_CUTOUT || option == RuleCodec.ALLOW_SCREENSHOT ||
            option == RuleCodec.COMPAT_NETWORK_ENVIRONMENT ||
            option == RuleCodec.DISABLE_BILIBILI_OPTIMIZATION)
        // Read inside the queue: different switches may be tapped before UI state catches up.
        val previous = currentFlags(packageName)
        writeRule(packageName, if (enabled) previous or option else previous and option.inv(),
            requestScope = enabled && (option == RuleCodec.ALLOW_SCREENSHOT ||
                option == RuleCodec.COMPAT_NETWORK_ENVIRONMENT))
    }

    private fun currentFlags(packageName: String): Int = RuleCodec.resolveDisplayRule(
        RuleCodec.decodeForScope(remote?.all.orEmpty() + local.all), packageName,
        confirmedScope?.contains(packageName),
        local.getStringSet(LAST_SCOPE, emptySet()).orEmpty().contains(packageName),
        local.getBoolean("reset_pending", false),
    )

    private fun writeRule(packageName: String, flags: Int, requestScope: Boolean = false) {
        require(packageName !in excluded && PACKAGE.matches(packageName))
        val key = PREFIX + packageName
        val edit = local.edit().putInt(key, flags and RuleCodec.ALL_FLAGS).putBoolean("dirty.$key", true)
        if (requestScope) queueScopeRequest(edit, packageName)
        else if (flags and (RuleCodec.ENABLED or RuleCodec.ALLOW_SCREENSHOT or
                RuleCodec.COMPAT_NETWORK_ENVIRONMENT) == 0) {
            // A framework prompt cannot be cancelled through the public service API. Retain its
            // marker to deduplicate rapid off/on actions; its callback never changes this rule.
            if (local.getString(REQUEST_PREFIX + packageName, null) != ScopeRequestPolicy.REQUESTED) {
                edit.remove(REQUEST_PREFIX + packageName).remove(REQUEST_TIME_PREFIX + packageName)
                requestTokens.remove(packageName)
            }
        }
        check(edit.commit())
        refreshLocked()
    }

    fun showSystem(show: Boolean) = enqueue {
        check(local.edit().putBoolean("show_system", show).commit())
        refreshLocked()
    }

    fun reset() = enqueue {
        val keys = local.all.keys.filter { it.startsWith(PREFIX) }.toSet() +
            (remote?.all?.keys?.filter { it.startsWith(PREFIX) } ?: emptyList()) +
            (confirmedScope ?: local.getStringSet(LAST_SCOPE, emptySet()).orEmpty())
                .filter { it !in excluded && PACKAGE.matches(it) }.map { PREFIX + it } +
            ScopeRequestPolicy.pendingPackages(local.all, REQUEST_PREFIX)
                .filter { it !in excluded && PACKAGE.matches(it) }.map { PREFIX + it }
        val edit = local.edit()
        keys.forEach { edit.putInt(it, 0).putBoolean("dirty.$it", true) }
        local.all.forEach { (key, value) ->
            if (key.startsWith(REQUEST_PREFIX) && value is String && value != ScopeRequestPolicy.REQUESTED) {
                val packageName = key.removePrefix(REQUEST_PREFIX)
                edit.remove(key).remove(REQUEST_TIME_PREFIX + packageName)
                requestTokens.remove(packageName)
            }
        }
        check(edit.putBoolean("reset_pending", true).commit())
        refreshLocked()
    }

    fun requestScope(packageName: String) = enqueue {
        require(packageName !in excluded && PACKAGE.matches(packageName))
        check(local.edit().also { queueScopeRequest(it, packageName) }.commit())
        refreshLocked()
    }

    private fun queueScopeRequest(edit: SharedPreferences.Editor, packageName: String) {
        if (confirmedScope?.contains(packageName) == true) return
        val key = REQUEST_PREFIX + packageName
        val reconciled = ScopeRequestPolicy.reconcile(local.getString(key, null), false,
            local.getLong(REQUEST_TIME_PREFIX + packageName, 0L), System.currentTimeMillis())
        edit.putString(key, ScopeRequestPolicy.queue(reconciled))
    }

    private fun reconcileScopeRequests(scope: Set<String>) {
        val edit = local.edit()
        var changed = false
        local.all.forEach { (key, value) ->
            if (!key.startsWith(REQUEST_PREFIX) || value !is String) return@forEach
            val packageName = key.removePrefix(REQUEST_PREFIX)
            val next = ScopeRequestPolicy.reconcile(value, packageName in scope,
                local.getLong(REQUEST_TIME_PREFIX + packageName, 0L), System.currentTimeMillis())
            if (next != value) {
                changed = true
                if (next == null) edit.remove(key).remove(REQUEST_TIME_PREFIX + packageName)
                else edit.putString(key, next)
                requestTokens.remove(packageName)
            }
        }
        if (changed) check(edit.commit())
    }

    private fun dispatchQueuedScopeRequests(current: XposedService, scope: Set<String>) {
        local.all.forEach { (key, value) ->
            if (!key.startsWith(REQUEST_PREFIX) || value !is String) return@forEach
            val packageName = key.removePrefix(REQUEST_PREFIX)
            if (packageName in excluded || !PACKAGE.matches(packageName) ||
                !ScopeRequestPolicy.shouldDispatch(value, true, packageName in scope)) return@forEach
            val token = Any()
            requestTokens[packageName] = token
            // Persist before IPC, since a process restart must not issue the same dialog again.
            check(local.edit().putString(key, ScopeRequestPolicy.REQUESTED)
                .putLong(REQUEST_TIME_PREFIX + packageName, System.currentTimeMillis()).commit())
            try {
                current.requestScope(listOf(packageName), object : XposedService.OnScopeEventListener {
                    override fun onScopeRequestApproved(approved: MutableList<String>) = enqueue {
                        finishScopeRequest(packageName, token)
                    }
                    override fun onScopeRequestFailed(message: String) = enqueue {
                        finishScopeRequest(packageName, token)
                    }
                })
            } catch (failure: RuntimeException) {
                requestTokens.remove(packageName)
                check(local.edit().putString(key, ScopeRequestPolicy.NEEDS_RETRY).commit())
                Log.w("SetAppFull", "Scope request failed: ${failure.javaClass.simpleName}")
            }
            scheduleScopeTimeout(packageName, token)
        }
    }

    /** A process restart loses callbacks and timers but keeps the outstanding request marker.
     * Restore its remaining timeout even while disconnected; never dispatch another request. */
    private fun restorePendingScopeTimeouts() {
        local.all.forEach { (key, value) ->
            if (!key.startsWith(REQUEST_PREFIX) || value != ScopeRequestPolicy.REQUESTED) return@forEach
            val packageName = key.removePrefix(REQUEST_PREFIX)
            if (packageName in excluded || !PACKAGE.matches(packageName) ||
                requestTokens.containsKey(packageName)) return@forEach
            val token = Any()
            requestTokens[packageName] = token
            scheduleScopeTimeout(packageName, token)
        }
    }

    private fun scheduleScopeTimeout(packageName: String, token: Any) {
        val remaining = ScopeRequestPolicy.remainingDelayMillis(
            local.getLong(REQUEST_TIME_PREFIX + packageName, 0L), System.currentTimeMillis())
        worker.launch {
            delay(remaining)
            enqueue {
                if (requestTokens[packageName] === token) finishScopeRequest(packageName, token)
            }
        }
    }

    private fun finishScopeRequest(packageName: String, token: Any) {
        // A newer request invalidates an old callback. Never change rules here: a reset/disable
        // while the framework prompt is open must remain off even if the user later approves.
        if (requestTokens[packageName] !== token) return
        requestTokens.remove(packageName)
        check(local.edit().putString(REQUEST_PREFIX + packageName, ScopeRequestPolicy.NEEDS_RETRY).commit())
        // The callback list is not authoritative: re-read the actual scope before claiming success.
        refreshLocked()
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
        val snapshot = remote?.all.orEmpty() + local.all
        val rules = RuleCodec.decodeForScope(snapshot)
        val lastScope = local.getStringSet(LAST_SCOPE, emptySet()).orEmpty()
        val pendingReset = local.getBoolean("reset_pending", false)
        val all = appCache.orEmpty().map { row ->
            val key = PREFIX + row.packageName
            val explicit = snapshot.containsKey(key)
            val flags = RuleCodec.resolveDisplayRule(rules, row.packageName,
                scope?.contains(row.packageName), row.packageName in lastScope, pendingReset)
            val dirty = local.getBoolean("dirty.$key", false)
            val pending = local.getString(REQUEST_PREFIX + row.packageName, null)
            row.copy(
                flags = flags,
                inScope = scope?.contains(row.packageName),
                scopeRequestPending = pending == ScopeRequestPolicy.QUEUED ||
                    pending == ScopeRequestPolicy.REQUESTED,
                usesScopeDefault = !explicit && scope?.contains(row.packageName) == true,
                syncMessage = when {
                    dirty -> "已保存到本机，待连接框架同步"
                    explicit && flags and (RuleCodec.ENABLED or RuleCodec.ALLOW_SCREENSHOT or
                        RuleCodec.COMPAT_NETWORK_ENVIRONMENT) == 0 -> "全屏控制已关闭"
                    pending == ScopeRequestPolicy.REQUESTED -> "等待授权：请确认框架通知中的作用域申请"
                    pending == ScopeRequestPolicy.QUEUED -> "已保存，等待框架连接后申请作用域"
                    pending == ScopeRequestPolicy.NEEDS_RETRY -> "授权未确认；可重试或在框架作用域中勾选"
                    !explicit && scope == null && !pendingReset ->
                        if (row.packageName in lastScope) "上次使用作用域默认规则；当前连接与效果待确认"
                        else "未配置规则；等待框架连接确认作用域"
                    flags and (RuleCodec.ENABLED or RuleCodec.ALLOW_SCREENSHOT or
                        RuleCodec.COMPAT_NETWORK_ENVIRONMENT) == 0 -> "全屏控制已关闭"
                    !explicit && scope?.contains(row.packageName) == true -> "默认沉浸式已准备；重启目标应用后检查效果"
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
        private const val REQUEST_PREFIX = "scope.request."
        private const val REQUEST_TIME_PREFIX = "scope.request.time."
        private const val LAST_SCOPE = "scope.last"
        private val PACKAGE = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")
    }
}
