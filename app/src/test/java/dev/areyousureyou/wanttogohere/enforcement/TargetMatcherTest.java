package dev.areyousureyou.wanttogohere.enforcement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import dev.areyousureyou.wanttogohere.data.TargetProfile;

public class TargetMatcherTest {
    private final TargetProfile site = new TargetProfile("site", "Video", TargetProfile.SourceType.WEBSITE,
            "example.com", "ward");
    private final TargetProfile app = new TargetProfile("app", "Mail", TargetProfile.SourceType.APP,
            "com.example.mail", "ward");
    private final List<TargetProfile> targets = Arrays.asList(site, app);

    @Test public void websiteMatchesUrlHostAndSubdomains() {
        assertEquals(site, TargetMatcher.findWebsiteTarget(targets, "https://www.example.com/watch?v=1"));
        assertEquals(site, TargetMatcher.findWebsiteTarget(targets, "m.example.com"));
    }

    @Test public void websiteDoesNotMatchLookalikeSuffixOrOtherTargetType() {
        assertNull(TargetMatcher.findWebsiteTarget(targets, "https://notexample.com"));
        assertNull(TargetMatcher.findWebsiteTarget(targets, "https://example.com.evil.test"));
    }

    @Test public void appTargetRequiresExactPackageName() {
        assertEquals(app, TargetMatcher.findAppTarget(targets, "com.example.mail"));
        assertNull(TargetMatcher.findAppTarget(targets, "com.example.mail.fake"));
    }
}
