# GrayGate — Per-App Grayscale for Android

GrayGate lets you choose which Android apps appear in grayscale. Open Instagram? Grayscale. Switch to Camera? Full color. It's automatic and runs quietly in the background.

## How It Works

Android doesn't expose a true per-app grayscale API to third-party apps. GrayGate uses a proven workaround: it monitors which app is in the foreground (via Usage Stats, polled every 1.5 seconds) and toggles Android's system-wide **daltonizer** (the accessibility color filter) on and off as you switch between apps.

This means grayscale applies to the **entire screen** (including the status bar and nav bar) when a selected app is open — not just the app's window. This is a fundamental Android limitation shared by every app in this space.

GrayGate does **not** use the AccessibilityService API. If you already use a color-correction filter (e.g., for color blindness), GrayGate saves your setting and restores it whenever grayscale turns off.

## Setup — once, then never again

`WRITE_SECURE_SETTINGS` can't be requested from inside an app on unrooted Android, so it
has to be granted over ADB. This happens **one time**: it survives reboots, app updates,
and Android version upgrades. Plug into any computer, paste one line, unplug.

```bash
adb shell pm grant com.graygate android.permission.WRITE_SECURE_SETTINGS
```

The in-app onboarding flow walks you through this — including a **Copy command** button, so
you never type it by hand — along with all other required permissions.

## Features

- **Whitelist mode:** Everything is grayscale except the apps you pick. Newly installed apps
  are gray by default, so the calm doesn't erode over time.
- **Blacklist mode:** Selected apps appear in grayscale; everything else stays in color
- **Quick Settings tile:** Tap to cycle between Auto / Force Grayscale / Force Color
- **Anti-flicker system:** Debouncing and package ignore lists prevent flickering from system UI transitions
- **Boot persistence:** Service restarts automatically after device reboot
- **Battery friendly:** Detection pauses while the screen is off

## Building

Open in Android Studio and build normally. Requires:
- Android SDK 36 (compile SDK and target SDK — Android 16)
- Kotlin 2.2.10 / AGP 9.2.1 (see [build.gradle.kts](build.gradle.kts))
- JDK 17
- `minSdk` is 26 (Android 8.0)

Target API 36 is a Play Store requirement: from **30 August 2026**, updates are rejected
unless the target API level is within one year of the latest Android release.

## Architecture

```
com.graygate/
├── controller/
│   ├── GrayscaleController    # Core toggle engine (daltonizer + debounce)
│   └── GrayscaleRules         # Pure decision logic (unit tested)
├── data/
│   ├── db/                    # Room database + DAO
│   ├── model/                 # Data classes + enums
│   └── repository/            # DataStore settings
├── receiver/
│   ├── BootReceiver           # Restart service on boot
│   └── PackageChangeReceiver  # Clean up uninstalled apps
├── service/
│   ├── UsageStatsDetectionService    # Foreground app detection (polling)
│   └── GrayscaleTileService          # Quick Settings tile
├── ui/
│   ├── screens/
│   │   ├── OnboardingScreen   # Permission setup wizard
│   │   ├── AppListScreen      # Main app selection UI
│   │   └── SettingsScreen     # Overrides, permission status
│   ├── theme/                 # Material 3 theming
│   ├── MainActivity           # Entry point + navigation
│   └── MainViewModel          # App state management
├── util/
│   └── PermissionUtil         # Permission checking helpers
└── GrayGateApp                # Application: syncs settings → controller
```

## License

Copyright (c) 2026 GrayGate. All rights reserved. See [LICENSE](LICENSE) for details.
