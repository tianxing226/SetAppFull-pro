package ss.colytitse.setappfull.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI contract tests with explicitly injected state. These do not connect to Xposed
 * and are not evidence of API 101/102 loading or Android 16/17 hook compatibility.
 */
@RunWith(AndroidJUnit4::class)
class SetAppFullUiTest {
    @get:Rule val compose = createComposeRule()

    private val alpha = AppRow("test.alpha", "阅读器")
    private val beta = AppRow("test.beta", "视频播放器", flags = 15, inScope = false)
    private val system = AppRow("test.system.clock", "系统时钟", isSystem = true)

    private fun scrollTo(tag: String, page: String = "settings_page"): SemanticsNodeInteraction {
        compose.onNodeWithTag(page).performScrollToNode(hasTestTag(tag))
        return compose.onNodeWithTag(tag)
    }

    @Composable
    private fun TestApp(
        state: MutableState<UiState>,
        onToggle: (String, Boolean) -> Unit = { _, _ -> },
        onRuleChange: (String, Int, Boolean) -> Unit = { _, _, _ -> },
        onScope: (String) -> Unit = {},
        onShowSystem: (Boolean) -> Unit = {},
        onReset: () -> Unit = {},
        onRefresh: () -> Unit = {},
        onOpenAppSettings: () -> Unit = {},
        permissionRequestInProgress: Boolean = false,
    ) {
        SetAppFullApp(state.value, onRefresh, onToggle, onRuleChange, onScope, onShowSystem, onReset,
            onOpenAppSettings, permissionRequestInProgress)
    }

    @Test
    fun deniedAppAccessPromptsOnceAndSettingsReturnCanRecover() {
        var settingsRequests = 0
        val state = mutableStateOf(UiState(appListAccess = AppListAccess.DENIED,
            appListAccessMessage = "系统限制了应用列表访问。", loading = false))
        compose.setContent { TestApp(state, onOpenAppSettings = { settingsRequests++ }) }
        compose.onNodeWithTag("app_access_dialog").assertIsDisplayed()
        compose.onNodeWithText("稍后处理").performClick()
        compose.runOnIdle { state.value = state.value.copy(frameworkApi = 102) }
        compose.onNodeWithTag("app_access_dialog").assertDoesNotExist()
        compose.runOnIdle { state.value = state.value.copy(appListAccess = AppListAccess.CHECKING) }
        compose.onNodeWithTag("app_access_dialog").assertDoesNotExist()
        compose.runOnIdle { state.value = state.value.copy(appListAccess = AppListAccess.DENIED) }
        compose.onNodeWithTag("app_access_dialog").assertDoesNotExist()
        compose.onNodeWithTag("nav_1").performClick()
        scrollTo("open_app_permissions").performClick()
        compose.runOnIdle {
            assertEquals(1, settingsRequests)
            state.value = state.value.copy(appListAccess = AppListAccess.READY,
                appListAccessMessage = "已读取可见应用。", apps = listOf(alpha))
        }
        compose.onNodeWithTag("app_access_dialog").assertDoesNotExist()
        scrollTo("app_row_test.alpha").assertIsDisplayed()
    }

    @Test
    fun realPermissionRequestDoesNotCompeteWithCustomGuidance() {
        val state = mutableStateOf(UiState(appListAccess = AppListAccess.DENIED,
            appListAccessMessage = "等待系统授权。", loading = false))
        compose.setContent { TestApp(state, permissionRequestInProgress = true) }
        compose.onNodeWithTag("app_access_purpose").assertIsDisplayed()
        compose.onNodeWithTag("app_access_dialog").assertDoesNotExist()
    }

    @Test
    fun frameworkStateAndApiAreDisplayedWithoutClaimingTargetActivation() {
        val state = mutableStateOf(UiState(status = FrameworkStatus.UNAVAILABLE, loading = false))
        compose.setContent { TestApp(state) }
        compose.onNodeWithTag("framework_status").assertTextEquals("尚未连接框架")
        scrollTo("framework_api", "home_page").assertTextEquals("未获取")

        compose.runOnIdle {
            state.value = state.value.copy(status = FrameworkStatus.CONNECTED,
                frameworkName = "Test framework", frameworkVersion = "2.2 (3100)", frameworkApi = 102)
        }
        compose.onNodeWithTag("framework_status").assertTextEquals("框架服务已连接")
        compose.onNodeWithTag("framework_api").assertTextEquals("102")
        compose.onNodeWithText("Test framework").assertExists()
        compose.onNodeWithText("2.2 (3100)").assertExists()
        compose.onNodeWithTag("home_page").performScrollToNode(hasText("服务连接成功不代表每个目标应用已加载模块。"))
        compose.onNodeWithText("服务连接成功不代表每个目标应用已加载模块。").assertIsDisplayed()
        compose.onNodeWithText("模块已激活").assertDoesNotExist()

        compose.runOnIdle {
            state.value = state.value.copy(status = FrameworkStatus.DISCONNECTED,
                frameworkName = "", frameworkVersion = "", frameworkApi = null)
        }
        scrollTo("framework_status", "home_page").assertTextEquals("框架连接已断开")
        scrollTo("framework_api", "home_page").assertTextEquals("未获取")
    }

    @Test
    fun api101IsShownAsRuntimeDataAndRefreshIsExplicit() {
        var refreshes = 0
        val state = mutableStateOf(UiState(status = FrameworkStatus.CONNECTED, frameworkApi = 101, loading = false))
        compose.setContent { TestApp(state, onRefresh = { refreshes++ }) }
        scrollTo("framework_api", "home_page").assertTextEquals("101")
        compose.onNodeWithTag("refresh").performClick()
        compose.runOnIdle { assertEquals(1, refreshes) }
    }

    @Test
    fun navigationAndToggleRespectDefaultOffState() {
        val state = mutableStateOf(UiState(apps = listOf(alpha), loading = false))
        val toggles = mutableListOf<Pair<String, Boolean>>()
        compose.setContent {
            TestApp(state, onToggle = { packageName, enabled ->
                toggles += packageName to enabled
                state.value = state.value.copy(apps = state.value.apps.map {
                    if (it.packageName == packageName) it.copy(flags = if (enabled) 15 else 14) else it
                })
            })
        }
        compose.onNodeWithTag("nav_0").assertIsSelected()
        compose.onNodeWithTag("nav_1").performClick().assertIsSelected()
        compose.onNodeWithTag("home_page").assertDoesNotExist()
        scrollTo("app_toggle_test.alpha").assertIsOff().performClick().assertIsOn()
        compose.runOnIdle { assertEquals(listOf("test.alpha" to true), toggles) }
        compose.onNodeWithTag("nav_0").performClick().assertIsSelected()
        compose.onNodeWithTag("settings_page").assertDoesNotExist()
        compose.onNodeWithTag("home_page").assertIsDisplayed()
    }

    @Test
    fun searchAndEnabledFiltersUseNameAndPackage() {
        val state = mutableStateOf(UiState(apps = listOf(alpha, beta, system), loading = false))
        compose.setContent { TestApp(state) }
        compose.onNodeWithTag("nav_1").performClick()
        compose.onNodeWithTag("filter_1").performClick()
        compose.onNodeWithTag("app_row_test.alpha").assertDoesNotExist()
        compose.onNodeWithTag("app_row_test.beta").assertExists()
        compose.onNodeWithTag("filter_2").performClick()
        compose.onNodeWithTag("app_row_test.beta").assertDoesNotExist()
        compose.onNodeWithTag("app_row_test.alpha").assertExists()
        compose.onNodeWithTag("filter_0").performClick()
        compose.onNodeWithTag("app_search").performTextInput("阅读")
        compose.onNodeWithTag("app_row_test.alpha").assertExists()
        compose.onNodeWithTag("app_row_test.beta").assertDoesNotExist()
        compose.onNodeWithTag("app_search").performTextClearance()
        compose.onNodeWithTag("app_search").performTextInput("TEST.BETA")
        compose.onNodeWithTag("app_row_test.alpha").assertDoesNotExist()
        compose.onNodeWithTag("app_row_test.beta").assertExists()
        compose.onNodeWithTag("app_search").performTextClearance()
        compose.onNodeWithTag("app_search").performTextInput("不存在的应用")
        compose.onNodeWithText("没有找到这个应用").assertExists()
    }

    @Test
    fun systemAppsAreOptInAndScopeRequestsDoNotPretendToGrantScope() {
        val state = mutableStateOf(UiState(apps = listOf(system.copy(inScope = false)), loading = false))
        val requests = mutableListOf<String>()
        var showSystemRequests = 0
        compose.setContent {
            TestApp(state, onScope = { requests += it }, onShowSystem = {
                showSystemRequests++
                state.value = state.value.copy(showSystem = it)
            })
        }
        compose.onNodeWithTag("nav_1").performClick()
        compose.onNodeWithTag("show_system").assertIsOff()
        compose.onNodeWithTag("app_row_test.system.clock").assertDoesNotExist()
        compose.onNodeWithTag("show_system").performClick().assertIsOn()
        scrollTo("app_row_test.system.clock").performClick()
        compose.onNodeWithTag("rules_dialog").assertIsDisplayed()
        compose.onNodeWithTag("request_scope").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, showSystemRequests)
            assertEquals(listOf("test.system.clock"), requests)
            assertEquals(false, state.value.apps.single().inScope)
        }
        compose.onNodeWithText("此应用尚未加入框架作用域，开启规则后仍需授权。").assertExists()
    }

    @Test
    fun changingRulesEmitsIndependentOperationsBeforeRepositoryAcknowledgement() {
        val state = mutableStateOf(UiState(apps = listOf(beta), loading = false))
        val changes = mutableListOf<Triple<String, Int, Boolean>>()
        compose.setContent {
            TestApp(state, onRuleChange = { packageName, optionBit, enabled ->
                // Deliberately keep the original UiState until both actions have arrived.
                changes += Triple(packageName, optionBit, enabled)
            })
        }
        compose.onNodeWithTag("nav_1").performClick()
        scrollTo("app_row_test.beta").performClick()
        compose.onNodeWithContentDescription("隐藏状态栏").performScrollTo().assertIsOn().performClick()
        compose.onNodeWithContentDescription("隐藏导航栏").assertIsOn().performClick()
        compose.onNodeWithContentDescription("延伸至挖孔区域").assertIsOn()
        compose.runOnIdle {
            assertEquals(listOf(Triple("test.beta", 2, false), Triple("test.beta", 4, false)), changes)
        }
    }

    @Test
    fun screenshotPermissionIsOffByDefaultAndOnlyDetailsCanEnableIt() {
        val state = mutableStateOf(UiState(apps = listOf(alpha), loading = false))
        val changes = mutableListOf<Triple<String, Int, Boolean>>()
        compose.setContent {
            TestApp(state, onRuleChange = { packageName, option, enabled ->
                changes += Triple(packageName, option, enabled)
                state.value = state.value.copy(apps = state.value.apps.map {
                    if (it.packageName == packageName) it.copy(flags = if (enabled) it.flags or option else it.flags and option.inv()) else it
                })
            })
        }
        compose.onNodeWithTag("nav_1").performClick()
        scrollTo("app_row_test.alpha").performClick()
        compose.onNodeWithContentDescription("允许截屏").performScrollTo().assertIsOff().performClick()
        compose.onNodeWithTag("screenshot_confirm_dialog").assertIsDisplayed()
        compose.runOnIdle { assertTrue(changes.isEmpty()) }
        compose.onNodeWithText("取消").performClick()
        compose.onNodeWithContentDescription("允许截屏").assertIsOff().performClick()
        compose.onNodeWithTag("confirm_screenshot").performClick()
        compose.runOnIdle {
            assertEquals(listOf(Triple("test.alpha", 16, true)), changes)
        }
        compose.onNodeWithTag("rules_dialog").assertIsDisplayed()
        compose.onNodeWithContentDescription("允许截屏").assertIsOn()
    }

    @Test
    fun fullscreenToggleDoesNotImplicitlyEnableScreenshot() {
        val state = mutableStateOf(UiState(apps = listOf(alpha), loading = false))
        val toggles = mutableListOf<Pair<String, Boolean>>()
        val changes = mutableListOf<Triple<String, Int, Boolean>>()
        compose.setContent {
            TestApp(state, onToggle = { packageName, enabled -> toggles += packageName to enabled },
                onRuleChange = { packageName, option, enabled -> changes += Triple(packageName, option, enabled) })
        }
        compose.onNodeWithTag("nav_1").performClick()
        scrollTo("app_toggle_test.alpha").performClick()
        compose.runOnIdle {
            assertEquals(listOf("test.alpha" to true), toggles)
            assertTrue(changes.isEmpty())
        }
    }

    @Test
    fun enablingSystemAppNeedsConfirmationFromBothEntryPointsButDisablingDoesNot() {
        val state = mutableStateOf(UiState(apps = listOf(system), showSystem = true, loading = false))
        val toggles = mutableListOf<Pair<String, Boolean>>()
        compose.setContent {
            TestApp(state, onToggle = { packageName, enabled ->
                toggles += packageName to enabled
                state.value = state.value.copy(apps = listOf(system.copy(flags = if (enabled) 15 else 14)))
            })
        }
        compose.onNodeWithTag("nav_1").performClick()
        scrollTo("app_toggle_test.system.clock").performClick()
        compose.onNodeWithTag("system_enable_dialog").assertIsDisplayed()
        compose.onNodeWithText("为「系统时钟」开启规则？").assertIsDisplayed()
        compose.runOnIdle { assertTrue(toggles.isEmpty()) }
        compose.onNodeWithTag("cancel_system_enable").performClick()
        compose.onNodeWithTag("app_toggle_test.system.clock").assertIsOff()
        compose.runOnIdle { assertTrue(toggles.isEmpty()) }

        scrollTo("app_row_test.system.clock").performClick()
        compose.onNodeWithContentDescription("开启全屏规则").performScrollTo().performClick()
        compose.onNodeWithTag("system_enable_dialog").assertIsDisplayed()
        compose.runOnIdle { assertTrue(toggles.isEmpty()) }
        compose.onNodeWithTag("confirm_system_enable").performClick()
        compose.onNodeWithContentDescription("开启全屏规则").assertIsOn().performClick().assertIsOff()
        compose.onNodeWithTag("system_enable_dialog").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf("test.system.clock" to true, "test.system.clock" to false), toggles) }
    }

    @Test
    fun resettingRulesRequiresConfirmationAndCancellationDoesNothing() {
        var resets = 0
        val state = mutableStateOf(UiState(apps = listOf(alpha), loading = false))
        compose.setContent { TestApp(state, onReset = { resets++ }) }
        compose.onNodeWithTag("nav_1").performClick()
        scrollTo("reset_rules").performClick()
        compose.onNodeWithText("重置应用规则？").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, resets) }
        compose.onNodeWithText("取消").performClick()
        compose.runOnIdle { assertEquals(0, resets) }
        compose.onNodeWithTag("reset_rules").performClick()
        compose.onNodeWithText("重置规则").performClick()
        compose.runOnIdle { assertEquals(1, resets) }
    }

    @Test
    fun narrowLayoutAtDoubleFontScaleKeepsBothTabsAndFiltersReachable() {
        val state = mutableStateOf(UiState(apps = listOf(alpha), loading = false))
        compose.setContent {
            val deviceDensity = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(deviceDensity, 2f)) {
                Box(Modifier.width(360.dp).height(720.dp)) { TestApp(state) }
            }
        }
        compose.onNodeWithTag("nav_0").assertIsDisplayed()
        compose.onNodeWithTag("nav_1").assertIsDisplayed().performClick()
        fun unobscuredFilter(tag: String): SemanticsNodeInteraction {
            val target = scrollTo(tag)
            val navTop = compose.onNodeWithTag("nav_0").fetchSemanticsNode().boundsInRoot.top
            val delta = target.fetchSemanticsNode().boundsInRoot.bottom - navTop + 24f
            if (delta > 0f) compose.onNodeWithTag("settings_page")
                .performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, delta) }
            return target.assertIsDisplayed()
        }
        unobscuredFilter("filter_2").performClick()
        compose.onNodeWithTag("settings_page").assertExists()
        val root = compose.onNodeWithTag("app_root").fetchSemanticsNode().boundsInRoot
        listOf("nav_0", "nav_1", "filter_0", "filter_1", "filter_2").forEach { tag ->
            // Wrapped filters can be clipped at opposite ends of a small viewport.
            // Verify each remains reachable instead of requiring simultaneous visibility.
            if (tag.startsWith("filter_")) unobscuredFilter(tag)
            val bounds = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
            assertTrue("$tag must fit horizontally", bounds.left >= root.left - 1f && bounds.right <= root.right + 1f)
        }
        scrollTo("app_toggle_test.alpha").assertIsDisplayed().assertIsOff()
        compose.onNodeWithTag("nav_0").assertIsDisplayed().performClick()
    }

    @Test
    fun darkThemeSuppliesReadableContentColorToTitlesAndTranslucentCards() {
        val state = mutableStateOf(UiState(apps = listOf(alpha), loading = false))
        compose.setContent {
            val darkConfiguration = Configuration(LocalConfiguration.current).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES
            }
            CompositionLocalProvider(LocalConfiguration provides darkConfiguration) { TestApp(state) }
        }
        compose.onNodeWithTag("nav_1").performClick()
        listOf("app_title", "settings_title", "app_label_test.alpha").forEach { tag ->
            if (tag != "app_title") scrollTo(if (tag.startsWith("app_label")) "app_row_test.alpha" else tag)
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithTag(tag, useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> action(layouts) }
            assertTrue("$tag must expose a text layout", layouts.isNotEmpty())
            assertTrue("$tag must use light text on the dark background", layouts.first().layoutInput.style.color.luminance() > 0.4f)
        }
    }
}
