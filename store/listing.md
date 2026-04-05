# GrayGate — Play Store Listing

## App Title
GrayGate — Per-App Grayscale

## Short Description (80 chars max)
Automatically switch selected apps to grayscale. Reduce distractions, stay focused.

## Full Description

Take control of your screen with GrayGate — the app that lets you choose which apps appear in grayscale and which stay in full color.

**How it works:**
GrayGate monitors which app you're using and automatically toggles your display's grayscale mode. Select the apps you want desaturated — like social media or games — and GrayGate handles the rest. When you switch to an app not on your list, full color returns instantly.

**Two modes to fit your style:**
- Blacklist Mode: Selected apps go grayscale, everything else stays colorful
- Whitelist Mode: Everything is grayscale except your selected apps

**Two detection speeds:**
- Standard Mode: Uses Usage Stats for reliable detection (~1-3 second delay)
- Fast Mode: Uses Accessibility Service for near-instant switching (~100ms)

**Quick Settings Tile:**
Toggle between Auto, Force Grayscale, and Force Color right from your notification shade — no need to open the app.

**Important: One-time setup required**
GrayGate uses Android's built-in grayscale filter (daltonizer), which requires a one-time permission granted via ADB from a computer. The app guides you through this simple setup during onboarding. No root required.

**Privacy first:**
- Zero data collection
- No analytics or tracking
- No network requests
- No ads
- All data stays on your device

**Features:**
- Per-app grayscale control
- Blacklist and whitelist modes
- Fast and standard detection engines
- Quick Settings tile for instant override
- Search and filter your app list
- Auto-restart on device reboot
- Battery optimization guidance
- Material You / Dynamic Color support

**Why grayscale?**
Research suggests that removing color from apps can reduce their addictive pull. Social media feeds, games, and video apps are designed with vibrant colors to grab attention. Grayscale makes them less engaging, helping you use your phone more intentionally.

## Category
Tools

## Content Rating
Everyone

## Tags
grayscale, screen time, digital wellbeing, focus, distraction, productivity, accessibility

## Permission Justifications

### QUERY_ALL_PACKAGES
GrayGate is an app management tool that must display the complete list of installed apps so users can select which ones to apply grayscale to. This is a core feature that requires full app enumeration. The app list is displayed in a selection UI and is never stored or transmitted.

### Accessibility Service
GrayGate's accessibility service is used exclusively to detect foreground app changes (TYPE_WINDOW_STATE_CHANGED events) for the "Fast Mode" detection engine. It does not read, collect, or access any screen content. The service only extracts the package name of the foreground activity to determine whether to toggle grayscale.
