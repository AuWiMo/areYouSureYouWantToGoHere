# Wards

Wards is a personal Android app project for adding a deliberate pause before opening selected websites or apps. Its first focus is site-specific wards for Google Chrome on a Samsung Galaxy S21+.

The project is currently at the setup stage. This repository contains the Android build scaffold and project notes; it does not yet contain a launchable app or enforcement behavior.

## Planned behavior

- Create named wards that either wait for a chosen time or ask for a passphrase.
- Choose whether a passphrase is shown and whether it is case-sensitive.
- Apply a ward to a website or an app.
- For a blocked website, return to the previous page after closing or completing the ward, rather than repeatedly reopening Chrome.
- Keep background work and battery use as low as Android allows.

The first prototype will focus on recognizing and handling a selected website in Chrome. The exact Android mechanism and its reliability need to be confirmed on the phone before the rest of the app is built.

## Open the project

1. Install Android Studio and its Android SDK tools.
2. Open this repository folder in Android Studio and allow Gradle sync to finish.
3. Install Android SDK Platform 36 when Android Studio prompts for it.
4. Use Android Studio's bundled JDK 17 for Gradle. The project uses Android Gradle Plugin 8.13.2, which requires Gradle 8.13 and JDK 17.

To use the emulator, create an Android Virtual Device from Android Studio's Device Manager and select Run after the app module has a launcher screen. To use the Galaxy S21+, enable Developer options and USB debugging, connect the phone, and select it as the run target. Device-specific background and Chrome behavior will need to be checked on the phone itself.

## Project status

This is a personal-use starting point. App screens, ward storage, Chrome site detection, and enforcement will be added in later changes. The configured application ID is a placeholder and should be reviewed before any public release.
