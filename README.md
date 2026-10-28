# Wards

Wards is a personal Android app project for adding a deliberate pause before opening selected websites or apps. Its first focus is site-specific wards for Google Chrome on a Samsung Galaxy S21+.

The app has a basic dark interface for creating, editing, and deleting ward and target profiles. When its accessibility service is enabled, it can show the assigned ward when a selected app opens or when a selected website appears in Chrome.

## Planned behavior

- Create named wards that either wait for a chosen time or ask for a passphrase.
- Choose whether a passphrase is shown and whether it is case-sensitive.
- Apply a ward to a website or an app.
- For a blocked website, return to the previous page after closing or completing the ward, rather than repeatedly reopening Chrome.
- Keep background work and battery use as low as Android allows.

Website detection reads Chrome's visible address bar through Android accessibility events. App targets use their Android package names. Matching a website target includes its subdomains. Android does not expose every browser's current page through a stable public API, so site detection currently supports Chrome only and must be checked on the target phone.

**Monitoring is off until you enable the service.** Install and open Wards, then open Android Settings → Accessibility → Installed apps (the exact labels vary by device) → Wards target monitoring, and turn it on. Return to Chrome and visit the target site to check it. Android requires the owner to enable accessibility services manually. The service observes foreground window changes and Chrome's visible address bar; it does not poll in the background. Disable the service in the same settings screen to stop monitoring.

When a match is found, Wards opens a full-screen ward screen above the app or page you were using. A wait ward has one button: it says **CLOSE** during the countdown and changes to **CONTINUE** when the wait ends. Closing abandons the target and goes back in the underlying app (for a website, this returns to the previous page); continuing proceeds to the target. Passphrase wards check the configured phrase using the selected case-sensitivity rule, and can display the phrase if that option is enabled. A ward is shown once per target visit, so closing it does not immediately reopen it while that same app or website remains active.

## Profiles and storage

Ward and target profiles are saved in a JSON file inside the app's private storage. Each target refers to a ward by its stable profile ID, so renaming a ward does not break its targets. A ward cannot be deleted while a target uses it; edit or remove those targets first.

Passphrases are stored in the same private app storage and are not separately encrypted. Android isolates this storage from other apps. Removing the app removes its saved profiles.

The profile editor accepts a website or domain for a website target, and a package name for an app target. App targets can be entered directly; the app does not look up installed apps.

## Open the project

1. Install Android Studio and its Android SDK tools.
2. Open this repository folder in Android Studio and allow Gradle sync to finish.
3. Install Android SDK Platform 36 when Android Studio prompts for it.
4. Use Android Studio's bundled JDK 17 for Gradle. The project uses Android Gradle Plugin 9.4.1, which works with Gradle 9.6 and requires JDK 17.

To use the emulator, create an Android Virtual Device from Android Studio's Device Manager and select Run. To use the Galaxy S21+, enable Developer options and USB debugging, connect the phone, and select it as the run target. Device-specific background and Chrome behavior will need to be checked on the phone itself.

## Project status

This is a personal-use starting point. The configured application ID is a placeholder and should be reviewed before any public release.
