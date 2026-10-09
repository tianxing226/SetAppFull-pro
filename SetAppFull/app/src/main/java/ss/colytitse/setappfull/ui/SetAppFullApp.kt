package ss.colytitse.setappfull.ui

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import ss.colytitse.setappfull.BuildConfig
import ss.colytitse.setappfull.core.RuleCodec
import ss.colytitse.setappfull.R
import androidx.compose.material3.ripple

private val LightPalette = lightColorScheme(
    primary = Color(0xFF6554BE), onPrimary = Color.White,
    primaryContainer = Color(0xFFECE6FF), onPrimaryContainer = Color(0xFF332467),
    secondary = Color(0xFF557B72), secondaryContainer = Color(0xFFDFF0E9),
    background = Color(0xFFF5F3F9), onBackground = Color(0xFF262332),
    surface = Color(0xFFFFFCFF), onSurface = Color(0xFF262332),
    surfaceVariant = Color(0xFFECE9F2), onSurfaceVariant = Color(0xFF777281),
    outline = Color(0xFF9991A8), outlineVariant = Color(0xFFE1DBE9),
)
private val DarkPalette = darkColorScheme(
    primary = Color(0xFFC7B9FF), onPrimary = Color(0xFF302356),
    primaryContainer = Color(0xFF3E315C), onPrimaryContainer = Color(0xFFECE5FF),
    secondary = Color(0xFFA1D3C4), secondaryContainer = Color(0xFF24453D),
    background = Color(0xFF15131D), onBackground = Color(0xFFF1EBF7),
    surface = Color(0xFF24212D), onSurface = Color(0xFFF1EBF7),
    surfaceVariant = Color(0xFF332E3E), onSurfaceVariant = Color(0xFFB5AEBF),
    outline = Color(0xFF81778F), outlineVariant = Color(0xFF3E374B),
)

@Composable
fun SetAppFullApp(
    state: UiState,
    onRefresh: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onRuleChange: (String, Int, Boolean) -> Unit,
    onScopeRequest: (String) -> Unit,
    onShowSystem: (Boolean) -> Unit,
    onReset: () -> Unit,
    onOpenAppSettings: () -> Unit = {},
    permissionRequestInProgress: Boolean = false,
    onJoinCommunity: () -> Unit = {},
) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(colorScheme = if (dark) DarkPalette else LightPalette) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        var page by rememberSaveable { mutableIntStateOf(0) }
        var detailsPackage by rememberSaveable { mutableStateOf<String?>(null) }
        var pendingSystemPackage by rememberSaveable { mutableStateOf<String?>(null) }
        var pendingScreenshotPackage by rememberSaveable { mutableStateOf<String?>(null) }
        var resetDialog by rememberSaveable { mutableStateOf(false) }
        var accessNoticeDismissed by rememberSaveable { mutableStateOf(false) }
        val accessNeedsAttention = state.appListAccess == AppListAccess.DENIED || state.appListAccess == AppListAccess.ERROR
        LaunchedEffect(state.appListAccess) {
            // A refresh passes through CHECKING; it must not reopen dismissed guidance.
            if (state.appListAccess == AppListAccess.READY) accessNoticeDismissed = false
        }
        val guardedToggle: (String, Boolean) -> Unit = { packageName, enabled ->
            if (enabled && state.apps.firstOrNull { it.packageName == packageName }?.isSystem == true) {
                pendingSystemPackage = packageName
            } else {
                onToggle(packageName, enabled)
            }
        }
        val backdrop = rememberLayerBackdrop()
        val navigationHeight = (64 * LocalDensity.current.fontScale.coerceIn(1f, 1.55f)).dp
        Box(Modifier.fillMaxSize().imePadding().background(MaterialTheme.colorScheme.background).testTag("app_root")) {
            Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                AmbientBackground(dark)
                Column(
                    Modifier.fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(Modifier.widthIn(max = 900.dp).fillMaxSize()) {
                        Header(onRefresh)
                        if (permissionRequestInProgress) {
                            Text("需要读取已安装应用，才能选择要设置全屏的应用；请在系统权限窗口中选择。",
                                Modifier.padding(horizontal = 24.dp, vertical = 8.dp).testTag("app_access_purpose"),
                                style = MaterialTheme.typography.bodyMedium)
                        }
                        if (page == 0) {
                            HomePage(state, { page = 1 }, onJoinCommunity, navigationHeight)
                        } else {
                            SettingsPage(state, guardedToggle, onShowSystem,
                                { detailsPackage = it }, { resetDialog = true }, navigationHeight,
                                onOpenAppSettings, onRefresh)
                        }
                    }
                }
            }
            GlassNavigation(page, { page = it }, backdrop, dark, navigationHeight,
                Modifier.align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                    .padding(horizontal = 28.dp, vertical = 16.dp))
        }
        if (accessNeedsAttention && !accessNoticeDismissed && !permissionRequestInProgress) {
            AlertDialog(
                onDismissRequest = { accessNoticeDismissed = true },
                modifier = Modifier.testTag("app_access_dialog"),
                title = { Text("需要检查应用列表访问") },
                text = { Text(state.appListAccessMessage) },
                confirmButton = {
                    TextButton(onClick = {
                        accessNoticeDismissed = true
                        onOpenAppSettings()
                    }) { Text("打开权限设置") }
                },
                dismissButton = {
                    TextButton(onClick = { accessNoticeDismissed = true }) { Text("稍后处理") }
                },
            )
        }
        val detailedApp = state.apps.firstOrNull { it.packageName == detailsPackage }
        if (detailedApp != null) {
            RulesDialog(detailedApp, guardedToggle, onRuleChange, onScopeRequest,
                onScreenshotChange = { packageName, enabled ->
                    if (enabled && !detailedApp.allowScreenshot) pendingScreenshotPackage = packageName
                    else onRuleChange(packageName, RuleCodec.ALLOW_SCREENSHOT, enabled)
                }) { detailsPackage = null }
        }
        val pendingScreenshotApp = state.apps.firstOrNull { it.packageName == pendingScreenshotPackage }
        if (pendingScreenshotApp != null) {
            AlertDialog(
                onDismissRequest = { pendingScreenshotPackage = null },
                modifier = Modifier.testTag("screenshot_confirm_dialog"),
                title = { Text("允许「${pendingScreenshotApp.label}」截屏？") },
                text = { Text("只对当前应用生效。DRM、银行、安全页面或硬件保护内容仍可能无法截屏；请仅用于你有权操作的应用和内容。关闭后会恢复该应用原有的安全窗口行为。") },
                confirmButton = {
                    TextButton(onClick = {
                        pendingScreenshotPackage = null
                        onRuleChange(pendingScreenshotApp.packageName, RuleCodec.ALLOW_SCREENSHOT, true)
                    }, modifier = Modifier.testTag("confirm_screenshot")) { Text("允许截屏") }
                },
                dismissButton = { TextButton(onClick = { pendingScreenshotPackage = null }) { Text("取消") } },
            )
        }
        val pendingSystemApp = state.apps.firstOrNull { it.packageName == pendingSystemPackage }
        if (pendingSystemApp != null) {
            AlertDialog(
                onDismissRequest = { pendingSystemPackage = null },
                modifier = Modifier.testTag("system_enable_dialog"),
                title = { Text("为「${pendingSystemApp.label}」开启规则？") },
                text = { Text("这会修改此系统应用的窗口显示，可能影响系统界面、输入法或相关功能。出现显示异常时，可关闭该应用的规则并重新启动该应用；部分系统组件可能需要重启设备才能恢复。") },
                confirmButton = {
                    TextButton(onClick = {
                        pendingSystemPackage = null
                        onToggle(pendingSystemApp.packageName, true)
                    }, modifier = Modifier.testTag("confirm_system_enable")) { Text("开启规则") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingSystemPackage = null },
                        modifier = Modifier.testTag("cancel_system_enable")) { Text("取消") }
                },
            )
        }
        if (resetDialog) {
            AlertDialog(
                onDismissRequest = { resetDialog = false },
                title = { Text("重置应用规则？") },
                text = { Text("将关闭所有应用的全屏规则并恢复默认选项。重新启动受影响的应用后恢复原有显示；框架作用域不会被更改。") },
                confirmButton = { TextButton(onClick = { resetDialog = false; onReset() }) { Text("重置规则") } },
                dismissButton = { TextButton(onClick = { resetDialog = false }) { Text("取消") } },
            )
        }
        }
    }
}

@Composable
private fun AmbientBackground(dark: Boolean) {
    Canvas(Modifier.fillMaxSize()) {
        val alpha = if (dark) 0.17f else 0.5f
        drawCircle(Brush.radialGradient(listOf(Color(0xFFDBCEFF).copy(alpha), Color.Transparent),
            Offset(size.width * 0.95f, size.height * 0.2f), size.width * 0.65f),
            size.width * 0.65f, Offset(size.width * 0.95f, size.height * 0.2f))
        drawCircle(Brush.radialGradient(listOf(Color(0xFFA5DCCD).copy(alpha), Color.Transparent),
            Offset(size.width * 0.12f, size.height * 0.88f), size.width * 0.7f),
            size.width * 0.7f, Offset(size.width * 0.12f, size.height * 0.88f))
        drawCircle(Brush.radialGradient(listOf(Color(0xFFC1ABFF).copy(alpha), Color.Transparent),
            Offset(size.width * 0.92f, size.height * 0.99f), size.width * 0.6f),
            size.width * 0.6f, Offset(size.width * 0.92f, size.height * 0.99f))
    }
}

@Composable
private fun Header(onRefresh: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 26.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.ic_pro_mark), null, Modifier.size(30.dp))
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).testTag("app_title"))
        IconButton(onClick = onRefresh, modifier = Modifier.testTag("refresh")) {
            Icon(AppIcons.Refresh, "刷新框架状态和应用列表", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HomePage(state: UiState, onSettings: () -> Unit, onJoinCommunity: () -> Unit,
    navigationHeight: androidx.compose.ui.unit.Dp) {
    val connected = state.status == FrameworkStatus.CONNECTED
    val statusText = when (state.status) {
        FrameworkStatus.CHECKING -> "正在检测"
        FrameworkStatus.CONNECTED -> "框架服务已连接"
        FrameworkStatus.UNAVAILABLE -> "尚未连接框架"
        FrameworkStatus.DISCONNECTED -> "框架连接已断开"
        FrameworkStatus.UNSUPPORTED -> "框架暂不兼容"
    }
    LazyColumn(Modifier.fillMaxSize().testTag("home_page"),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 18.dp,
            bottom = navigationHeight + 64.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Column(Modifier.padding(vertical = 10.dp)) {
                Text("让视野，自由延伸", color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge, letterSpacing = 1.sp)
                Spacer(Modifier.height(14.dp))
                Text("全屏，刚刚好。", fontSize = 34.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(10.dp))
                Text("为每个应用，选择适合它的显示方式。", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Surface(shape = RoundedCornerShape(30.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).background(
                            if (connected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                            Icon(if (connected) AppIcons.Check else AppIcons.Link, null,
                                tint = if (connected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(statusText, modifier = Modifier.testTag("framework_status"),
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("模块连接状态", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    InfoLine("运行框架", state.frameworkName.ifBlank { "未获取" })
                    InfoLine("框架版本", state.frameworkVersion.ifBlank { "未获取" })
                    InfoLine("Xposed API", state.frameworkApi?.toString() ?: "未获取", "framework_api")
                    if (state.statusMessage.isNotBlank()) {
                        Text(state.statusMessage, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            Surface(onClick = onSettings, shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)) {
                Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(state.enabledCount.toString(), fontSize = 38.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("  个应用", Modifier.padding(bottom = 6.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("已开启全屏规则", color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                    Icon(AppIcons.Arrow, "管理应用规则", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
        if (state.error != null) item { MessageCard(state.error, true) }
        item {
            OutlinedButton(onClick = onJoinCommunity,
                modifier = Modifier.fillMaxWidth().testTag("join_community"),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 18.dp)) {
                Icon(AppIcons.Link, null)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("加入群聊", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Text("Telegram · @tiaxcj", style = MaterialTheme.typography.bodySmall)
                }
                Icon(AppIcons.Arrow, null)
            }
        }
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    InfoLine("模块版本", BuildConfig.VERSION_NAME, "module_version")
                    InfoLine("Android 版本", "${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}", "android_version")
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("让规则真正生效", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("在设置中开启应用规则，并将应用加入框架作用域。按提示重启目标应用后检查显示效果。",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("服务连接成功不代表每个目标应用已加载模块。",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String, tag: String? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, Modifier.weight(0.8f), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, Modifier.weight(1.2f).then(if (tag != null) Modifier.testTag(tag) else Modifier),
            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SettingsPage(
    state: UiState, onToggle: (String, Boolean) -> Unit, onShowSystem: (Boolean) -> Unit,
    onDetails: (String) -> Unit, onReset: () -> Unit, navigationHeight: androidx.compose.ui.unit.Dp,
    onOpenAppSettings: () -> Unit, onRefresh: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val apps = remember(state.apps, query, filter, state.showSystem) {
        state.apps.filter { app ->
            (state.showSystem || !app.isSystem) &&
                (query.isBlank() || app.label.contains(query.trim(), ignoreCase = true) ||
                    app.packageName.contains(query.trim(), ignoreCase = true)) &&
                (filter == 0 || (filter == 1 && app.enabled) || (filter == 2 && !app.enabled))
        }
    }
    LazyColumn(Modifier.fillMaxSize().testTag("settings_page"),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 10.dp,
            bottom = navigationHeight + 64.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("应用设置", Modifier.padding(bottom = 5.dp).testTag("settings_title"), style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold)
            Text("每个应用，都有自己的全屏方式。", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.appListAccess == AppListAccess.DENIED || state.appListAccess == AppListAccess.ERROR) item {
            AppListAccessCard(state, onOpenAppSettings, onRefresh)
        }
        item {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(top = 10.dp).testTag("app_search"),
                placeholder = { Text("搜索应用名称或包名") }, leadingIcon = { Icon(AppIcons.Search, null) },
                trailingIcon = if (query.isNotEmpty()) {
                    { IconButton(onClick = { query = "" }) { Icon(AppIcons.Close, "清空搜索") } }
                } else null,
                shape = RoundedCornerShape(20.dp), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)))
        }
        item {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("全部", "已开启", "未开启").forEachIndexed { index, label ->
                    FilterChip(filter == index, { filter = index }, { Text(label) },
                        modifier = Modifier.testTag("filter_$index"))
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("显示系统应用", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Switch(state.showSystem, onShowSystem, modifier = Modifier.testTag("show_system")
                    .semantics { contentDescription = "显示系统应用" })
            }
        }
        if (state.error != null) item { MessageCard(state.error, true) }
        if (state.loading) item {
            Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("正在加载应用…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (!state.loading && apps.isEmpty() && state.appListAccess != AppListAccess.DENIED && state.appListAccess != AppListAccess.ERROR) item {
            Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(AppIcons.Search, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Text(if (query.isBlank()) "没有符合筛选条件的应用" else "没有找到这个应用",
                    style = MaterialTheme.typography.bodyLarge)
                Text("试试其他关键词或筛选条件", Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(apps, key = { it.packageName }) { app ->
            ApplicationCard(app, { onToggle(app.packageName, it) }, { onDetails(app.packageName) })
        }
        if (state.appListAccess != AppListAccess.DENIED && state.appListAccess != AppListAccess.ERROR) item {
            AppListAccessCard(state, onOpenAppSettings, onRefresh)
        }
        item {
            Column(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${apps.size} 个应用 · 点击应用可调整显示细则", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onReset, modifier = Modifier.padding(top = 10.dp).testTag("reset_rules")) {
                    Text("重置所有应用规则")
                }
            }
        }
    }
}

@Composable
private fun AppListAccessCard(state: UiState, onOpenAppSettings: () -> Unit, onRefresh: () -> Unit) {
    val ready = state.appListAccess == AppListAccess.READY
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)) {
        Column(Modifier.fillMaxWidth().padding(16.dp).testTag("app_list_access"),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(when (state.appListAccess) {
                AppListAccess.CHECKING -> "正在检查应用列表访问"
                AppListAccess.READY -> "应用列表可读取"
                AppListAccess.DENIED -> "应用列表访问受限"
                AppListAccess.ERROR -> "应用列表读取未完成"
            }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(state.appListAccessMessage.ifBlank { "启动时自动检查读取应用列表所需的访问权限。" },
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.appListAccess != AppListAccess.CHECKING) {
                TextButton(onClick = onOpenAppSettings, modifier = Modifier.testTag("open_app_permissions")) {
                    Text(if (ready) "列表不完整？检查系统权限" else "打开权限设置")
                }
            }
            if (state.appListAccess == AppListAccess.DENIED || state.appListAccess == AppListAccess.ERROR) {
                TextButton(onClick = onRefresh, modifier = Modifier.testTag("retry_app_access")) { Text("重新检查") }
            }
        }
    }
}

@Composable
private fun ApplicationCard(app: AppRow, onToggle: (Boolean) -> Unit, onDetails: () -> Unit) {
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)) {
        Row(Modifier.fillMaxWidth().testTag("app_row_${app.packageName}").clickable(onClick = onDetails)
            .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppIcon(app)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(app.label, modifier = Modifier.testTag("app_label_${app.packageName}"),
                    fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(app.packageName, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (app.enabled) {
                    Text(when (app.inScope) { true -> "已在作用域 · 效果待验证"; false -> "需要加入框架作用域"; null -> "作用域状态待确认" },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (app.inScope == false) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                }
                if (app.allowScreenshot) Text("允许截屏（仅详情页可关闭）", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary)
                if (app.syncMessage.isNotBlank()) Text(app.syncMessage, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(app.enabled, onToggle, modifier = Modifier.testTag("app_toggle_${app.packageName}")
                .semantics { contentDescription = "${app.label}全屏规则" })
        }
    }
}

@Composable
private fun AppIcon(app: AppRow) {
    val bitmap = remember(app.packageName, app.icon) { runCatching { app.icon?.toBitmap(96, 96)?.asImageBitmap() }.getOrNull() }
    if (bitmap != null) Image(bitmap, null, Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)))
    else Box(Modifier.size(44.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center) {
        Text(app.label.take(1), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RulesDialog(
    app: AppRow, onToggle: (String, Boolean) -> Unit, onRuleChange: (String, Int, Boolean) -> Unit,
    onScopeRequest: (String) -> Unit, onScreenshotChange: (String, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(onDismissRequest = onDismiss, modifier = Modifier.testTag("rules_dialog"),
        title = { Text(app.label) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                RuleRow("开启全屏规则", "此应用独立生效", app.enabled) { onToggle(app.packageName, it) }
                HorizontalDivider()
                RuleRow("隐藏状态栏", "扩大顶部可用空间", app.flags and 2 != 0, app.enabled) {
                    onRuleChange(app.packageName, 2, it)
                }
                RuleRow("隐藏导航栏", "可通过边缘手势临时呼出", app.flags and 4 != 0, app.enabled) {
                    onRuleChange(app.packageName, 4, it)
                }
                RuleRow("延伸至挖孔区域", "允许内容绘制到屏幕缺口附近", app.flags and 8 != 0, app.enabled) {
                    onRuleChange(app.packageName, 8, it)
                }
                RuleRow("允许截屏", "仅当前应用；部分 DRM 或硬件保护内容仍可能无法截屏",
                    app.allowScreenshot, enabled = true) { onScreenshotChange(app.packageName, it) }
                RuleRow("VPN 检测兼容", "仅当前应用；默认关闭，兼容常见 Android VPN 状态接口；不改变代理路由或 TLS 校验",
                    app.networkEnvironmentCompat, enabled = true) {
                    onRuleChange(app.packageName, RuleCodec.COMPAT_NETWORK_ENVIRONMENT, it)
                }
                HorizontalDivider()
                Text(when (app.inScope) {
                    true -> "已加入框架作用域。规则变化后，请重新启动此应用并检查显示效果。"
                    false -> "此应用尚未加入框架作用域，开启规则后仍需授权。"
                    null -> "暂时无法确认作用域，请检查框架连接与模块启用状态。"
                }, style = MaterialTheme.typography.bodySmall)
                if (app.inScope != true) OutlinedButton(onClick = { onScopeRequest(app.packageName) },
                    modifier = Modifier.fillMaxWidth().testTag("request_scope")) { Text("请求加入作用域") }
                if (app.syncMessage.isNotBlank()) Text(app.syncMessage, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
                Text("全屏不会改变应用自身的画面比例；应用内部留白可能仍然保留。",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("允许截屏与全屏规则独立，仅能在此详情页手动开启。关闭后恢复原应用安全窗口行为。",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
    )
}

@Composable
private fun RuleRow(label: String, explanation: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, onChange, enabled = enabled, modifier = Modifier.semantics { contentDescription = label })
    }
}

@Composable
private fun MessageCard(message: String, error: Boolean) {
    Surface(color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(18.dp)) {
        Text(message, Modifier.fillMaxWidth().padding(16.dp), style = MaterialTheme.typography.bodySmall,
            color = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun GlassNavigation(
    selected: Int, onSelect: (Int) -> Unit, backdrop: Backdrop, dark: Boolean,
    height: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(40.dp)
    val tint = if (dark) Color(0xFF292437).copy(alpha = 0.45f) else Color.White.copy(alpha = 0.42f)
    // Capture only the page behind us. The navigation is a sibling of that capture,
    // preventing recursive self-sampling and keeping labels crisp above the lens.
    val glass = if (Build.VERSION.SDK_INT >= 33) {
        Modifier.drawBackdrop(backdrop = backdrop, shape = { shape },
            effects = { blur(8.dp.toPx()); lens(20.dp.toPx(), 28.dp.toPx(), depthEffect = true) },
            highlight = { Highlight(width = 0.8.dp, alpha = if (dark) 0.65f else 0.9f) },
            onDrawSurface = { drawRect(tint) })
    } else {
        Modifier.background(if (dark) Color(0xFF292437).copy(alpha = 0.97f) else Color.White.copy(alpha = 0.96f), shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
    }
    Row(modifier.widthIn(max = 380.dp).fillMaxWidth().height(height)
        .then(glass).padding(7.dp).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("主页" to AppIcons.Home, "设置" to AppIcons.Settings).forEachIndexed { index, (label, icon) ->
            val active = selected == index
            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()
            val focused by interactionSource.collectIsFocusedAsState()
            val background by animateColorAsState(
                if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (dark) 0.82f else 0.68f)
                else Color.Transparent, label = "tabColor")
            val scale by animateFloatAsState(
                when { pressed -> 0.95f; focused -> 1.015f; active -> 1f; else -> 0.985f },
                animationSpec = spring(dampingRatio = 0.78f), label = "tabScale")
            val alpha by animateFloatAsState(if (pressed) 0.82f else 1f,
                animationSpec = spring(dampingRatio = 0.8f), label = "tabAlpha")
            Column(Modifier.weight(1f).fillMaxHeight().graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
                .clip(RoundedCornerShape(34.dp)).background(background)
                .indication(interactionSource, ripple())
                .selectable(active, role = Role.Tab, onClick = { onSelect(index) },
                    indication = null, interactionSource = interactionSource)
                .focusable()
                .testTag("nav_$index")
                .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                Icon(icon, null, Modifier.size(20.dp), tint = color)
                Spacer(Modifier.height(2.dp))
                Text(label, color = color, fontSize = 12.sp, lineHeight = 15.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}

private object AppIcons {
    private fun icon(name: String, path: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round, pathBuilder = path)
        }.build()
    val Home = icon("Home") {
        moveTo(3f, 10.5f); lineTo(12f, 3f); lineTo(21f, 10.5f)
        moveTo(5f, 9f); lineTo(5f, 21f); lineTo(10f, 21f); lineTo(10f, 14f)
        lineTo(14f, 14f); lineTo(14f, 21f); lineTo(19f, 21f); lineTo(19f, 9f)
    }
    val Settings = icon("Settings") {
        moveTo(4f, 7f); lineTo(20f, 7f); moveTo(4f, 17f); lineTo(20f, 17f)
        moveTo(9f, 4f); lineTo(9f, 10f); moveTo(15f, 14f); lineTo(15f, 20f)
    }
    val Expand = icon("Expand") {
        moveTo(9f, 3f); lineTo(3f, 3f); lineTo(3f, 9f)
        moveTo(15f, 3f); lineTo(21f, 3f); lineTo(21f, 9f)
        moveTo(3f, 15f); lineTo(3f, 21f); lineTo(9f, 21f)
        moveTo(15f, 21f); lineTo(21f, 21f); lineTo(21f, 15f)
    }
    val Refresh = icon("Refresh") {
        moveTo(20f, 7f); curveTo(16f, 1f, 7f, 2f, 4f, 8f)
        moveTo(20f, 3f); lineTo(20f, 8f); lineTo(15f, 8f)
        moveTo(4f, 17f); curveTo(8f, 23f, 17f, 22f, 20f, 16f)
        moveTo(4f, 21f); lineTo(4f, 16f); lineTo(9f, 16f)
    }
    val Check = icon("Check") { moveTo(5f, 12f); lineTo(10f, 17f); lineTo(20f, 7f) }
    val Arrow = icon("Arrow") {
        moveTo(4f, 12f); lineTo(20f, 12f); moveTo(14f, 6f); lineTo(20f, 12f); lineTo(14f, 18f)
    }
    val Search = icon("Search") {
        moveTo(17f, 10f); arcTo(7f, 7f, 0f, true, true, 3f, 10f)
        arcTo(7f, 7f, 0f, true, true, 17f, 10f); moveTo(15f, 15f); lineTo(21f, 21f)
    }
    val Close = icon("Close") { moveTo(6f, 6f); lineTo(18f, 18f); moveTo(6f, 18f); lineTo(18f, 6f) }
    val Link = icon("Link") {
        moveTo(10f, 8f); lineTo(13f, 5f); curveTo(18f, 1f, 23f, 7f, 19f, 11f); lineTo(16f, 14f)
        moveTo(14f, 16f); lineTo(11f, 19f); curveTo(6f, 23f, 1f, 17f, 5f, 13f); lineTo(8f, 10f)
        moveTo(8f, 16f); lineTo(16f, 8f)
    }
}
