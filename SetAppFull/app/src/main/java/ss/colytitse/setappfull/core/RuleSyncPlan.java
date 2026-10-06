package ss.colytitse.setappfull.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A persistence-independent merge transaction. The caller must keep the pending-reset flag and
 * dirty markers until every remote write succeeds, then persist the returned local values and
 * acknowledgement together. A failed transaction can be recalculated and retried unchanged.
 */
public final class RuleSyncPlan {
    private final Map<String, Integer> localValues;
    private final Map<String, Integer> remoteWrites;
    private final Set<String> acknowledgedDirtyKeys;

    private RuleSyncPlan(Map<String, Integer> localValues, Map<String, Integer> remoteWrites,
                         Set<String> acknowledgedDirtyKeys) {
        this.localValues = Collections.unmodifiableMap(localValues);
        this.remoteWrites = Collections.unmodifiableMap(remoteWrites);
        this.acknowledgedDirtyKeys = Collections.unmodifiableSet(acknowledgedDirtyKeys);
    }

    public static RuleSyncPlan create(Map<String, ?> localAll, Map<String, ?> remoteAll,
                                       boolean pendingReset) {
        Map<String, Integer> local = validRules(localAll);
        Map<String, Integer> remote = validRules(remoteAll);
        Set<String> keys = new LinkedHashSet<>(local.keySet());
        keys.addAll(remote.keySet());
        Map<String, Integer> result = new LinkedHashMap<>();
        Map<String, Integer> writes = new LinkedHashMap<>();
        Set<String> acknowledged = new LinkedHashSet<>();

        for (String key : keys) {
            boolean dirty = local.containsKey(key) && Boolean.TRUE.equals(localAll.get("dirty." + key));
            int value;
            if (dirty) {
                // Includes explicit edits after a pending reset. These are the newest user intent.
                value = local.get(key);
                writes.put(key, value);
                acknowledged.add(key);
            } else if (pendingReset) {
                value = 0;
                writes.put(key, 0);
            } else if (remote.containsKey(key)) {
                value = remote.get(key);
            } else {
                // A daemon reset must not silently discard this device's persisted choices.
                value = local.get(key);
                writes.put(key, value);
            }
            result.put(key, value);
        }
        return new RuleSyncPlan(result, writes, acknowledged);
    }

    /** Keys use the on-wire rule.&lt;package&gt; form. */
    public Map<String, Integer> getLocalValues() { return localValues; }
    public Map<String, Integer> getRemoteWrites() { return remoteWrites; }
    /** Returns rule keys, not the corresponding dirty-marker keys. */
    public Set<String> getAcknowledgedDirtyKeys() { return acknowledgedDirtyKeys; }

    private static Map<String, Integer> validRules(Map<String, ?> values) {
        Map<String, Integer> result = new LinkedHashMap<>();
        RuleCodec.decode(values).forEach((packageName, flags) -> result.put(RuleCodec.key(packageName), flags));
        return result;
    }
}
