package ss.colytitse.setappfull.core;

/** Pure decision logic: special windows and the keyboard keep platform-controlled geometry. */
public final class WindowPolicy {
    private WindowPolicy() {}

    public static int effectiveRule(int configured, boolean multiWindow, boolean pictureInPicture,
                                    boolean floatingWindow, boolean keyboardVisible) {
        int flags = RuleCodec.normalize(configured);
        if (!RuleCodec.isEnabled(flags) || multiWindow || pictureInPicture || floatingWindow) return 0;
        return keyboardVisible ? flags & ~RuleCodec.HIDE_NAVIGATION : flags;
    }
}
