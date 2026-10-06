package ss.colytitse.setappfull.ui

import android.graphics.drawable.Drawable

/** The service connection and each target's scope are intentionally separate states. */
enum class FrameworkStatus { CHECKING, CONNECTED, UNAVAILABLE, DISCONNECTED, UNSUPPORTED }

data class UiState(
    val frameworkName: String = "",
    val frameworkVersion: String = "",
    val frameworkApi: Int? = null,
    val status: FrameworkStatus = FrameworkStatus.CHECKING,
    val statusMessage: String = "",
    val enabledCount: Int = 0,
    val apps: List<AppRow> = emptyList(),
    val showSystem: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
)

data class AppRow(
    val packageName: String,
    val label: String,
    val icon: Drawable? = null,
    val flags: Int = 0,
    val inScope: Boolean? = null,
    val isSystem: Boolean = false,
    val syncMessage: String = "",
) {
    val enabled: Boolean get() = flags and 1 != 0
}
