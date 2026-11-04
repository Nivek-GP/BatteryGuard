<div align="center">
  <img src="resources/batteryguard.svg" width="100" alt="BatteryGuard">
  <h1>BatteryGuard</h1>
  <p>A full-screen alarm that tells you when to plug in and when to unplug, to keep your battery healthy.</p>
  <img src="https://img.shields.io/badge/android-13%2B%20(API%2033)-green" alt="Android">
  <img src="https://img.shields.io/badge/language-Kotlin-purple" alt="Kotlin">
</div>

## About

BatteryGuard watches your battery in the background and fires a full-screen alarm, like a clock alarm, when the charge leaves the range you set: **"Plug in"** when it drops to your minimum, **"Unplug"** when it reaches your maximum while charging.

It started with a music player: the Hiby M300 has no built-in charge limit, and keeping a battery between roughly 20 % and 80 % helps it last longer. It now runs on any device with Android 13 or later, in English and Spanish.

## Features

- **Two sliders, done** — set a minimum and a maximum (20 % and 80 % by default)
- **Hard to miss** — a full-screen alarm that wakes the screen and shows over the lock screen
- **Knows when you're charging** — "Plug in" only fires on battery, "Unplug" only while charging; plug in or unplug and the alert clears by itself
- **No nagging** — dismissing an alert snoozes it for 30 minutes
- **Your call on noise** — sound and vibration can be turned on or off
- **Set and forget** — starts with your device if monitoring was on
- **Light on battery** — event-driven, no polling: it only wakes up when the battery level changes

## Download

There are no prebuilt releases yet. When there are, they'll be on the [Releases page](https://github.com/Nivek-GP/BatteryGuard/releases) as `BatteryGuard-x.x.x.apk`. Until then, see [Building from source](#building-from-source).

## Installation

1. On your device, enable **Developer options** and **USB debugging**
2. Connect it and check that it shows up with `adb devices`
3. Install the app:

```bash
adb install -r BatteryGuard-x.x.x.apk
```

> You can also copy the APK to the device and open it from a file manager, after allowing it to install unknown apps.

## Setup

1. Open **BatteryGuard** and allow **notifications**
2. When asked, enable **Display over other apps** for BatteryGuard, so the alarm can open on top of whatever is on screen
3. Set the **minimum** (plug in below this) and the **maximum** (unplug above this)
4. **Alert options** — choose sound, vibration, or both
5. Turn on **monitoring**

> **No permission screen?** Some music player ROMs don't include the *Display over other apps* screen. Grant it over adb instead:
>
> ```bash
> adb shell appops set com.kg.batteryguard SYSTEM_ALERT_WINDOW allow
> ```

BatteryGuard runs as a foreground service, so it shows a quiet "BatteryGuard active" notification while monitoring. To dismiss an alarm, tap anywhere on the screen or press back.

## Testing without draining the battery

```bash
adb shell dumpsys battery unplug          # pretend the charger is unplugged
adb shell dumpsys battery set level 15    # below the minimum: "Plug in"
adb shell dumpsys battery set ac 1        # pretend it's charging
adb shell dumpsys battery set level 85    # above the maximum: "Unplug"
adb shell dumpsys battery reset           # back to the real state
```

> There's also a hidden **Test alert** section: tap the logo or the title 10 times in a row to show it (or hide it again).

## Building from source

Requires [Android Studio](https://developer.android.com/studio), or just its bundled JDK and the Android SDK.

Stack: Kotlin 1.9, Gradle 8.13, AGP 8.13, `compileSdk`/`targetSdk` 34, `minSdk` 33.

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"  # Android Studio's JDK on Windows
./gradlew assembleRelease  # APK in app/build/outputs/apk/release/
./gradlew assembleDebug    # debug build
```

> Release builds are signed with the key described in a local `keystore.properties` file (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`), which is never committed. Without it, the release APK is unsigned. To sideload on your own device, use the debug build.

## Related

- [BatteryLine](https://github.com/Nivek-GP/batteryline) — a thin battery line across the status bar, same look, same devices
- [App Launch Tools](https://github.com/Nivek-GP/app-launch-tools) — ShortcutLauncher and QuickLaunch, same look, same devices
