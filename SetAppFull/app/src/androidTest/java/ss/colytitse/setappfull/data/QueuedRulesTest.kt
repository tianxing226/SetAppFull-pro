package ss.colytitse.setappfull.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import ss.colytitse.setappfull.SetAppFullApplication

/** Real repository + SharedPreferences; only changes this run's instrumentation package. */
@RunWith(AndroidJUnit4::class)
class QueuedRulesTest {
    @Test
    fun independentOptionsPreserveEarlierQueuedChangesAndSurviveReload() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as SetAppFullApplication
        val repository = app.repository
        val target = instrumentation.context.packageName
        repository.refresh()
        val initial = withTimeout(15_000) { repository.state.first { state ->
            state.apps.any { it.packageName == target }
        } }.apps.first { it.packageName == target }.flags
        suspend fun awaitFlags(expected: Int) {
            withTimeout(15_000) { repository.state.first { state ->
                state.apps.any { it.packageName == target && it.flags == expected }
            } }
        }
        try {
            repository.toggle(target, false)
            listOf(2, 4, 8).forEach { repository.setOption(target, it, false) }
            awaitFlags(0)

            // Enqueue without waiting for recomposition or disk I/O between operations.
            repository.toggle(target, true)
            repository.setOption(target, 2, false)
            repository.setOption(target, 4, false)
            awaitFlags(9)
            assertEquals(9, app.getSharedPreferences("rules_local_v2", 0).getInt("rule.$target", -1))

            repository.toggle(target, false)
            awaitFlags(8)
            repository.refresh()
            awaitFlags(8)
            assertEquals(8, app.getSharedPreferences("rules_local_v2", 0).getInt("rule.$target", -1))
        } finally {
            repository.toggle(target, initial and 1 != 0)
            listOf(2, 4, 8).forEach { repository.setOption(target, it, initial and it != 0) }
            awaitFlags(initial)
        }
    }
}
