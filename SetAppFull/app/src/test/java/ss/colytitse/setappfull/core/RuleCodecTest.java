package ss.colytitse.setappfull.core;

import org.junit.Test;
import java.util.Map;
import static org.junit.Assert.*;

public class RuleCodecTest {
    @Test public void disablingPreservesOptionsAndNewAppsStartDisabled() {
        assertFalse(RuleCodec.isEnabled(0));
        assertEquals(15, RuleCodec.withEnabled(0, true));
        assertEquals(8, RuleCodec.withEnabled(9, false));
        assertEquals(9, RuleCodec.withEnabled(8, true));
        assertEquals(1, RuleCodec.withEnabled(1, true));
    }

    @Test public void wireFormatRejectsMalformedTypesAndKeys() {
        var decoded = RuleCodec.decode(Map.of(
                "rule.com.valid", 255,
                "rule.com.string", "15",
                "rule.com.boolean", true,
                "rule.bad/name", 15,
                "unrelated", 15));
        assertEquals(Map.of("com.valid", 15), decoded);
    }

    @Test public void migratesBothHistoricalAppKeysWithoutEnablingAllScope() {
        var migrated = RuleCodec.migrateLegacy(Map.of(
                "AppMode", "#com.first##com.duplicate#",
                "TimelyMode", "#com.second##com.duplicate#",
                "SystemMode", "#com.cutout##com.duplicate#",
                "scope_mode_switch", true), Map.of());
        assertEquals(Map.of("rule.com.first", 15, "rule.com.second", 15,
                "rule.com.duplicate", 15, "rule.com.cutout", 9), migrated);
    }

    @Test public void migrationNeverOverwritesAnExplicitNewDecision() {
        var existing = Map.of("rule.com.first", 0, "rule.com.second", 8);
        assertTrue(RuleCodec.migrateLegacy(
                Map.of("AppMode", "#com.first##com.second#"), existing).isEmpty());
    }

    @Test public void migrationIgnoresCorruptLegacyData() {
        assertTrue(RuleCodec.migrateLegacy(Map.of("AppMode", 42,
                "SystemMode", "#android##../x##bad name#"), Map.of()).isEmpty());
    }

    @Test public void decodedSnapshotIsImmutable() {
        try {
            RuleCodec.decode(Map.of("rule.com.app", 15)).put("com.other", 15);
            fail("Snapshot must be immutable across threads");
        } catch (UnsupportedOperationException expected) {
            // Reader caches can be safely shared with window callbacks.
        }
    }
}
