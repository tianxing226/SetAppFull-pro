package ss.colytitse.setappfull.core;

import org.junit.Test;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.*;
import static ss.colytitse.setappfull.core.ScopeRequestPolicy.*;

public class ScopeRequestPolicyTest {
    @Test public void offlineEnableIsQueuedUntilConnectionAndDoesNotNeedAnotherTap() {
        String state = queue(null);
        assertEquals(QUEUED, state);
        assertFalse(shouldDispatch(state, false, false));
        assertTrue(shouldDispatch(state, true, false));
        assertFalse(shouldDispatch(state, true, true));
    }

    @Test public void repeatedEnableAndRefreshNeverRepeatAnOutstandingPrompt() {
        assertEquals(REQUESTED, queue(REQUESTED));
        assertFalse(shouldDispatch(REQUESTED, true, false));
        assertFalse(shouldDispatch(REQUESTED, false, false));
        assertEquals(REQUESTED, reconcile(REQUESTED, false, 100, 1000));
    }

    @Test public void onlyAuthoritativeScopeConfirmsApproval() {
        assertEquals(NEEDS_RETRY, reconcile(NEEDS_RETRY, false, 100, 1000));
        assertNull(reconcile(NEEDS_RETRY, true, 100, 1000));
        assertNull(reconcile(REQUESTED, true, 100, 1000));
    }

    @Test public void deniedOrTimedOutRequestsRequireAnExplicitRetry() {
        assertEquals(NEEDS_RETRY, reconcile(REQUESTED, false, 100, 120100));
        assertFalse(shouldDispatch(NEEDS_RETRY, true, false));
        assertTrue(shouldDispatch(queue(NEEDS_RETRY), true, false));
    }

    @Test public void interruptedOrClockInvalidRequestsDoNotBlockTheUserForever() {
        assertEquals(NEEDS_RETRY, reconcile(REQUESTED, false, 0, 1000));
        assertEquals(NEEDS_RETRY, reconcile(REQUESTED, false, 2000, 1000));
    }

    @Test public void restartedProcessUsesRemainingTimeoutWithoutRestartingTheDeadline() {
        assertEquals(120_000L, remainingDelayMillis(1_000L, 1_000L));
        assertEquals(30_000L, remainingDelayMillis(1_000L, 91_000L));
        assertEquals(1L, remainingDelayMillis(1_000L, 120_999L));
        assertEquals(0L, remainingDelayMillis(1_000L, 121_000L));
        assertEquals(0L, remainingDelayMillis(1_000L, 200_000L));
        assertEquals(0L, remainingDelayMillis(0L, 1_000L));
        assertEquals(0L, remainingDelayMillis(2_000L, 1_000L));
        assertFalse(shouldDispatch(REQUESTED, true, false));
    }

    @Test public void lateApprovalAfterResetCannotEnableAnAppRequestedWithoutAStoredRule() {
        String packageName = "com.example.manual";
        Map<String, Object> local = new LinkedHashMap<>(Map.of(
                "scope.request." + packageName, REQUESTED,
                "scope.request.time." + packageName, 100L));
        for (String pending : pendingPackages(local, "scope.request.")) {
            local.put(RuleCodec.key(pending), 0);
            local.put("dirty." + RuleCodec.key(pending), true);
        }
        // The framework still has no scope when reset is acknowledged.
        var reset = RuleSyncPlan.create(local, Map.of(), true, Set.of());
        assertEquals(Map.of(RuleCodec.key(packageName), 0), reset.getRemoteWrites());
        // The existing prompt is approved later, after reset_pending was cleared.
        assertEquals(0, RuleCodec.resolveScopedRule(
                RuleCodec.decodeForScope(reset.getRemoteWrites()), packageName, true, true));
    }

    @Test public void pendingPackagesIncludesAllRequestStatesButNotTimestampsOrInvalidKeys() {
        assertEquals(Set.of("com.example.queued", "com.example.requested", "com.example.retry"),
                pendingPackages(Map.of(
                        "scope.request.com.example.queued", QUEUED,
                        "scope.request.com.example.requested", REQUESTED,
                        "scope.request.com.example.retry", NEEDS_RETRY,
                        "scope.request.time.com.example.queued", 100L,
                        "scope.request.bad/key", REQUESTED,
                        "scope.request.com.example.unknown", "unknown"), "scope.request."));
    }
}
