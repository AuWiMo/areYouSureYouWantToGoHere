package dev.areyousureyou.wanttogohere.enforcement;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PassphraseMatcherTest {
    @Test public void caseSensitivitySettingControlsPassphraseAcceptance() {
        assertTrue(PassphraseMatcher.matches("NorthStar", "northstar", false));
        assertFalse(PassphraseMatcher.matches("NorthStar", "northstar", true));
        assertTrue(PassphraseMatcher.matches("NorthStar", "NorthStar", true));
    }

    @Test public void missingPassphraseInputNeverMatches() {
        assertFalse(PassphraseMatcher.matches("NorthStar", null, false));
    }
}
