package ss.colytitse.setappfull.core;

import org.junit.Test;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.*;

public class RuleSyncPlanTest {
    @Test public void offlineDisableWinsOverAnOlderEnabledRemoteRule() {
        var plan = RuleSyncPlan.create(Map.of("rule.com.app", 14, "dirty.rule.com.app", true),
                Map.of("rule.com.app", 15), false);
        assertEquals(Map.of("rule.com.app", 14), plan.getLocalValues());
        assertEquals(Map.of("rule.com.app", 14), plan.getRemoteWrites());
        assertEquals(Set.of("rule.com.app"), plan.getAcknowledgedDirtyKeys());
    }

    @Test public void offlineResetAlsoDisablesRulesNeverDownloadedToThisInstallation() {
        var plan = RuleSyncPlan.create(Map.of(),
                Map.of("rule.com.unseen", 15, "rule.com.another", 9), true);
        assertEquals(Map.of("rule.com.unseen", 0, "rule.com.another", 0), plan.getLocalValues());
        assertEquals(plan.getLocalValues(), plan.getRemoteWrites());
    }

    @Test public void enablingOneAppAfterOfflineResetPreservesThatNewDecisionOnly() {
        var plan = RuleSyncPlan.create(Map.of("rule.com.enabledLater", 15,
                        "dirty.rule.com.enabledLater", true, "rule.com.reset", 0,
                        "dirty.rule.com.reset", true),
                Map.of("rule.com.enabledLater", 0, "rule.com.reset", 15, "rule.com.unseen", 15), true);
        assertEquals(Map.of("rule.com.enabledLater", 15, "rule.com.reset", 0,
                "rule.com.unseen", 0), plan.getRemoteWrites());
    }

    @Test public void daemonDataLossRepublishesKnownPersistedLocalChoices() {
        var plan = RuleSyncPlan.create(Map.of("rule.com.enabled", 15, "rule.com.disabled", 8),
                Map.of(), false);
        assertEquals(Map.of("rule.com.enabled", 15, "rule.com.disabled", 8), plan.getRemoteWrites());
        assertTrue(plan.getAcknowledgedDirtyKeys().isEmpty());
    }

    @Test public void cleanLocalCopyAcceptsTheFrameworkValue() {
        var plan = RuleSyncPlan.create(Map.of("rule.com.app", 15), Map.of("rule.com.app", 0), false);
        assertEquals(Map.of("rule.com.app", 0), plan.getLocalValues());
        assertTrue(plan.getRemoteWrites().isEmpty());
    }

    @Test public void failedCommitMustBeRetriedEvenWhenRemoteObjectCacheAlreadyChanged() {
        // libxposed RemotePreferences updates its local map before its binder commit can fail.
        var plan = RuleSyncPlan.create(Map.of("rule.com.app", 0, "dirty.rule.com.app", true),
                Map.of("rule.com.app", 0), false);
        assertEquals(Map.of("rule.com.app", 0), plan.getRemoteWrites());
        assertEquals(Set.of("rule.com.app"), plan.getAcknowledgedDirtyKeys());
    }

    @Test public void malformedRulesAndUnrelatedPreferencesAreNeverPublished() {
        var plan = RuleSyncPlan.create(Map.of("rule.com.string", "15", "show_system", true),
                Map.of("rule.bad/key", 15, "rule.com.boolean", true), false);
        assertTrue(plan.getLocalValues().isEmpty());
        assertTrue(plan.getRemoteWrites().isEmpty());
    }
}
