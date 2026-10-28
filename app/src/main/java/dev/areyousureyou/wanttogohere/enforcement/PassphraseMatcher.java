package dev.areyousureyou.wanttogohere.enforcement;

public final class PassphraseMatcher {
    private PassphraseMatcher() { }

    public static boolean matches(String expected, String entered, boolean caseSensitive) {
        if (expected == null || entered == null) return false;
        return caseSensitive ? expected.equals(entered) : expected.equalsIgnoreCase(entered);
    }
}
