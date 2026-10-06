package ss.colytitse.setappfull.probe

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private fun Activity.prepareWindow() {
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    if (Build.VERSION.SDK_INT >= 30) {
        window.setDecorFitsSystemWindows(true)
        window.insetsController?.show(WindowInsets.Type.systemBars())
    }
    window.decorView.viewTreeObserver.addOnGlobalLayoutListener {
        val view = window.decorView
        val insets = view.rootWindowInsets
        if (Build.VERSION.SDK_INT >= 30 && insets != null) {
            val bars = insets.getInsets(WindowInsets.Type.systemBars())
            Log.i("SAFProbe", "${javaClass.simpleName} size=${view.width}x${view.height} " +
                "status=${insets.isVisible(WindowInsets.Type.statusBars())} " +
                "nav=${insets.isVisible(WindowInsets.Type.navigationBars())} " +
                "ime=${insets.isVisible(WindowInsets.Type.ime())} insets=$bars " +
                "keepScreenOn=${window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0} " +
                "cutout=${window.attributes.layoutInDisplayCutoutMode}")
        }
    }
}

class ViewProbeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepareWindow()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(AndroidColor.rgb(226, 243, 235))
        }
        root.addView(TextView(this).apply { text = "VIEW PROBE — TOP EDGE"; textSize = 22f })
        root.addView(EditText(this).apply { hint = "Tap to test keyboard"; isSingleLine = true })
        root.addView(Button(this).apply {
            text = "Show dialog"
            setOnClickListener { AlertDialog.Builder(this@ViewProbeActivity).setTitle("Probe dialog")
                .setMessage("Dialog text and buttons must remain visible.").setPositiveButton("Close", null).show() }
        })
        listOf("Compose" to ComposeProbeActivity::class.java, "WebView" to WebProbeActivity::class.java,
            "SurfaceView" to SurfaceProbeActivity::class.java).forEach { (label, target) ->
            root.addView(Button(this).apply {
                text = label
                setOnClickListener { startActivity(Intent(this@ViewProbeActivity, target)) }
            })
        }
        root.addView(TextView(this).apply { text = "CENTER\nFlags unrelated to fullscreen must survive."; textSize = 20f },
            LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(TextView(this).apply { text = "BOTTOM EDGE"; textSize = 22f })
        setContentView(root)
    }
}

class ComposeProbeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepareWindow()
        setContent {
            MaterialTheme {
                var text by remember { mutableStateOf("") }
                Column(Modifier.fillMaxSize().background(Color(0xFFF0E9FF)).padding(20.dp)) {
                    Text("COMPOSE PROBE — TOP EDGE", style = MaterialTheme.typography.headlineSmall)
                    OutlinedTextField(text, { text = it }, label = { Text("Keyboard test") })
                    Spacer(Modifier.weight(1f))
                    Text("BOTTOM EDGE", style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
    }
}

class WebProbeActivity : Activity() {
    private var web: WebView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepareWindow()
        web = WebView(this).apply {
            settings.javaScriptEnabled = false
            loadDataWithBaseURL(null, """<html><meta name="viewport" content="width=device-width,initial-scale=1"><body style="margin:0;background:#e1f1fa;font:24px sans-serif;min-height:100vh;display:flex;flex-direction:column"><div>WEBVIEW PROBE — TOP EDGE</div><input placeholder="Keyboard test" style="font-size:22px"><div style="flex:1">Local content. No network access.</div><div>BOTTOM EDGE</div></body></html>""", "text/html", "UTF-8", null)
        }
        setContentView(web)
    }
    override fun onDestroy() { web?.destroy(); web = null; super.onDestroy() }
}

class SurfaceProbeActivity : Activity(), SurfaceHolder.Callback {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepareWindow()
        setContentView(SurfaceView(this).apply { holder.addCallback(this@SurfaceProbeActivity) })
    }
    override fun surfaceCreated(holder: SurfaceHolder) = draw(holder)
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = draw(holder)
    override fun surfaceDestroyed(holder: SurfaceHolder) {}
    private fun draw(holder: SurfaceHolder) {
        val canvas = holder.lockCanvas() ?: return
        try {
            canvas.drawColor(AndroidColor.rgb(21, 52, 66))
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.WHITE; textSize = 36f }
            canvas.drawText("SURFACE PROBE — TOP EDGE", 20f, 50f, paint)
            canvas.drawText("${canvas.width} x ${canvas.height}", 20f, canvas.height / 2f, paint)
            canvas.drawText("BOTTOM EDGE", 20f, canvas.height - 24f, paint)
        } finally { holder.unlockCanvasAndPost(canvas) }
    }
}
