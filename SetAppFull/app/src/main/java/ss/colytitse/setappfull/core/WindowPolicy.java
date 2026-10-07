package ss.colytitse.setappfull.core;

/** Pure decision logic: special windows and the keyboard keep platform-controlled geometry. */
public final class WindowPolicy {
    private WindowPolicy() {}

    public static int effectiveRule(int configured, boolean multiWindow, boolean pictureInPicture,
                                    boolean floatingWindow, boolean keyboardVisible) {
        int flags = RuleCodec.normalize(configured);
        // Screenshot permission is deliberately independent from the fullscreen master switch.
        // Preserve it even when geometry control is suspended for dialogs, PiP or multi-window.
        int screenshot = flags & RuleCodec.ALLOW_SCREENSHOT;
        if (!RuleCodec.isEnabled(flags) || multiWindow || pictureInPicture || floatingWindow) return screenshot;
        return keyboardVisible ? (flags & ~RuleCodec.HIDE_NAVIGATION) : flags;
    }
}
