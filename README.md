# Wards

Wards is a personal Android app project for adding a deliberate pause before opening selected websites or apps. Its first focus is site-specific wards for Google Chrome on a Samsung Galaxy S21+.

The app now has a basic dark interface for creating, editing, and deleting ward and target profiles. It does not yet enforce wards or monitor Chrome.

## Planned behavior

- Create named wards that either wait for a chosen time or ask for a passphrase.
- Choose whether a passphrase is shown and whether it is case-sensitive.
- Apply a ward to a website or an app.
- For a blocked website, return to the previous page after closing or completing the ward, rather than repeatedly reopening Chrome.
- Keep background work and battery use as low as Android allows.

The first behavior prototype will focus on recognizing and handling a selected website in Chrome. The exact Android mechanism and its reliability need to be confirmed on the phone before enforcement is added.

## Profiles and storage

Ward and target profiles are saved in a JSON file inside the app's private storage. Each target refers to a ward by its stable profile ID, so renaming a ward does not break its targets. A ward cannot be deleted while a target uses it; edit or remove those targets first.

Passphrases are stored in the same private app storage and are not separately encrypted. Android isolates this storage from other apps. Removing the app removes its saved profiles.

The profile editor accepts a website or domain for a website target, and a package name for an app target. These are saved profiles only: the app does not yet look up installed apps, detect active sites, or apply a ward.

## Open the project

1. Install Android Studio and its Android SDK tools.
2. Open this repository folder in Android Studio and allow Gradle sync to finish.
3. Install Android SDK Platform 36 when Android Studio prompts for it.
4. Use Android Studio's bundled JDK 17 for Gradle. The project uses Android Gradle Plugin 9.4.1, which works with Gradle 9.6 and requires JDK 17.

To use the emulator, create an Android Virtual Device from Android Studio's Device Manager and select Run. To use the Galaxy S21+, enable Developer options and USB debugging, connect the phone, and select it as the run target. Device-specific background and Chrome behavior will need to be checked on the phone itself.

## Project status

This is a personal-use starting point. The configured application ID is a placeholder and should be reviewed before any public release.
