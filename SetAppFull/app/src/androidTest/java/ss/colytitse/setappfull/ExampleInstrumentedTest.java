package ss.colytitse.setappfull;

import android.content.Context;
import android.content.Intent;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("io.github.tianxing226.setappfullpro", appContext.getPackageName());
        assertEquals(BuildConfig.APPLICATION_ID, appContext.getPackageName());
        // The independent APK ID must still resolve components in the retained source namespace.
        Intent launch = appContext.getPackageManager().getLaunchIntentForPackage(appContext.getPackageName());
        assertNotNull(launch);
        assertNotNull(launch.getComponent());
        assertEquals(appContext.getPackageName(), launch.getComponent().getPackageName());
        assertEquals(MainActivity.class.getName(), launch.getComponent().getClassName());
        assertTrue(appContext.getApplicationContext() instanceof SetAppFullApplication);
    }
}
