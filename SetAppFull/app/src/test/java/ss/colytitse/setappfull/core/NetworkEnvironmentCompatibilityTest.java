package ss.colytitse.setappfull.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NetworkEnvironmentCompatibilityTest {
    @Test public void recognizesVpnSignalsWithoutChangingUnrelatedValues() {
        assertTrue(NetworkEnvironmentCompatibility.isVpnTransport(4));
        assertFalse(NetworkEnvironmentCompatibility.isVpnTransport(1));
        assertTrue(NetworkEnvironmentCompatibility.isNotVpnCapability(15));
        assertFalse(NetworkEnvironmentCompatibility.isNotVpnCapability(16));
        assertTrue(NetworkEnvironmentCompatibility.isVpnNetworkType(17));
        assertFalse(NetworkEnvironmentCompatibility.isVpnNetworkType(1));
    }

    @Test public void unrelatedSignalsAreNotTreatedAsVpn() {
        assertFalse(NetworkEnvironmentCompatibility.isVpnTransport(1));
        assertFalse(NetworkEnvironmentCompatibility.isNotVpnCapability(16));
        assertFalse(NetworkEnvironmentCompatibility.isVpnNetworkType(1));
    }
}
