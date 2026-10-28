package dev.areyousureyou.wanttogohere.enforcement;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import dev.areyousureyou.wanttogohere.WardActivity;
import dev.areyousureyou.wanttogohere.data.JsonProfileRepository;
import dev.areyousureyou.wanttogohere.data.ProfileRepository;
import dev.areyousureyou.wanttogohere.data.TargetProfile;
import dev.areyousureyou.wanttogohere.data.WardProfile;

import java.io.IOException;
import java.util.List;

/** Event-driven observer for configured app targets and Chrome website targets. */
public final class WardAccessibilityService extends AccessibilityService {
    private static final String CHROME = "com.android.chrome";
    private String suppressedTargetId;
    private ProfileRepository profiles;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        try {
            profiles = new JsonProfileRepository(this);
        } catch (IOException exception) {
            profiles = null;
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        String packageName = event.getPackageName().toString();
        if (getPackageName().equals(packageName)) {
            refreshProfiles();
            return;
        }
        if (profiles == null) {
            try {
                profiles = new JsonProfileRepository(this);
            } catch (IOException exception) {
                return;
            }
        }

        List<TargetProfile> targets = profiles.getTargets();
        TargetProfile target = TargetMatcher.findAppTarget(targets, packageName);
        if (CHROME.equals(packageName)) {
            String address = readChromeAddress(getRootInActiveWindow());
            TargetProfile website = TargetMatcher.findWebsiteTarget(targets, address);
            if (website != null) target = website;
            else if (target == null) {
                suppressedTargetId = null;
                return;
            }
        }
        if (target == null) {
            suppressedTargetId = null;
            return;
        }
        if (target.id.equals(suppressedTargetId)) return;

        WardProfile ward = findWard(profiles.getWards(), target.wardId);
        if (ward == null) return;
        suppressedTargetId = target.id;
        Intent intent = new Intent(this, WardActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(WardActivity.EXTRA_TARGET_ID, target.id)
                .putExtra(WardActivity.EXTRA_TARGET_NAME, target.name)
                .putExtra(WardActivity.EXTRA_WARD_NAME, ward.name)
                .putExtra(WardActivity.EXTRA_MODE, ward.mode.name())
                .putExtra(WardActivity.EXTRA_WAIT_SECONDS, ward.waitSeconds)
                .putExtra(WardActivity.EXTRA_PASSPHRASE, ward.passphrase)
                .putExtra(WardActivity.EXTRA_SHOW_PASSPHRASE, ward.showPassphrase)
                .putExtra(WardActivity.EXTRA_CASE_SENSITIVE, ward.caseSensitive);
        startActivity(intent);
    }

    private WardProfile findWard(List<WardProfile> wards, String wardId) {
        for (WardProfile ward : wards) {
            if (ward.id.equals(wardId)) return ward;
        }
        return null;
    }

    private void refreshProfiles() {
        try {
            profiles = new JsonProfileRepository(this);
        } catch (IOException exception) {
            profiles = null;
        }
    }

    private String readChromeAddress(AccessibilityNodeInfo root) {
        if (root == null) return null;
        String[] address = {null};
        visitNodes(root, address);
        return address[0];
    }

    private void visitNodes(AccessibilityNodeInfo node, String[] address) {
        if (node == null || address[0] != null) return;
        CharSequence id = node.getViewIdResourceName();
        CharSequence text = node.getText();
        if (id != null && id.toString().contains("url_bar") && text != null) {
            address[0] = text.toString();
            return;
        }
        for (int index = 0; index < node.getChildCount(); index++) {
            visitNodes(node.getChild(index), address);
        }
    }

    @Override public void onInterrupt() { }
}
