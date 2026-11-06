package com.auwimo.areyousure.enforcement;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.provider.Settings;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.auwimo.areyousure.WardActivity;
import com.auwimo.areyousure.data.JsonProfileRepository;
import com.auwimo.areyousure.data.ProfileRepository;
import com.auwimo.areyousure.data.TargetProfile;
import com.auwimo.areyousure.data.WardProfile;

import java.io.IOException;
import java.util.List;

/** Event-driven observer for configured app targets and Chrome website targets. */
public final class WardAccessibilityService extends AccessibilityService {
    private static final String CHROME = "com.android.chrome";
    private static final long APP_EXIT_CONFIRMATION_MS = 800;
    private static final long WEBSITE_CONFIRMATION_MS = 600;
    private static WardAccessibilityService activeInstance;
    private final Handler eventHandler = new Handler(Looper.getMainLooper());
    private final TargetVisitGate visitGate = new TargetVisitGate();
    private String activeAppTargetPackage;
    private Runnable pendingAppExit;
    private Runnable pendingWebsiteConfirmation;
    private String pendingWebsiteAddress;
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
            ChromeAddress observed = readChromeAddress(getRootInActiveWindow());
            if (observed.focused) {
                cancelWebsiteConfirmation();
                if (target != null) {
                    // Continue below so an explicitly configured Chrome app target still works.
                } else {
                    return;
                }
            } else if (target == null) {
                scheduleWebsiteConfirmation(observed.text, targets);
                return;
            } else {
                scheduleWebsiteConfirmation(observed.text, targets);
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

    private void scheduleWebsiteConfirmation(String address, List<TargetProfile> targets) {
        if (!TargetMatcher.hasRecognizableWebsiteHost(address)) {
            cancelWebsiteConfirmation();
            if (activeAppTargetPackage != null) scheduleAppExitConfirmation();
            else visitGate.leaveTarget();
            return;
        }
        if (address.equals(pendingWebsiteAddress) && pendingWebsiteConfirmation != null) return;
        cancelWebsiteConfirmation();
        pendingWebsiteAddress = address;
        List<TargetProfile> observedTargets = targets;
        pendingWebsiteConfirmation = () -> {
            pendingWebsiteConfirmation = null;
            ChromeAddress current = readChromeAddress(getRootInActiveWindow());
            if (current.focused || !address.equals(current.text)) return;
            TargetProfile website = TargetMatcher.findWebsiteTarget(observedTargets, current.text);
            if (website != null) {
                onAccessibilityEventForTarget(website);
            } else {
                TargetProfile app = TargetMatcher.findAppTarget(observedTargets, CHROME);
                if (app != null) onAccessibilityEventForTarget(app);
                else if (activeAppTargetPackage != null) scheduleAppExitConfirmation();
                else visitGate.leaveTarget();
            }
            pendingWebsiteAddress = null;
        };
        eventHandler.postDelayed(pendingWebsiteConfirmation, WEBSITE_CONFIRMATION_MS);
    }

    private void cancelWebsiteConfirmation() {
        if (pendingWebsiteConfirmation != null) {
            eventHandler.removeCallbacks(pendingWebsiteConfirmation);
            pendingWebsiteConfirmation = null;
        }
        pendingWebsiteAddress = null;
    }

    private void onAccessibilityEventForTarget(TargetProfile target) {
        cancelPendingAppExit();
        WardProfile ward = findWard(profiles.getWards(), target.wardId);
        if (ward == null || !visitGate.shouldShow(target.id)) return;
        activeAppTargetPackage = target.sourceType == TargetProfile.SourceType.APP
                ? target.sourceValue : null;
        Intent intent = new Intent(this, WardActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_SINGLE_TOP)
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

    private ChromeAddress readChromeAddress(AccessibilityNodeInfo root) {
        if (root == null) return new ChromeAddress(null, false);
        ChromeAddress address = new ChromeAddress(null, false);
        visitNodes(root, address);
        return address;
    }

    private void visitNodes(AccessibilityNodeInfo node, ChromeAddress address) {
        if (node == null || address.text != null) return;
        CharSequence id = node.getViewIdResourceName();
        CharSequence text = node.getText();
        if (id != null && id.toString().contains("url_bar")) {
            if (text != null) address.text = text.toString();
            address.focused = node.isFocused() || node.isAccessibilityFocused();
            return;
        }
        for (int index = 0; index < node.getChildCount(); index++) {
            visitNodes(node.getChild(index), address);
        }
    }

    private static final class ChromeAddress {
        String text;
        boolean focused;
        ChromeAddress(String text, boolean focused) { this.text = text; this.focused = focused; }
    }

    @Override public void onInterrupt() { }

    @Override public void onDestroy() {
        cancelPendingAppExit();
        cancelWebsiteConfirmation();
        if (activeInstance == this) activeInstance = null;
        super.onDestroy();
    }
}
