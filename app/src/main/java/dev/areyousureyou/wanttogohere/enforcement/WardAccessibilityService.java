package dev.areyousureyou.wanttogohere.enforcement;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
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
    private static WardAccessibilityService activeInstance;
    private final TargetVisitGate visitGate = new TargetVisitGate();
    private ProfileRepository profiles;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        activeInstance = this;
        try {
            profiles = new JsonProfileRepository(this);
        } catch (IOException exception) {
            profiles = null;
        }
    }

    public static void returnToPreviousPage() {
        WardAccessibilityService service = activeInstance;
        if (service == null) return;
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> service.performGlobalAction(GLOBAL_ACTION_BACK), 350);
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
                if (TargetMatcher.hasRecognizableWebsiteHost(address)) {
                    visitGate.leaveTarget();
                }
                return;
            }
        }
        if (target == null) {
            visitGate.leaveTarget();
            return;
        }
        WardProfile ward = findWard(profiles.getWards(), target.wardId);
        if (ward == null) return;
        if (!visitGate.shouldShow(target.id)) return;
        Intent intent = new Intent(this, WardActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(WardActivity.EXTRA_TARGET_ID, target.id)
                .putExtra(WardActivity.EXTRA_TARGET_NAME, target.sourceType == TargetProfile.SourceType.APP
                        ? appLabel(target.sourceValue, target.name) : target.name)
                .putExtra(WardActivity.EXTRA_WARD_NAME, ward.name)
                .putExtra(WardActivity.EXTRA_MODE, ward.mode.name())
                .putExtra(WardActivity.EXTRA_WAIT_SECONDS, ward.waitSeconds)
                .putExtra(WardActivity.EXTRA_PASSPHRASE, ward.passphrase)
                .putExtra(WardActivity.EXTRA_SHOW_PASSPHRASE, ward.showPassphrase)
                .putExtra(WardActivity.EXTRA_CASE_SENSITIVE, ward.caseSensitive);
        startActivity(intent);
    }

    private String appLabel(String packageName, String fallback) {
        try {
            return getPackageManager().getApplicationLabel(
                    getPackageManager().getApplicationInfo(packageName, 0)).toString();
        } catch (android.content.pm.PackageManager.NameNotFoundException exception) {
            return fallback;
        }
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

    @Override public void onDestroy() {
        if (activeInstance == this) activeInstance = null;
        super.onDestroy();
    }
}
