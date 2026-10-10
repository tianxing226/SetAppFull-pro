package ss.colytitse.setappfull.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Shared, Android-independent wire format for module UI and target processes. */
public final class RuleCodec {
    public static final String PREF_GROUP = "rules";
    public static final String KEY_PREFIX = "rule.";
    public static final int ENABLED = 1;
    public static final int HIDE_STATUS = 1 << 1;
    public static final int HIDE_NAVIGATION = 1 << 2;
    public static final int ALLOW_CUTOUT = 1 << 3;
    /** Explicit per-app opt-in for clearing FLAG_SECURE. Kept outside ENABLED semantics. */
    public static final int ALLOW_SCREENSHOT = 1 << 4;
    /** Explicit per-app opt-in for scoped network-environment compatibility. */
    public static final int COMPAT_NETWORK_ENVIRONMENT = 1 << 5;
    /** Explicit per-app opt-out for the Bilibili-only fullscreen correction. */
    public static final int DISABLE_BILIBILI_OPTIMIZATION = 1 << 6;
    public static final int DEFAULT_ENABLED = ENABLED | HIDE_STATUS | HIDE_NAVIGATION | ALLOW_CUTOUT;
    public static final int ALL_FLAGS = DEFAULT_ENABLED | ALLOW_SCREENSHOT | COMPAT_NETWORK_ENVIRONMENT
            | DISABLE_BILIBILI_OPTIMIZATION;
    private static final Pattern PACKAGE = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+");

    private RuleCodec() {}

    public static boolean validPackage(String packageName) {
        return packageName != null && PACKAGE.matcher(packageName).matches();
    }

    public static String key(String packageName) {
        if (!validPackage(packageName)) throw new IllegalArgumentException("Invalid package name");
        return KEY_PREFIX + packageName;
    }

    public static int normalize(int flags) {
        return flags & ALL_FLAGS;
    }

    public static boolean isEnabled(int flags) {
        return (flags & ENABLED) != 0;
    }

    /** A default is allowed only for a package whose scope/injection was actually confirmed. */
    public static int resolveScopedRule(Map<String, Integer> rules, String packageName,
                                        boolean scoped, boolean snapshotAvailable) {
        if (!snapshotAvailable || !validPackage(packageName)
                || "com.android.systemui".equals(packageName)) return 0;
        Integer explicit = rules.get(packageName);
        if (explicit != null) return normalize(explicit);
        return scoped ? DEFAULT_ENABLED : 0;
    }

    /** UI/editing intent only: preserve the last known scoped choice while disconnected. A cached
     * choice does not prove current scope, module loading or visual effect. Hook code must use
     * resolveScopedRule with actual injection instead. An authoritative removal overrides cache. */
    public static int resolveDisplayRule(Map<String, Integer> rules, String packageName,
                                        Boolean inScope, boolean lastKnownScope, boolean pendingReset) {
        boolean selected = inScope == null ? lastKnownScope : inScope;
        return resolveScopedRule(rules, packageName, selected && !pendingReset, true);
    }

    /** Preserve corrupt entries as explicit off, so they never fall through to scoped defaults. */
    public static Map<String, Integer> decodeForScope(Map<String, ?> preferences) {
        Map<String, Integer> rules = new LinkedHashMap<>(decode(preferences));
        for (String key : preferences.keySet()) {
            if (key != null && key.startsWith(KEY_PREFIX)) {
                String packageName = key.substring(KEY_PREFIX.length());
                if (validPackage(packageName)) rules.putIfAbsent(packageName, 0);
            }
        }
        return Collections.unmodifiableMap(rules);
    }

    /** Disabling preserves selected options. Enabling a new, all-zero rule uses the default. */
    public static int withEnabled(int flags, boolean enabled) {
        int normalized = normalize(flags);
        if (!enabled) return normalized & ~ENABLED;
        int geometry = HIDE_STATUS | HIDE_NAVIGATION | ALLOW_CUTOUT;
        boolean independentOptionsOnly = (normalized & (ENABLED | geometry)) == 0;
        return independentOptionsOnly
                ? normalized | DEFAULT_ENABLED : normalized | ENABLED;
    }

    /** Invalid values fail closed; never coerce strings or booleans into enabled rules. */
    public static Map<String, Integer> decode(Map<String, ?> preferences) {
        Map<String, Integer> rules = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : preferences.entrySet()) {
            String name = entry.getKey();
            if (name == null || !name.startsWith(KEY_PREFIX)) continue;
            String packageName = name.substring(KEY_PREFIX.length());
            if (validPackage(packageName) && entry.getValue() instanceof Integer) {
                rules.put(packageName, normalize((Integer) entry.getValue()));
            }
        }
        return Collections.unmodifiableMap(rules);
    }

    /**
     * Converts old explicit selections, never the old enable-everything scope switch.
     * Existing new-format keys, including disabled or malformed entries, always win.
     * Returns only new key/value pairs to write, leaving ownership of persistence to the caller.
     */
    public static Map<String, Integer> migrateLegacy(Map<String, ?> legacy, Map<String, ?> existing) {
        Map<String, Integer> additions = new LinkedHashMap<>();
        for (String name : packages(legacy.get("SystemMode"))) {
            putIfMissing(additions, existing, name, DEFAULT_ENABLED);
        }
        Set<String> appMode = packages(legacy.get("AppMode"));
        appMode.addAll(packages(legacy.get("TimelyMode")));
        for (String name : appMode) {
            // Explicit app mode takes precedence over duplicate old system mode selections.
            putIfMissing(additions, existing, name, DEFAULT_ENABLED);
        }
        return Collections.unmodifiableMap(additions);
    }

    private static Set<String> packages(Object value) {
        Set<String> result = new LinkedHashSet<>();
        if (value instanceof String) {
            for (String token : ((String) value).split("#")) {
                String name = token.trim();
                if (validPackage(name)) result.add(name);
            }
        }
        return result;
    }

    private static void putIfMissing(Map<String, Integer> output, Map<String, ?> existing,
                                     String packageName, int flags) {
        String key = key(packageName);
        if (!existing.containsKey(key)) output.put(key, flags);
    }
}
