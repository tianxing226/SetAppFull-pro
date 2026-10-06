package ss.colytitse.setappfull

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ss.colytitse.setappfull.ui.SetAppFullApp

class MainActivity : ComponentActivity() {
    private val repository get() = (application as SetAppFullApplication).repository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
            )
        }
    }

    override fun onResume() {
        super.onResume()
        repository.refresh()
    }
}
