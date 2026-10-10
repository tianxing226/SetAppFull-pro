package ss.colytitse.setappfull.core;

/** Narrow activity matchers for window compatibility paths that must not affect other apps. */
public final class WindowCompatibilityPolicy {
    private static final String FUSION_ACTIVITY_PREFIX = "cn.woobx.webapp.";
    private static final String BILIBILI_PACKAGE = "tv.danmaku.bili";
    private static final String BILIBILI_PLAYER_ACTIVITY =
            "com.bilibili.ship.theseus.detail.UnitedBizDetailsActivity";
    private static final String BILIBILI_STORY_ACTIVITY =
            "com.bilibili.video.story.StoryVideoActivity";

    private WindowCompatibilityPolicy() {}

    public static boolean isFusionWebActivity(String packageName, String activityClassName) {
        return packageName != null
                && !"app.mapforfree".equals(packageName)
                && activityClassName != null
                && activityClassName.startsWith(FUSION_ACTIVITY_PREFIX)
                && activityClassName.endsWith("WebAppMainActivity");
    }

    public static boolean isBilibiliPlayer(String packageName, String activityClassName) {
        return BILIBILI_PACKAGE.equals(packageName)
                && (BILIBILI_PLAYER_ACTIVITY.equals(activityClassName)
                || BILIBILI_STORY_ACTIVITY.equals(activityClassName));
    }
}
