package ss.colytitse.setappfull

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ss.colytitse.setappfull.data.appListRuntimePermission
import ss.colytitse.setappfull.ui.SetAppFullApp

class MainActivity : ComponentActivity() {
    private val repository get() = (application as SetAppFullApplication).repository
    private val permissionPreferences get() = getSharedPreferences("app_list_access", MODE_PRIVATE)
    private var permissionRequestInProgress by mutableStateOf(false)
    private var permissionRequestLaunched = false
    private val appListPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        permissionRequestLaunched = false
        finishPermissionRequest()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        permissionRequestLaunched = savedInstanceState?.getBoolean(REQUEST_LAUNCHED) ?: false
        permissionRequestInProgress = savedInstanceState?.getBoolean(REQUEST_IN_PROGRESS) ?: false
        val autoPermission = if (!permissionRequestLaunched &&
            !permissionPreferences.getBoolean(AUTO_REQUESTED, false)) {
            runCatching { packageManager.appListRuntimePermission() }.getOrNull()?.takeIf {
                checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
            }
        } else null
        if (autoPermission != null) permissionRequestInProgress = true
        else if (permissionRequestInProgress && !permissionRequestLaunched) finishPermissionRequest()
        setContent {
            val state = repository.state.collectAsStateWithLifecycle().value
            SetAppFullApp(
                state = state,
                onRefresh = repository::refresh,
                onToggle = repository::toggle,
                onRuleChange = repository::setOption,
                onScopeRequest = repository::requestScope,
                onShowSystem = repository::showSystem,
                onReset = repository::reset,
                onOpenAppSettings = ::openAppSettings,
                permissionRequestInProgress = permissionRequestInProgress,
                onJoinCommunity = ::openCommunity,
            )
            LaunchedEffect(autoPermission) {
                if (autoPermission != null) {
                    // Show the purpose on the first visible app frame before the real OS dialog.
                    withFrameNanos { }
                    delay(350)
                    lifecycle.withResumed {
                        // Cancellation while stopped or rotating cannot record a request that
                        // never ran. This block and launcher call are synchronous on the UI thread.
                        if (permissionPreferences.getBoolean(AUTO_REQUESTED, false) ||
                            checkSelfPermission(autoPermission) == PackageManager.PERMISSION_GRANTED) {
                            finishPermissionRequest()
                        } else {
                            permissionPreferences.edit().putBoolean(AUTO_REQUESTED, true).apply()
                            permissionRequestLaunched = true
                            try {
                                appListPermissionLauncher.launch(autoPermission)
                            } catch (_: RuntimeException) {
                                permissionRequestLaunched = false
                                Toast.makeText(this@MainActivity,
                                    "无法打开系统权限窗口，请在系统应用设置中检查应用列表权限。",
                                    Toast.LENGTH_SHORT).show()
                                finishPermissionRequest()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        repository.refresh()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(REQUEST_IN_PROGRESS, permissionRequestInProgress)
        outState.putBoolean(REQUEST_LAUNCHED, permissionRequestLaunched)
        super.onSaveInstanceState(outState)
    }

    private fun finishPermissionRequest() {
        lifecycleScope.launch {
            repository.refreshAndAwait()
            permissionRequestInProgress = false
        }
    }

    private fun openAppSettings() {
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null)))
        } catch (_: ActivityNotFoundException) {
            showSettingsUnavailable()
        } catch (_: SecurityException) {
            showSettingsUnavailable()
        }
    }

    private fun openCommunity() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/tiaxcj")))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "请安装浏览器或 Telegram 后再打开群聊链接。", Toast.LENGTH_LONG).show()
        } catch (_: SecurityException) {
            Toast.makeText(this, "系统未允许打开链接，请访问 t.me/tiaxcj。", Toast.LENGTH_LONG).show()
        }
    }

    private fun showSettingsUnavailable() {
        Toast.makeText(this, "无法打开系统应用设置，请手动在系统设置中找到本应用。", Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val AUTO_REQUESTED = "xiaomi_auto_requested"
        const val REQUEST_IN_PROGRESS = "app_list_request_in_progress"
        const val REQUEST_LAUNCHED = "app_list_request_launched"
    }
}
