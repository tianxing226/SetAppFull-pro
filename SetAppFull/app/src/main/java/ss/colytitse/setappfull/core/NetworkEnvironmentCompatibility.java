package ss.colytitse.setappfull.core;

/** Narrow predicates for common, opt-in VPN and system-proxy checks. */
public final class NetworkEnvironmentCompatibility {
    public static final int TRANSPORT_VPN = 4;
    public static final int CAPABILITY_NOT_VPN = 15;
    public static final int TYPE_VPN = 17;

    private NetworkEnvironmentCompatibility() {}

    public static boolean isVpnTransport(int transport) {
        return transport == TRANSPORT_VPN;
    }

    public static boolean isNotVpnCapability(int capability) {
        return capability == CAPABILITY_NOT_VPN;
    }

    public static boolean isVpnNetworkType(int type) {
        return type == TYPE_VPN;
    }

}
