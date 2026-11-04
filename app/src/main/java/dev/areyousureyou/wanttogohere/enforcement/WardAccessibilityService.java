package dev.areyousureyou.wanttogohere.enforcement;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.provider.Settings;
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
    private static final long APP_EXIT_CONFIRMATION_MS = 800;
    private static WardAccessibilityService activeInstance;
    private final Handler eventHandler = new Handler(Looper.getMainLooper());
    private final TargetVisitGate visitGate = new TargetVisitGate();
    private String activeAppTargetPackage;
    private Runnable pendingAppExit;
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
                    if (activeAppTargetPackage != null) scheduleAppExitConfirmation();
                    else visitGate.leaveTarget();
                }
                return;
            }
        }
        if (target == null) {
            // Content changes and transient system windows do not necessarily mean
            // the user left the warded app. Reset only after a real app switch.
            if (activeAppTargetPackage != null
                    && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    && !activeAppTargetPackage.equals(packageName)
                    && !isTransientWindowPackage(packageName)) {
                scheduleAppExitConfirmation();
            }
            return;
        }
        cancelPendingAppExit();
        WardProfile ward = findWard(profiles.getWards(), target.wardId);
        if (ward == null) return;
        if (!visitGate.shouldShow(target.id)) return;
        activeAppTargetPackage = target.sourceType == TargetProfile.SourceType.APP
                ? target.sourceValue : null;
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

    private boolean isTransientWindowPackage(String packageName) {
        if ("com.android.systemui".equals(packageName)) return true;
        String defaultInputMethod = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        if (defaultInputMethod == null) return false;
        int separator = defaultInputMethod.indexOf('/');
        String inputMethodPackage = separator < 0
                ? defaultInputMethod : defaultInputMethod.substring(0, separator);
        return packageName.equals(inputMethodPackage);
    }

    private void scheduleAppExitConfirmation() {
        cancelPendingAppExit();
        pendingAppExit = () -> {
            visitGate.leaveTarget();
            activeAppTargetPackage = null;
            pendingAppExit = null;
        };
        eventHandler.postDelayed(pendingAppExit, APP_EXIT_CONFIRMATION_MS);
    }

    private void cancelPendingAppExit() {
        if (pendingAppExit == null) return;
        eventHandler.removeCallbacks(pendingAppExit);
        pendingAppExit = null;
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
        cancelPendingAppExit();
        if (activeInstance == this) activeInstance = null;
        super.onDestroy();
    }
}
