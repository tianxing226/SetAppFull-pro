package ss.colytitse.setappfull.hook;

import android.app.Activity;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.ViewGroup;
import android.webkit.WebView;

import java.lang.ref.WeakReference;

import ss.colytitse.setappfull.core.RuleCodec;
import ss.colytitse.setappfull.core.WindowPolicy;
import ss.colytitse.setappfull.core.WindowCompatibilityPolicy;

/** All access is on the target application's main thread. No strong Activity/Window references. */
final class WindowSession implements ViewTreeObserver.OnGlobalLayoutListener,
        ViewTreeObserver.OnWindowFocusChangeListener, View.OnAttachStateChangeListener {
    private final FullscreenModule module;
    private WeakReference<Activity> activityReference;
    private final WeakReference<Window> windowReference;
    private String packageName;
    private boolean floating;
    private int requestedCutout;
    private int requestedVisibleBars;
    private int originalBarBehavior;
    private int controlledBarTypes;
    private int lastEffectiveRule = -1;
    private boolean baselineCaptured;
    private boolean cutoutControlled;
    private boolean behaviorControlled;
    private boolean secureBaselineCaptured;
    private boolean originalSecure;
    private boolean screenshotControlled;
    private boolean originalFullscreenFlag;
    private boolean requestedFullscreenFlag;
    private boolean fullscreenFlagControlled;
    private boolean systemUiBaselineCaptured;
    private int controlledLegacySystemUi;
    private boolean reliefBaselineCaptured;
    private int originalStatusBarColor;
    private int originalNavigationBarColor;
    private int originalSystemUiVisibility;
    private boolean reliefEdgeToEdge;
    private boolean scopedEdgeBaselineCaptured;
    private int scopedOriginalStatusBarColor;
    private int scopedOriginalNavigationBarColor;
    private int scopedOriginalSystemUiVisibility;
    private boolean scopedEdgeToEdge;
    // Window decor fitting defaults to true; explicit target requests are captured by the
    // public setDecorFitsSystemWindows hook.
    private boolean requestedDecorFitsSystemWindows = true;
    private boolean detached;
    private boolean applyQueued;
    private final Runnable applyRunnable = () -> {
        applyQueued = false;
        apply(true);
    };

    WindowSession(FullscreenModule module, Activity activity) {
        this.module = module;
        this.activityReference = new WeakReference<>(activity);
        Window window = activity.getWindow();
        this.windowReference = new WeakReference<>(window);
        this.packageName = activity.getPackageName();
        this.requestedCutout = window.getAttributes().layoutInDisplayCutoutMode;
        TypedValue value = new TypedValue();
        floating = activity.getTheme().resolveAttribute(android.R.attr.windowIsFloating, value, true)
                && value.data != 0;
    }

    WindowSession(FullscreenModule module, Window window, String packageName, boolean floating) {
        this.module = module;
        this.activityReference = new WeakReference<>(null);
        this.windowReference = new WeakReference<>(window);
        this.packageName = packageName;
        this.requestedCutout = window.getAttributes().layoutInDisplayCutoutMode;
        this.floating = floating;
    }

    /** Promote a lazily tracked Activity window without losing its existing restore baselines. */
    void bindActivity(Activity activity) {
        Window window = windowReference.get();
        if (activity == null || window == null || activity.getWindow() != window) return;
        activityReference = new WeakReference<>(activity);
        packageName = activity.getPackageName();
        TypedValue value = new TypedValue();
        floating = activity.getTheme().resolveAttribute(android.R.attr.windowIsFloating, value, true)
                && value.data != 0;
    }

    void attach() {
        Window window = windowReference.get();
        if (window == null) return;
        View decor = window.getDecorView();
        decor.addOnAttachStateChangeListener(this);
        addTreeListeners(decor);
    }

    private void addTreeListeners(View decor) {
        ViewTreeObserver observer = decor.getViewTreeObserver();
        if (observer.isAlive()) {
            // Remove first: an unattached ViewTreeObserver may be merged during attachment.
            observer.removeOnGlobalLayoutListener(this);
            observer.removeOnWindowFocusChangeListener(this);
            observer.addOnGlobalLayoutListener(this);
            observer.addOnWindowFocusChangeListener(this);
        }
    }

    void detach() {
        detached = true;
        Window window = windowReference.get();
        if (window == null) return;
        View decor = window.peekDecorView();
        if (decor == null) return;
        decor.removeCallbacks(applyRunnable);
        decor.removeOnAttachStateChangeListener(this);
        ViewTreeObserver observer = decor.getViewTreeObserver();
        if (observer.isAlive()) {
            observer.removeOnGlobalLayoutListener(this);
            observer.removeOnWindowFocusChangeListener(this);
        }
    }

    void scheduleApply() {
        Window window = windowReference.get();
        if (detached || applyQueued || window == null) return;
        View decor = window.peekDecorView();
        if (decor != null) {
            applyQueued = true;
            decor.post(applyRunnable);
        }
    }

    boolean ownsView(View view) {
        Window window = windowReference.get();
        if (window == null || view == null) return false;
        View decor = window.peekDecorView();
        if (decor == null) return false;
        return view == decor || view.getRootView() == decor;
    }

    int enforceSystemUiVisibility(int requested) {
        // Intercept the target's own decor reset so the hide takes effect in the same call. The
        // follow-up scheduled apply also restores Relief Map's layout flags and controller state.
        return controlledLegacySystemUi == 0 ? requested : requested | controlledLegacySystemUi;
    }

    void observeCutoutRequest(int requested) {
        // A caller may mutate getAttributes() in place, retaining our own cutout value.
        if (!cutoutControlled || requested != WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS) {
            requestedCutout = requested;
        }
    }

    void observeBarRequest(int types, boolean visible) {
        int bars = types & (WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
        if (visible) requestedVisibleBars |= bars;
        else requestedVisibleBars &= ~bars;
    }

    void observeBehaviorRequest(int behavior) {
        originalBarBehavior = behavior;
    }

    void observeDecorFitsRequest(boolean decorFits) {
        requestedDecorFitsSystemWindows = decorFits;
    }

    void observeSystemUiVisibilityRequest(int requested) {
        if (!scopedEdgeToEdge) return;
        int layoutFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        scopedOriginalSystemUiVisibility = (scopedOriginalSystemUiVisibility & ~layoutFlags)
                | (requested & layoutFlags);
    }

    void observeStatusBarColorRequest(int color) {
        if (scopedEdgeToEdge) scopedOriginalStatusBarColor = color;
    }

    void observeNavigationBarColorRequest(int color) {
        if (scopedEdgeToEdge) scopedOriginalNavigationBarColor = color;
    }

    void observeFlagsRequest(int flags, int mask) {
        if ((mask & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0) {
            requestedFullscreenFlag = (flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0;
            observeBarRequest(WindowInsets.Type.statusBars(),
                    (flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) == 0);
        }
        if ((mask & WindowManager.LayoutParams.FLAG_SECURE) != 0) {
            observeSecureRequest((flags & WindowManager.LayoutParams.FLAG_SECURE) != 0);
        }
    }

    void observeAttributesFullscreenRequest(boolean fullscreen) {
        // LayoutParams frequently mirrors the FLAG_FULLSCREEN value that this module just
        // applied. Do not let that mirror make the disabled state fullscreen forever; explicit
        // setFlags/addFlags/clearFlags calls remain authoritative through observeFlagsRequest.
        if (!fullscreenFlagControlled) requestedFullscreenFlag = fullscreen;
    }

    void observeSecureRequest(boolean secure) {
        if (!secureBaselineCaptured) {
            originalSecure = secure;
            secureBaselineCaptured = true;
        } else if (!screenshotControlled) {
            // When the feature is off, the target application's latest request is authoritative.
            originalSecure = secure;
        } else {
            // Remember requests made while the feature is on so disabling restores them.
            originalSecure = secure;
        }
    }

    /** LayoutParams often mirror the flag after our own clearFlags call. Do not treat that mirror
     * as the target explicitly removing its original secure request; explicit clearFlags remains
     * observable through the dedicated Window hook. */
    void observeAttributesSecureRequest(boolean secure) {
        if (!screenshotControlled || secure) observeSecureRequest(secure);
    }

    void apply(boolean force) {
        Activity activity = activityReference.get();
        Window window = windowReference.get();
        if (detached || window == null || (activity != null && activity.isDestroyed())) return;
        try {
            View decor = window.peekDecorView();
            if (decor == null) return;
            WindowInsets insets = decor.getRootWindowInsets();
            WindowInsetsController controller = window.getInsetsController();
            // Wait for real insets rather than guessing visibility before the first layout.
            if (insets == null || controller == null) return;
            if (!baselineCaptured) {
                requestedVisibleBars = 0;
                if (insets.isVisible(WindowInsets.Type.statusBars())) requestedVisibleBars |= WindowInsets.Type.statusBars();
                if (insets.isVisible(WindowInsets.Type.navigationBars())) requestedVisibleBars |= WindowInsets.Type.navigationBars();
                originalBarBehavior = controller.getSystemBarsBehavior();
                baselineCaptured = true;
                module.observeController(controller, this);
            }
            if (!secureBaselineCaptured) {
                originalSecure = (window.getAttributes().flags & WindowManager.LayoutParams.FLAG_SECURE) != 0;
                secureBaselineCaptured = true;
            }
            if (!systemUiBaselineCaptured) {
                originalSystemUiVisibility = decor.getSystemUiVisibility();
                originalFullscreenFlag = (window.getAttributes().flags
                        & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0;
                requestedFullscreenFlag = originalFullscreenFlag;
                systemUiBaselineCaptured = true;
            }
            int configured = module.ruleFor(packageName);
            int effective = WindowPolicy.effectiveRule(configured,
                    activity != null && activity.isInMultiWindowMode(),
                    activity != null && activity.isInPictureInPictureMode(), floating,
                    insets.isVisible(WindowInsets.Type.ime()));
            if (!force && effective == lastEffectiveRule && !barsNeedCorrection(window, decor, insets, effective)) return;
            int previous = lastEffectiveRule;
            module.mutate(() -> applyPolicy(window, controller, effective));
            lastEffectiveRule = effective;
            if (previous != effective && (configured > 0 || previous > 0 || effective > 0)) {
                module.reportApplied(packageName, configured, effective, activity != null, floating);
            }
        } catch (RuntimeException | LinkageError failure) {
            module.reportFailure("Could not apply window policy for " + packageName, failure);
        }
    }

    private void applyPolicy(Window window, WindowInsetsController controller, int effective) {
        int hideTypes = 0;
        if ((effective & RuleCodec.HIDE_STATUS) != 0) hideTypes |= WindowInsets.Type.statusBars();
        if ((effective & RuleCodec.HIDE_NAVIGATION) != 0) hideTypes |= WindowInsets.Type.navigationBars();

        int released = controlledBarTypes & ~hideTypes;
        if (released != 0) {
            int show = released & requestedVisibleBars;
            int hide = released & ~requestedVisibleBars;
            if (show != 0) controller.show(show);
            if (hide != 0) controller.hide(hide);
        }
        if (hideTypes != 0) {
            if (!behaviorControlled) originalBarBehavior = controller.getSystemBarsBehavior();
            controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            behaviorControlled = true;
            controller.hide(hideTypes);
        } else if (behaviorControlled) {
            controller.setSystemBarsBehavior(originalBarBehavior);
            behaviorControlled = false;
        }
        controlledBarTypes = hideTypes;

        // Keep the legacy path in sync with WindowInsetsController. A number of real-world
        // applications (and some Android 15/16 compatibility layers) reapply decor visibility
        // after the modern controller call, which otherwise makes the status bar visible again.
        applyLegacySystemUi(window, effective);

        boolean allowScreenshot = (effective & RuleCodec.ALLOW_SCREENSHOT) != 0;
        int currentFlags = window.getAttributes().flags;
        boolean secure = (currentFlags & WindowManager.LayoutParams.FLAG_SECURE) != 0;
        if (allowScreenshot) {
            if (secure) window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            screenshotControlled = true;
        } else if (screenshotControlled || (secureBaselineCaptured && secure != originalSecure)) {
            if (originalSecure) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            screenshotControlled = false;
        }

        boolean allowCutout = (effective & RuleCodec.ALLOW_CUTOUT) != 0;
        if (allowCutout || cutoutControlled) {
            int desired = allowCutout ? WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS : requestedCutout;
            WindowManager.LayoutParams current = window.getAttributes();
            if (current.layoutInDisplayCutoutMode != desired) {
                // Copy all attributes and change exactly one field. Flags, dimensions and IME mode survive.
                WindowManager.LayoutParams updated = new WindowManager.LayoutParams();
                updated.copyFrom(current);
                updated.layoutInDisplayCutoutMode = desired;
                window.setAttributes(updated);
            }
        }
        cutoutControlled = allowCutout;

        applyReliefCompatibility(window, effective);
        applyScopedEdgeToEdgeCompatibility(window, effective);
        // The Relief compatibility path updates legacy decor flags after the modern controller;
        // issue one final hide so Android's insets state and the legacy state converge.
        if (hideTypes != 0) controller.hide(hideTypes);
    }

    private boolean barsNeedCorrection(Window window, View decor, WindowInsets insets, int effective) {
        int hideTypes = 0;
        if ((effective & RuleCodec.HIDE_STATUS) != 0) hideTypes |= WindowInsets.Type.statusBars();
        if ((effective & RuleCodec.HIDE_NAVIGATION) != 0) hideTypes |= WindowInsets.Type.navigationBars();
        if (hideTypes == 0) return false;
        if ((hideTypes & WindowInsets.Type.statusBars()) != 0
                && (insets.isVisible(WindowInsets.Type.statusBars())
                || (android.os.Build.VERSION.SDK_INT < 35
                && ((window.getAttributes().flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) == 0
                || (decor.getSystemUiVisibility() & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0)))) return true;
        return (hideTypes & WindowInsets.Type.navigationBars()) != 0
                && (insets.isVisible(WindowInsets.Type.navigationBars())
                || (android.os.Build.VERSION.SDK_INT < 35
                && (decor.getSystemUiVisibility() & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0));
    }

    private void applyLegacySystemUi(Window window, int effective) {
        View decor = window.getDecorView();
        int desiredControlled = 0;
        if ((effective & RuleCodec.HIDE_STATUS) != 0) {
            desiredControlled |= View.SYSTEM_UI_FLAG_FULLSCREEN;
            if ((window.getAttributes().flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) == 0) {
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
            fullscreenFlagControlled = true;
        } else if (fullscreenFlagControlled) {
            if (requestedFullscreenFlag) window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            else window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            fullscreenFlagControlled = false;
        }
        if ((effective & RuleCodec.HIDE_NAVIGATION) != 0) {
            desiredControlled |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }

        int current = decor.getSystemUiVisibility();
        int released = controlledLegacySystemUi & ~desiredControlled;
        if (released != 0) current = (current & ~released) | (originalSystemUiVisibility & released);
        int updated = current | desiredControlled;
        if (updated != current) decor.setSystemUiVisibility(updated);
        controlledLegacySystemUi = desiredControlled;
    }

    /** Relief Map is a legacy WebView wrapper whose loaded page is inset by the visible status bar. */
    private void applyReliefCompatibility(Window window, int effective) {
        int configured = module.ruleFor(packageName);
        boolean enabled = RuleCodec.isEnabled(configured)
                && (configured & (RuleCodec.HIDE_STATUS | RuleCodec.HIDE_NAVIGATION | RuleCodec.ALLOW_CUTOUT)) != 0
                && "app.mapforfree".equals(packageName);
        if (!"app.mapforfree".equals(packageName)) return;
        if (!reliefBaselineCaptured) {
            originalStatusBarColor = window.getStatusBarColor();
            originalNavigationBarColor = window.getNavigationBarColor();
            View decor = window.getDecorView();
            originalSystemUiVisibility = decor.getSystemUiVisibility();
            reliefBaselineCaptured = true;
        }
        View decor = window.getDecorView();
        if (enabled) {
            if (!reliefEdgeToEdge) {
                window.setStatusBarColor(Color.TRANSPARENT);
                window.setNavigationBarColor(Color.TRANSPARENT);
                if (android.os.Build.VERSION.SDK_INT >= 30) window.setDecorFitsSystemWindows(false);
                reliefEdgeToEdge = true;
            }
            int desired = originalSystemUiVisibility
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            if ((effective & RuleCodec.HIDE_STATUS) != 0) desired |= View.SYSTEM_UI_FLAG_FULLSCREEN;
            if ((effective & RuleCodec.HIDE_NAVIGATION) != 0) {
                desired |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            }
            if (decor.getSystemUiVisibility() != desired) decor.setSystemUiVisibility(desired);
        } else if (!enabled && reliefEdgeToEdge) {
            window.setStatusBarColor(originalStatusBarColor);
            window.setNavigationBarColor(originalNavigationBarColor);
            if (android.os.Build.VERSION.SDK_INT >= 30) window.setDecorFitsSystemWindows(true);
            decor.setSystemUiVisibility(originalSystemUiVisibility);
            reliefEdgeToEdge = false;
        }
    }

    /**
     * Some legacy WebView wrappers and the Bilibili player activity hide the bars but still fit
     * their content below the cutout inset. Keep this correction limited to recognizable windows;
     * ordinary WebView apps and Bilibili's home activity retain their own layout policy.
     */
    private void applyScopedEdgeToEdgeCompatibility(Window window, int effective) {
        Activity activity = activityReference.get();
        if (activity == null) return;
        String activityName = activity.getClass().getName();
        boolean fusionWebApp = WindowCompatibilityPolicy.isFusionWebActivity(packageName, activityName)
                && containsWebView(window.getDecorView());
        boolean bilibiliPlayer = WindowCompatibilityPolicy.isBilibiliPlayer(packageName, activityName);
        int configured = module.ruleFor(packageName);
        boolean enabled = RuleCodec.isEnabled(configured)
                && (effective & (RuleCodec.HIDE_STATUS | RuleCodec.HIDE_NAVIGATION | RuleCodec.ALLOW_CUTOUT)) != 0;
        WindowInsets rootInsets = window.getDecorView().getRootWindowInsets();
        boolean hasTopInset = bilibiliPlayer
                && (scopedEdgeToEdge || contentStartsBelowStatusInset(window, rootInsets));
        boolean matched = (fusionWebApp || (bilibiliPlayer && hasTopInset)) && enabled;
        View decor = window.getDecorView();
        if (matched && !scopedEdgeToEdge) {
            scopedOriginalStatusBarColor = window.getStatusBarColor();
            scopedOriginalNavigationBarColor = window.getNavigationBarColor();
            scopedOriginalSystemUiVisibility = decor.getSystemUiVisibility();
            scopedEdgeBaselineCaptured = true;
        }
        if (matched) {
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
            if (requestedDecorFitsSystemWindows) window.setDecorFitsSystemWindows(false);
            int desired = decor.getSystemUiVisibility()
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            if ((effective & RuleCodec.HIDE_STATUS) != 0) desired |= View.SYSTEM_UI_FLAG_FULLSCREEN;
            if ((effective & RuleCodec.HIDE_NAVIGATION) != 0) {
                desired |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            }
            if (decor.getSystemUiVisibility() != desired) decor.setSystemUiVisibility(desired);
            scopedEdgeToEdge = true;
        } else if (scopedEdgeToEdge && scopedEdgeBaselineCaptured) {
            window.setStatusBarColor(scopedOriginalStatusBarColor);
            window.setNavigationBarColor(scopedOriginalNavigationBarColor);
            window.setDecorFitsSystemWindows(requestedDecorFitsSystemWindows);
            int layoutFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            int current = decor.getSystemUiVisibility();
            int restored = (current & ~layoutFlags) | (scopedOriginalSystemUiVisibility & layoutFlags);
            if (restored != current) decor.setSystemUiVisibility(restored);
            scopedEdgeToEdge = false;
            scopedEdgeBaselineCaptured = false;
        }
    }

    private static boolean contentStartsBelowStatusInset(Window window, WindowInsets insets) {
        if (insets == null) return false;
        int topInset = insets.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).top;
        View content = window.findViewById(android.R.id.content);
        if (content == null || topInset <= 0) return false;
        int[] location = new int[2];
        content.getLocationOnScreen(location);
        return location[1] >= topInset;
    }

    private static boolean containsWebView(View view) {
        if (view instanceof WebView) return true;
        if (!(view instanceof ViewGroup)) return false;
        ViewGroup group = (ViewGroup) view;
        for (int index = 0; index < group.getChildCount(); index++) {
            if (containsWebView(group.getChildAt(index))) return true;
        }
        return false;
    }

    @Override public void onGlobalLayout() { apply(false); }
    @Override public void onWindowFocusChanged(boolean hasFocus) { if (hasFocus) apply(true); }
    @Override public void onViewAttachedToWindow(View view) { addTreeListeners(view); apply(true); }
    @Override public void onViewDetachedFromWindow(View view) {
        view.removeCallbacks(applyRunnable);
        applyQueued = false;
    }
}
