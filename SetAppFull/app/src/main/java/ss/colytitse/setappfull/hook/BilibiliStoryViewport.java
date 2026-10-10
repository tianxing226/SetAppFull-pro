package ss.colytitse.setappfull.hook;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Looper;
import android.view.View;
import android.view.WindowInsets;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import ss.colytitse.setappfull.core.RuleCodec;

/**
 * Bilibili 9.13's StoryPlayer reserves a status/cutout-height band INSIDE its renderer viewport.
 * Correct that one input, retaining the player's aspect ratio, crop policy and vertical offset.
 * No resource, global insets or Surface dimensions are spoofed.
 */
final class BilibiliStoryViewport {
    static final String PACKAGE = "tv.danmaku.bili";
    private static final String ACTIVITY = "com.bilibili.video.story.StoryVideoActivity";
    private final FullscreenModule module;
    private final Field contextField;
    private final Method setter;
    private final Method scene;
    private final Map<Object, Request> requests = new WeakHashMap<>();
    private boolean replay;
    private int reported;
    private int inspected;
    private Boolean supportedVersion;

    private static final class Request {
        final int offset;
        final int padding;
        int applied;
        Request(int offset, int padding) { this.offset = offset; this.padding = padding; this.applied = padding; }
    }

    BilibiliStoryViewport(FullscreenModule module, ClassLoader loader) throws ReflectiveOperationException {
        this.module = module;
        Class<?> player = Class.forName("com.bilibili.video.story.player.StoryPlayer", false, loader);
        contextField = player.getDeclaredField("F");
        if (!Context.class.isAssignableFrom(contextField.getType())) throw new NoSuchFieldException("Context F");
        setter = player.getDeclaredMethod("Y0", int.class, int.class);
        scene = player.getDeclaredMethod("p");
        if (!scene.getReturnType().isEnum() ||
                !"tv.danmaku.biliplayerv2.ControlContainerType".equals(scene.getReturnType().getName())) {
            throw new NoSuchMethodException("Player scene type");
        }
        if (setter.getReturnType() != void.class) throw new NoSuchMethodException("Y0 return type");
        contextField.setAccessible(true);
        setter.setAccessible(true);
        scene.setAccessible(true);
        module.hook(setter).intercept(chain -> {
            if (replay || Looper.myLooper() != Looper.getMainLooper()) return chain.proceed();
            Object playerObject = chain.getThisObject();
            int offset = (Integer) chain.getArg(0);
            int padding = (Integer) chain.getArg(1);
            Request request = new Request(offset, padding);
            requests.put(playerObject, request);
            request.applied = correctedPadding(playerObject, padding);
            if (inspected++ < 3) module.reportBilibiliPadding(padding, request.applied);
            if (request.applied != padding && reported++ < 4) {
                module.reportBilibiliPadding(padding, request.applied);
            }
            return request.applied == padding ? chain.proceed()
                    : chain.proceed(new Object[]{offset, request.applied});
        });
        module.reportBilibiliAdapter();
    }

    private int correctedPadding(Object player, int requested) {
        int flags = module.ruleFor(PACKAGE);
        int required = RuleCodec.ENABLED | RuleCodec.HIDE_STATUS | RuleCodec.ALLOW_CUTOUT;
        if ((flags & required) != required || (flags & RuleCodec.DISABLE_BILIBILI_OPTIMIZATION) != 0
                || requested <= 0) return requested;
        try {
            Activity activity = activity((Context) contextField.get(player));
            if (activity == null || !ACTIVITY.equals(activity.getClass().getName()) ||
                    !PACKAGE.equals(activity.getPackageName()) || activity.isDestroyed()
                    || activity.isInMultiWindowMode() || activity.isInPictureInPictureMode()) return requested;
            if (supportedVersion == null) {
                long version = activity.getPackageManager().getPackageInfo(PACKAGE, 0).getLongVersionCode();
                // Internal method names are version-specific: unknown builds retain stock behavior.
                supportedVersion = version == 9130500L || version == 9140400L;
            }
            if (!supportedVersion) return requested;
            Object state = scene.invoke(player);
            if (!(state instanceof Enum<?>) || !"VERTICAL_FULLSCREEN".equals(((Enum<?>) state).name())) return requested;
            View decor = activity.getWindow().peekDecorView();
            if (decor == null || decor.getHeight() <= decor.getWidth()) return requested;
            WindowInsets insets = decor.getRootWindowInsets();
            if (insets == null || insets.isVisible(WindowInsets.Type.ime())) return requested;
            int reserved = insets.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).top;
            if (insets.getDisplayCutout() != null) reserved = Math.max(reserved, insets.getDisplayCutout().getSafeInsetTop());
            return reserved > 0 && Math.abs(requested - reserved) <= 1 ? 0 : requested;
        } catch (ReflectiveOperationException | android.content.pm.PackageManager.NameNotFoundException
                | RuntimeException failure) {
            return requested;
        }
    }

    /** Called on the main thread after rule/insets/lifecycle changes; off restores original input. */
    void refresh() {
        if (replay || Looper.myLooper() != Looper.getMainLooper()) return;
        for (Map.Entry<Object, Request> entry : new ArrayList<>(requests.entrySet())) {
            Object player = entry.getKey();
            Request request = entry.getValue();
            if (player == null) continue;
            int desired = correctedPadding(player, request.padding);
            if (desired == request.applied) continue;
            try {
                replay = true;
                setter.invoke(player, request.offset, desired);
                request.applied = desired;
                if (reported++ < 4) module.reportBilibiliPadding(request.padding, desired);
            } catch (ReflectiveOperationException | RuntimeException failure) {
                module.reportFailure("Bilibili viewport replay unavailable; restart target to restore", failure);
            } finally {
                replay = false;
            }
        }
    }

    private static Activity activity(Context context) {
        Set<Context> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        while (context != null && visited.add(context)) {
            if (context instanceof Activity) return (Activity) context;
            if (!(context instanceof ContextWrapper)) return null;
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }
}
