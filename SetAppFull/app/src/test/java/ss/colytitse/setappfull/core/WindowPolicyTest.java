package ss.colytitse.setappfull.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class WindowPolicyTest {
    @Test public void multiWindowPipAndDialogsSuspendPolicy() {
        assertEquals(0, WindowPolicy.effectiveRule(15, true, false, false, false));
        assertEquals(0, WindowPolicy.effectiveRule(15, false, true, false, false));
        assertEquals(0, WindowPolicy.effectiveRule(15, false, false, true, false));
    }

    @Test public void keyboardKeepsNavigationAccessibleWithoutEnablingDisabledRule() {
        assertEquals(11, WindowPolicy.effectiveRule(15, false, false, false, true));
        assertEquals(0, WindowPolicy.effectiveRule(14, false, false, false, true));
        assertEquals(15, WindowPolicy.effectiveRule(15, false, false, false, false));
    }
}
