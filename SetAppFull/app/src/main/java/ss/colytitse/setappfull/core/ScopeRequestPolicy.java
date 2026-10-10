package ss.colytitse.setappfull.core;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Durable request states. Refreshing a page or reconnecting a service is never user consent
 * to repeat an already issued or rejected authorization prompt. */
public final class ScopeRequestPolicy {
    public static final String QUEUED = "queued";
    public static final String REQUESTED = "requested";
    public static final String NEEDS_RETRY = "needs_retry";
    public static final long TIMEOUT_MILLIS = 120_000L;

    private ScopeRequestPolicy() {}

    /** Called only for a new explicit enable/request action. Keep an active prompt deduplicated. */
    public static String queue(String state) {
        return REQUESTED.equals(state) ? REQUESTED : QUEUED;
    }

    public static String reconcile(String state, boolean inScope, long issuedAt, long now) {
        if (inScope) return null;
        if (REQUESTED.equals(state) && remainingDelayMillis(issuedAt, now) == 0) return NEEDS_RETRY;
        return state;
    }

    /** Process restarts retain the original deadline instead of granting another full timeout. */
    public static long remainingDelayMillis(long issuedAt, long now) {
        if (issuedAt <= 0 || now < issuedAt) return 0;
        return Math.max(0, TIMEOUT_MILLIS - (now - issuedAt));
    }

    public static boolean shouldDispatch(String state, boolean connected, boolean inScope) {
        return connected && !inScope && QUEUED.equals(state);
    }

    /** A request without a stored rule can still be approved after reset. Include it in the
     * explicit-off records, so that late approval cannot revive the implicit scoped default. */
    public static Set<String> pendingPackages(Map<String, ?> preferences, String requestPrefix) {
        Set<String> result = new LinkedHashSet<>();
        preferences.forEach((key, value) -> {
            if (key == null || !key.startsWith(requestPrefix)) return;
            if (!QUEUED.equals(value) && !REQUESTED.equals(value) && !NEEDS_RETRY.equals(value)) return;
            String packageName = key.substring(requestPrefix.length());
            if (RuleCodec.validPackage(packageName)) result.add(packageName);
        });
        return result;
    }
}
