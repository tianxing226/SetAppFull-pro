package ss.colytitse.setappfull.hook;

import android.app.Activity;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import java.lang.ref.WeakReference;

import ss.colytitse.setappfull.core.RuleCodec;
import ss.colytitse.setappfull.core.WindowPolicy;

/** All access is on the target application's main thread. No strong Activity/Window references. */
final class WindowSession implements ViewTreeObserver.OnGlobalLayoutListener,
        ViewTreeObserver.OnWindowFocusChangeListener, View.OnAttachStateChangeListener {
    private final FullscreenModule module;
    private final WeakReference<Activity> activityReference;
    private final WeakReference<Window> windowReference;
    private final String packageName;
    private final boolean floating;
    private int requestedCutout;
    private int requestedVisibleBars;
    private int originalBarBehavior;
    private int controlledBarTypes;
    private int lastEffectiveRule = -1;
    private boolean baselineCaptured;
    private boolean cutoutControlled;
    private boolean behaviorControlled;
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

    void observeFlagsRequest(int flags, int mask) {
        if ((mask & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0) {
            observeBarRequest(WindowInsets.Type.statusBars(),
                    (flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) == 0);
        }
    }

    void apply(boolean force) {
        Activity activity = activityReference.get();
        Window window = windowReference.get();
        if (detached || activity == null || window == null || activity.isDestroyed()) return;
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
            int effective = WindowPolicy.effectiveRule(module.ruleFor(packageName),
                    activity.isInMultiWindowMode(), activity.isInPictureInPictureMode(), floating,
                    insets.isVisible(WindowInsets.Type.ime()));
            if (!force && effective == lastEffectiveRule) return;
            int previous = lastEffectiveRule;
            module.mutate(() -> applyPolicy(window, controller, effective));
            lastEffectiveRule = effective;
            if (previous != effective && (previous > 0 || effective > 0)) module.reportApplied(packageName, effective);
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
    }

    @Override public void onGlobalLayout() { apply(false); }
    @Override public void onWindowFocusChanged(boolean hasFocus) { if (hasFocus) apply(true); }
    @Override public void onViewAttachedToWindow(View view) { addTreeListeners(view); apply(true); }
    @Override public void onViewDetachedFromWindow(View view) {
        view.removeCallbacks(applyRunnable);
        applyQueued = false;
    }
}
