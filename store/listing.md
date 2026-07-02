# GrayGate — Play Store Listing

## App Title
GrayGate — Per-App Grayscale

## Short Description (80 chars max)
Automatically switch selected apps to grayscale. Reduce distractions, stay focused.

## Full Description

Take control of your screen with GrayGate — the app that lets you choose which apps appear in grayscale and which stay in full color.

**How it works:**
GrayGate checks which app is in the foreground (using Android's Usage Stats, roughly every 1.5 seconds) and automatically toggles your display's grayscale mode. Select the apps you want desaturated — like social media or games — and GrayGate handles the rest. When you switch to an app not on your list, full color returns within a couple of seconds.

GrayGate does NOT use Android's AccessibilityService API. Foreground app detection is done entirely through the Usage Stats permission, which you grant in system settings during onboarding.

**Two modes to fit your style:**
- Blacklist Mode: Selected apps go grayscale, everything else stays colorful
- Whitelist Mode: Everything is grayscale except your selected apps

**Quick Settings Tile:**
Toggle between Auto, Force Grayscale, and Force Color right from your notification shade — no need to open the app.

**Important: One-time setup required**
GrayGate uses Android's built-in grayscale filter (daltonizer), which requires a one-time permission granted via ADB from a computer. The app guides you through this simple setup during onboarding. No root required. If you already use a color-correction filter, GrayGate saves your setting and restores it whenever grayscale turns off.

**Privacy first:**
- Zero data collection
- No analytics or tracking
- No network requests
- No ads
- All data stays on your device

**Features:**
- Per-app grayscale control
- Blacklist and whitelist modes
- Quick Settings tile for instant override
- Search and filter your app list
- Auto-restart on device reboot
- Battery-friendly: detection pauses while your screen is off
- Battery optimization guidance
- Material You / Dynamic Color support

**Why grayscale?**
Some apps are designed with vibrant colors to constantly grab your attention. Grayscale removes the visual noise, giving you a calmer, less distracting screen experience. Choose which apps stay colorful and which ones fade to gray — it's your screen, your rules.

## Category
Tools

## Content Rating
Everyone

## Tags
grayscale, focus, distraction, productivity, customization, per-app

## Permission Justifications

### Usage Access (PACKAGE_USAGE_STATS)
GrayGate reads Usage Stats events solely to determine which app is currently in the foreground, so it can toggle the system grayscale filter for the apps the user selected. No usage data is stored, logged, or transmitted.

### App list (queries declaration)
GrayGate declares an intent query for launcher activities (it does not request QUERY_ALL_PACKAGES). The app list is shown in a selection UI so users can choose which apps to grayscale; it is never stored beyond the user's selections or transmitted.

### WRITE_SECURE_SETTINGS
Granted by the user via a one-time ADB command. Used exclusively to toggle the system daltonizer (grayscale) setting. The user's pre-existing daltonizer configuration is saved and restored.
