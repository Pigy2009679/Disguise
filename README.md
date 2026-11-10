# DISGUISE

DISGUISE is a native Android app for creating separate, customizable disguise experiences associated with apps already installed on your phone.

## Current MVP

- Discover launchable apps visible to Android's package-visibility rules.
- Create separate Notes, Calculator, Weather, and Calendar disguise profiles.
- Configure an independent unlock method for each profile:
  - Notes: type the configured phrase in the Notes search field.
  - Calculator: enter the configured code/expression and press equals.
  - Weather: triple-tap the configured weekday.
  - Calendar: select the configured date and triple-tap the configured event.
- Preview each disguise inside DISGUISE.
- Request a home-screen pinned shortcut for each disguise.
- Keep the original app installed and unchanged.
- Open the original app from the private space after unlocking.
- Store profile settings and notes locally on the device.

## Important Android limitation

A normal third-party Android app cannot replace the internal interface or package identity of another installed app. A DISGUISE shortcut opens a separate screen implemented by DISGUISE; it does not turn TikTok itself into Notes. The original app remains installed and unchanged.

## Build an APK

The GitHub Actions workflow builds a debug APK whenever code is pushed to main, and can also be started manually from the Actions tab.

The output artifact is named disguise-debug-apk and contains:

app/build/outputs/apk/debug/app-debug.apk

For local builds, install JDK 17 and Android SDK (platform 35), then run:

gradle assembleDebug

## Security status

This is an early MVP. Unlock phrases/codes are salted and hashed, but local private notes currently use ordinary app-private preferences and are not encrypted. Do not store highly sensitive information in this version. A production release should add Android Keystore-backed encryption, biometric/device credential protection, backup controls, and additional tests.
