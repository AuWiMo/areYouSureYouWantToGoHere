package com.auwimo.areyousure.enforcement;

import com.auwimo.areyousure.data.TargetProfile;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

/** Matches observed foreground apps and website hosts to configured targets. */
public final class TargetMatcher {
    private TargetMatcher() { }

    public static TargetProfile findAppTarget(List<TargetProfile> targets, String packageName) {
        if (packageName == null || packageName.isEmpty()) return null;
        for (TargetProfile target : targets) {
            if (target.sourceType == TargetProfile.SourceType.APP
                    && packageName.equals(target.sourceValue.trim())) return target;
        }
        return null;
    }

    public static TargetProfile findWebsiteTarget(List<TargetProfile> targets, String address) {
        String host = hostFromAddress(address);
        if (host == null) return null;
        for (TargetProfile target : targets) {
            if (target.sourceType != TargetProfile.SourceType.WEBSITE) continue;
            String configuredHost = hostFromAddress(target.sourceValue);
            if (configuredHost != null && (host.equals(configuredHost)
                    || host.endsWith("." + configuredHost))) return target;
        }
        return null;
    }

    public static boolean hasRecognizableWebsiteHost(String address) {
        return hostFromAddress(address) != null;
    }

    static String hostFromAddress(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        String address = value.trim();
        if (!address.contains("://")) address = "https://" + address;
        try {
            String host = new URI(address).getHost();
            if (host == null) return null;
            host = host.toLowerCase(Locale.ROOT);
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (URISyntaxException exception) {
            return null;
        }
    }
}
