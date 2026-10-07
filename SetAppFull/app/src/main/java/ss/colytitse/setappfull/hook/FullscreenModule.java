package ss.colytitse.setappfull.hook;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.app.Instrumentation;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.View;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import io.github.libxposed.api.XposedModule;
import ss.colytitse.setappfull.BuildConfig;
import ss.colytitse.setappfull.core.RuleCodec;

/** API 101 baseline; API 102 runs the same public API without opting into hot reload. */
public final class FullscreenModule extends XposedModule {
    private static final String TAG = "SetAppFull";
    private static final String MODULE_PACKAGE = BuildConfig.APPLICATION_ID;
    private final Map<Window, WindowSession> windows = new WeakHashMap<>();
    private final Map<WindowInsetsController, WeakReference<WindowSession>> controllers = new WeakHashMap<>();
    private final Set<Method> observedMethods = new HashSet<>();
    private final ThreadLocal<Boolean> moduleMutation = ThreadLocal.withInitial(() -> false);
    private volatile Map<String, Integer> rules = Collections.emptyMap();
    private SharedPreferences remotePreferences;
    // SharedPreferences may keep weak listener references; the module must retain this one.
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceListener;
    private Handler main;
    private boolean systemServer;
    private boolean hooksInstalled;
    private int errorCount;

    @Override public void onModuleLoaded(ModuleLoadedParam param) {
        systemServer = param.isSystemServer();
        if (systemServer || Build.VERSION.SDK_INT < 30) return;
        Looper mainLooper = Looper.getMainLooper();
        if (mainLooper != null) main = new Handler(mainLooper);
        if ((getFrameworkProperties() & PROP_CAP_REMOTE) == 0) {
            log(Log.WARN, TAG, "Remote preferences unavailable; no fullscreen rules will be applied.");
            return;
        }
        try {
            remotePreferences = getRemotePreferences(RuleCodec.PREF_GROUP);
            preferenceListener = (prefs, key) -> {
                if (key == null || key.startsWith(RuleCodec.KEY_PREFIX)) refreshRules();
            };
            // Register before the first snapshot so a concurrent write is not missed.
            remotePreferences.registerOnSharedPreferenceChangeListener(preferenceListener);
            refreshRules();
            log(Log.INFO, TAG, "Module loaded: process=" + param.getProcessName()
                    + ", Android API=" + Build.VERSION.SDK_INT + ", Xposed API=" + getApiVersion());
        } catch (RuntimeException failure) {
            reportFailure("Remote preferences initialization failed; policy remains disabled", failure);
        }
    }

    @Override public void onPackageReady(PackageReadyParam param) {
        if (main == null && Looper.getMainLooper() != null) main = new Handler(Looper.getMainLooper());
        if (systemServer || main == null || remotePreferences == null || hooksInstalled
                || !param.isFirstPackage() || "android".equals(param.getPackageName())
                || MODULE_PACKAGE.equals(param.getPackageName())) return;
        hooksInstalled = true;
        installHook(Instrumentation.class, "callActivityOnResume", new Class<?>[]{Activity.class}, chain -> {
            Object result = chain.proceed();
            Activity activity = (Activity) chain.getArg(0);
            onMain(() -> resume(activity));
            return result;
        });
        installHook(Instrumentation.class, "callActivityOnDestroy", new Class<?>[]{Activity.class}, chain -> {
            Activity activity = (Activity) chain.getArg(0);
            onMain(() -> destroy(activity));
            return chain.proceed();
        });
        installHook(Window.class, "setAttributes", new Class<?>[]{WindowManager.LayoutParams.class}, chain -> {
            Object result = chain.proceed();
            if (!moduleMutation.get() && Looper.myLooper() == Looper.getMainLooper()) {
                WindowSession session = sessionFor((Window) chain.getThisObject());
                if (session != null) {
                    WindowManager.LayoutParams params = (WindowManager.LayoutParams) chain.getArg(0);
                    session.observeCutoutRequest(params.layoutInDisplayCutoutMode);
                    session.observeAttributesFullscreenRequest(
                            (params.flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0);
                    session.observeAttributesSecureRequest(
                            (params.flags & WindowManager.LayoutParams.FLAG_SECURE) != 0);
                    session.scheduleApply();
                }
            }
            return result;
        });
        installHook(Window.class, "setDecorFitsSystemWindows", new Class<?>[]{boolean.class}, chain -> {
            Object result = chain.proceed();
            if (!moduleMutation.get() && Looper.myLooper() == Looper.getMainLooper()) {
                WindowSession session = sessionFor((Window) chain.getThisObject());
                if (session != null) session.scheduleApply();
            }
            return result;
        });
        installHook(Window.class, "setFlags", new Class<?>[]{int.class, int.class}, chain -> {
            Object result = chain.proceed();
            if (!moduleMutation.get() && Looper.myLooper() == Looper.getMainLooper()) {
                WindowSession session = sessionFor((Window) chain.getThisObject());
                if (session != null) {
                    session.observeFlagsRequest((Integer) chain.getArg(0), (Integer) chain.getArg(1));
                    session.scheduleApply();
                }
            }
            return result;
        });
        installHook(Window.class, "addFlags", new Class<?>[]{int.class}, chain -> {
            Object result = chain.proceed();
            if (!moduleMutation.get() && Looper.myLooper() == Looper.getMainLooper()) {
                WindowSession session = sessionFor((Window) chain.getThisObject());
                if (session != null && (((Integer) chain.getArg(0)) & WindowManager.LayoutParams.FLAG_SECURE) != 0) {
                    session.observeSecureRequest(true);
                    session.scheduleApply();
                }
            }
            return result;
        });
        installHook(Window.class, "clearFlags", new Class<?>[]{int.class}, chain -> {
            Object result = chain.proceed();
            if (!moduleMutation.get() && Looper.myLooper() == Looper.getMainLooper()) {
                WindowSession session = sessionFor((Window) chain.getThisObject());
                if (session != null && (((Integer) chain.getArg(0)) & WindowManager.LayoutParams.FLAG_SECURE) != 0) {
                    session.observeSecureRequest(false);
                    session.scheduleApply();
                }
            }
            return result;
        });
        installHook(View.class, "setSystemUiVisibility", new Class<?>[]{int.class}, chain -> {
            Object result = chain.proceed();
            if (!moduleMutation.get() && Looper.myLooper() == Looper.getMainLooper()) {
                // Targets often reset decor visibility after resume or after a WebView relayout.
                // Reapply every tracked window in this process so the configured bar policy wins
                // without suppressing the target's original call.
                onMain(() -> {
                    for (WindowSession session : new ArrayList<>(windows.values())) session.apply(true);
                });
            }
            return result;
        });
        installHook(Activity.class, "onWindowFocusChanged", new Class<?>[]{boolean.class}, chain -> {
            Object result = chain.proceed();
            if (!moduleMutation.get() && Boolean.TRUE.equals(chain.getArg(0))) {
                Activity activity = (Activity) chain.getThisObject();
                onMain(() -> resume(activity));
            }
            return result;
        });
        installHook(Dialog.class, "show", new Class<?>[]{}, chain -> {
            Object result = chain.proceed();
            Dialog dialog = (Dialog) chain.getThisObject();
            if (!moduleMutation.get() && Looper.myLooper() == Looper.getMainLooper()) {
                Window window = dialog.getWindow();
                if (window != null && dialog.getContext() != null) {
                    trackWindow(window, dialog.getContext().getPackageName(), true);
                }
            }
            return result;
        });
        installHook(Dialog.class, "dismiss", new Class<?>[]{}, chain -> {
            Dialog dialog = (Dialog) chain.getThisObject();
            Window window = dialog.getWindow();
            Object result = chain.proceed();
            if (window != null) destroyWindow(window);
            return result;
        });
    }

    private void refreshRules() {
        try {
            // IPC/disk access is confined to initialization and change notifications, never draw hooks.
            rules = RuleCodec.decode(remotePreferences.getAll());
        } catch (RuntimeException failure) {
            rules = Collections.emptyMap();
            reportFailure("Configuration refresh failed; restoring controlled windows", failure);
        }
        onMain(() -> {
            for (WindowSession session : new ArrayList<>(windows.values())) session.apply(true);
        });
    }

    private void resume(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()
                || MODULE_PACKAGE.equals(activity.getPackageName())) return;
        Window window = activity.getWindow();
        if (window == null) return;
        WindowSession session = windows.get(window);
        if (session == null) {
            session = new WindowSession(this, activity);
            windows.put(window, session);
            session.attach();
        }
        session.apply(true);
    }

    private void destroy(Activity activity) {
        WindowSession session = windows.remove(activity.getWindow());
        if (session != null) session.detach();
    }

    private void trackWindow(Window window, String packageName, boolean floating) {
        if (window == null || packageName == null || MODULE_PACKAGE.equals(packageName)
                || "android".equals(packageName)) return;
        WindowSession session = windows.get(window);
        if (session == null) {
            session = new WindowSession(this, window, packageName, floating);
            windows.put(window, session);
            session.attach();
        }
        session.apply(true);
    }

    /** Dialog and PopupWindow use a Window without an Activity callback. Track it lazily when a
     * target itself changes flags, so secure-window handling remains package scoped. */
    private WindowSession sessionFor(Window window) {
        WindowSession session = windows.get(window);
        if (session != null || window == null) return session;
        try {
            Context context = window.getContext();
            if (context != null) trackWindow(window, context.getPackageName(), true);
        } catch (RuntimeException failure) {
            reportFailure("Could not inspect secondary window", failure);
        }
        return windows.get(window);
    }

    private void destroyWindow(Window window) {
        WindowSession session = windows.remove(window);
        if (session != null) session.detach();
    }

    int ruleFor(String packageName) {
        return rules.getOrDefault(packageName, 0);
    }

    void mutate(Runnable action) {
        boolean previous = moduleMutation.get();
        moduleMutation.set(true);
        try { action.run(); }
        finally { moduleMutation.set(previous); }
    }

    void observeController(WindowInsetsController controller, WindowSession session) {
        controllers.put(controller, new WeakReference<>(session));
        observeControllerMethod(controller.getClass(), "show");
        observeControllerMethod(controller.getClass(), "hide");
        observeControllerMethod(controller.getClass(), "setSystemBarsBehavior");
    }

    private void observeControllerMethod(Class<?> type, String name) {
        try {
            Method method = type.getMethod(name, int.class);
            if (Modifier.isAbstract(method.getModifiers()) || !observedMethods.add(method)) return;
            hook(method).intercept(chain -> {
                Object result = chain.proceed();
                if (!moduleMutation.get() && Looper.myLooper() == Looper.getMainLooper()) {
                    WeakReference<WindowSession> reference = controllers.get(chain.getThisObject());
                    WindowSession session = reference == null ? null : reference.get();
                    if (session != null) {
                        if (name.equals("setSystemBarsBehavior")) session.observeBehaviorRequest((Integer) chain.getArg(0));
                        else session.observeBarRequest((Integer) chain.getArg(0), name.equals("show"));
                        // The target may immediately show a bar after our hide call. Queue a
                        // correction even when the configured rule itself has not changed.
                        session.scheduleApply();
                    }
                }
                // Observation only: do not suppress system gestures or an app's IME requests.
                return result;
            });
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            reportFailure("Cannot observe " + type.getName() + "." + name, failure);
        }
    }

    private void installHook(Class<?> owner, String name, Class<?>[] parameters, Hooker hooker) {
        try {
            hook(owner.getDeclaredMethod(name, parameters)).intercept(hooker);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            reportFailure("Cannot register " + owner.getName() + "." + name, failure);
        }
    }

    private void onMain(Runnable action) {
        // Some embedded frameworks may initialize modules before the main Looper. There are no
        // tracked windows yet; onPackageReady creates the handler and resume uses the cached rules.
        if (main == null) return;
        Runnable protectedAction = () -> {
            try { action.run(); }
            catch (RuntimeException | LinkageError failure) { reportFailure("Window event failed", failure); }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) protectedAction.run();
        else main.post(protectedAction);
    }

    void reportFailure(String message, Throwable failure) {
        // Bound repeated vendor-specific failures instead of filling target-app logs.
        if (errorCount++ < 8) log(Log.WARN, TAG, message, failure);
    }

    void reportApplied(String packageName, int effectiveRule) {
        log(Log.INFO, TAG, "Window policy: package=" + packageName + ", rule=" + effectiveRule);
    }
}
