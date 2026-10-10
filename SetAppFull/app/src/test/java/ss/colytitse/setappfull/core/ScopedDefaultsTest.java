package ss.colytitse.setappfull.core;

import org.junit.Test;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.*;

/** Mirrors target initialization and repository refresh using the same wire snapshot. */
public class ScopedDefaultsTest {
    private static final String APP = "com.example.player";

    private int rule(Map<String, ?> snapshot, boolean scope, boolean readable) {
        return RuleCodec.resolveScopedRule(RuleCodec.decodeForScope(snapshot), APP, scope, readable);
    }

    @Test public void managerScopeAloneEnablesAllThreeGeometryOptionsWithoutAStoredRule() {
        assertEquals(RuleCodec.DEFAULT_ENABLED, rule(Map.of(), true, true));
        assertEquals(0, rule(Map.of(), false, true));
        assertEquals(0, rule(Map.of(), true, false));
    }

    @Test public void explicitOffCustomAndIndependentOptionsAlwaysOverrideScopeDefaults() {
        for (int value : new int[]{0, 1, 8, 9, 14, 16, 32}) {
            assertEquals(value, rule(Map.of(RuleCodec.key(APP), value), true, true));
        }
        assertEquals(0, rule(Map.of(RuleCodec.key(APP), "15"), true, true));
        assertEquals(0, rule(Map.of(RuleCodec.key(APP), true), true, true));
    }

    @Test public void removingAndReaddingScopeRetainsAnExplicitDisable() {
        var choices = Map.of(RuleCodec.key(APP), 14);
        assertEquals(14, rule(choices, true, true));
        assertEquals(14, rule(choices, false, true));
        assertEquals(14, rule(choices, true, true));
    }

    @Test public void displayRetainsLastKnownDefaultsOnlyWhileScopeIsUnknown() {
        assertEquals(15, RuleCodec.resolveDisplayRule(Map.of(), APP, null, true, false));
        assertEquals(0, RuleCodec.resolveDisplayRule(Map.of(), APP, false, true, false));
        assertEquals(0, RuleCodec.resolveDisplayRule(Map.of(), APP, null, false, false));
        assertEquals(0, RuleCodec.resolveDisplayRule(Map.of(), APP, null, true, true));
        assertEquals(0, RuleCodec.resolveDisplayRule(Map.of(APP, 0), APP, null, true, false));
        assertEquals(9, RuleCodec.resolveDisplayRule(Map.of(APP, 9), APP, null, true, false));
        // Display caching is never an authorization source for an un-injected target.
        assertEquals(0, RuleCodec.resolveScopedRule(Map.of(), APP, false, true));
    }

    @Test public void resetCreatesTombstonesForNeverConfiguredScopes() {
        var reset = RuleSyncPlan.create(Map.of(), Map.of(), true, Set.of(APP));
        assertEquals(Map.of(RuleCodec.key(APP), 0), reset.getRemoteWrites());
        assertEquals(0, rule(reset.getRemoteWrites(), true, true));
        var reconnected = RuleSyncPlan.create(reset.getLocalValues(), Map.of(), false, Set.of(APP));
        assertEquals(0, rule(reconnected.getRemoteWrites(), true, true));
    }

    @Test public void reenableAfterOfflineResetWinsButOtherNewlyDiscoveredScopesStayOff() {
        var reset = RuleSyncPlan.create(
                Map.of(RuleCodec.key(APP), 9, "dirty." + RuleCodec.key(APP), true),
                Map.of(), true, Set.of(APP, "com.other.player"));
        assertEquals(Map.of(RuleCodec.key(APP), 9, "rule.com.other.player", 0), reset.getRemoteWrites());
    }

    @Test public void criticalSystemProcessesNeverGetTheImplicitDefault() {
        assertEquals(0, RuleCodec.resolveScopedRule(Map.of(), "android", true, true));
        assertEquals(0, RuleCodec.resolveScopedRule(Map.of(), "com.android.systemui", true, true));
    }

    @Test public void bilibiliOptOutIsIndependentOfFullscreenDefaults() {
        assertEquals(RuleCodec.DEFAULT_ENABLED | RuleCodec.DISABLE_BILIBILI_OPTIMIZATION,
                RuleCodec.withEnabled(RuleCodec.DISABLE_BILIBILI_OPTIMIZATION, true));
        assertEquals(RuleCodec.DISABLE_BILIBILI_OPTIMIZATION,
                RuleCodec.withEnabled(RuleCodec.DISABLE_BILIBILI_OPTIMIZATION, false));
        assertEquals(111, RuleCodec.withEnabled(
                RuleCodec.DISABLE_BILIBILI_OPTIMIZATION | RuleCodec.COMPAT_NETWORK_ENVIRONMENT, true));
    }
}
