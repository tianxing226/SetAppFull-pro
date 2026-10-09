package ss.colytitse.setappfull.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WindowCompatibilityPolicyTest {
    @Test public void fusionRequiresGeneratedWebActivityIdentity() {
        assertTrue(WindowCompatibilityPolicy.isFusionWebActivity("com.example.generated",
                "cn.woobx.webapp.WebAppMainActivity"));
        assertFalse(WindowCompatibilityPolicy.isFusionWebActivity("app.mapforfree",
                "cn.woobx.webapp.WebAppMainActivity"));
        assertFalse(WindowCompatibilityPolicy.isFusionWebActivity("com.example.generated",
                "com.example.WebAppMainActivity"));
        assertFalse(WindowCompatibilityPolicy.isFusionWebActivity("com.example.generated",
                "cn.woobx.webapp.MainActivity"));
    }

    @Test public void bilibiliCompatibilityIsLimitedToVerifiedPlayerActivity() {
        assertTrue(WindowCompatibilityPolicy.isBilibiliPlayer("tv.danmaku.bili",
                "com.bilibili.ship.theseus.detail.UnitedBizDetailsActivity"));
        assertFalse(WindowCompatibilityPolicy.isBilibiliPlayer("tv.danmaku.bili",
                "tv.danmaku.bili.MainActivityV2"));
        assertFalse(WindowCompatibilityPolicy.isBilibiliPlayer("com.example.other",
                "com.bilibili.ship.theseus.detail.UnitedBizDetailsActivity"));
    }
}
