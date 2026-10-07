package ss.colytitse.setappfull.probe

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Exercises a window first discovered by a flag hook before Activity.onResume. */
@Suppress("DEPRECATION")
class StatusBarRegressionActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var state: TextView
    private var dialog: AlertDialog? = null
    private var resumed = false
    private var resetCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Do not move these calls into onResume: this order reproduces the session promotion bug.
        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setDecorFitsSystemWindows(true)
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.insetsController?.show(WindowInsets.Type.statusBars())
        Log.i(TAG, "stage=onCreate-window-before-resume resumed=$resumed")

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(0xFFE6F2E9.toInt())
        }
        root.addView(TextView(this).apply {
            text = "STATUS BAR REGRESSION — TOP EDGE"
            textSize = 22f
        })
        state = TextView(this).apply { textSize = 16f }
        root.addView(state, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(Button(this).apply {
            text = "Reset flags and show status bar"
            setOnClickListener { resetBars("manual") }
        })
        root.addView(Button(this).apply {
            text = "Show floating dialog"
            setOnClickListener { showFloatingDialog() }
        })
        root.addView(TextView(this).apply { text = "BOTTOM EDGE"; textSize = 22f })
        setContentView(root)

        reportAfterLayout("created")
        if (intent.getBooleanExtra("reset_bars", true)) {
            val count = intent.getIntExtra("reset_count", 3).coerceIn(0, 10)
            val initialDelay = intent.getIntExtra("reset_delay_ms", 800).coerceIn(0, 30_000)
            val interval = intent.getIntExtra("reset_interval_ms", 1_000).coerceIn(300, 30_000)
            repeat(count) { index ->
                handler.postDelayed({ resetBars("scheduled-${index + 1}") },
                    initialDelay.toLong() + index.toLong() * interval)
            }
        }
        if (intent.getBooleanExtra("show_dialog", false)) {
            val delay = intent.getIntExtra("dialog_delay_ms", 4_200).coerceIn(0, 120_000)
            handler.postDelayed({ showFloatingDialog() }, delay.toLong())
        }
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        reportAfterLayout("resumed")
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && ::state.isInitialized) reportAfterLayout("activity-focus")
    }

    private fun resetBars(reason: String) {
        if (isFinishing || isDestroyed) return
        resetCount++
        // The three paths are intentional: attributes may mirror a module-supplied flag,
        // while clearFlags, decor visibility and InsetsController are explicit app requests.
        window.attributes = WindowManager.LayoutParams().also { it.copyFrom(window.attributes) }
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.setFlags(0, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.setDecorFitsSystemWindows(true)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.insetsController?.show(WindowInsets.Type.statusBars())
        Log.i(TAG, "stage=reset-request reason=$reason reset=$resetCount resumed=$resumed")
        reportAfterLayout("$reason-settled")
    }

    private fun showFloatingDialog() {
        if (isFinishing || isDestroyed || dialog?.isShowing == true) return
        val current = AlertDialog.Builder(this)
            .setTitle("Floating dialog regression")
            .setMessage("This dialog must retain its normal floating size. Its host Activity must regain the configured status-bar state after dismissal.")
            .setPositiveButton("Close", null)
            .create()
        dialog = current
        current.setOnDismissListener {
            if (dialog === current) dialog = null
            reportAfterLayout("dialog-dismissed")
        }
        current.show()
        handler.postDelayed({
            current.window?.let { logWindow("dialog-visible", it, true) }
        }, SETTLE_MS)
        val duration = intent.getIntExtra("dialog_duration_ms", 2_000).coerceIn(0, 120_000)
        if (duration > 0) handler.postDelayed({ if (current.isShowing) current.dismiss() }, duration.toLong())
    }

    private fun reportAfterLayout(stage: String) {
        handler.postDelayed({
            if (!isFinishing && !isDestroyed) state.text = logWindow(stage, window, false)
        }, SETTLE_MS)
    }

    private fun logWindow(stage: String, target: Window, dialogWindow: Boolean): String {
        val decor = target.decorView
        val insets = decor.rootWindowInsets
        val themeValue = TypedValue()
        val floating = target.context.theme.resolveAttribute(android.R.attr.windowIsFloating, themeValue, true)
            && themeValue.data != 0
        val attributes = target.attributes
        val report = "stage=$stage dialog=$dialogWindow floating=$floating resumed=$resumed " +
            "resets=$resetCount attached=${decor.isAttachedToWindow} size=${decor.width}x${decor.height} " +
            "status=${insets?.isVisible(WindowInsets.Type.statusBars())} " +
            "nav=${insets?.isVisible(WindowInsets.Type.navigationBars())} " +
            "insets=${insets?.getInsets(WindowInsets.Type.systemBars())} " +
            "fullscreenFlag=${attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN != 0} " +
            "keepScreenOn=${attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0} " +
            "flags=0x${Integer.toHexString(attributes.flags)} " +
            "systemUi=0x${Integer.toHexString(decor.systemUiVisibility)} " +
            "cutout=${attributes.layoutInDisplayCutoutMode}"
        Log.i(TAG, report)
        return report
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        dialog?.setOnDismissListener(null)
        dialog?.dismiss()
        dialog = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "SAFRegression"
        private const val SETTLE_MS = 350L
    }
}
