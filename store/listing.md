# GrayGate — Play Store Listing

## App Title (30 char max)

```
GrayGate — Per-App Grayscale
```

## Short Description (80 char max)

```
Automatically switch selected apps to grayscale. Cut distractions, stay focused.
```

## Full Description

Take control of your screen with GrayGate — the app that lets you choose which apps appear in grayscale and which stay in full colour.

**How it works:**
GrayGate checks which app is in the foreground (using Android's Usage Stats, roughly every 1.5 seconds) and automatically toggles your display's grayscale mode. Select the apps you want desaturated — like social media or games — and GrayGate handles the rest. When you switch to an app not on your list, full colour returns within a couple of seconds.

GrayGate does NOT use Android's AccessibilityService API. Foreground app detection is done entirely through the Usage Stats permission, which you grant in system settings during onboarding.

**Quick Settings Tile:**
Toggle between Auto, Force Grayscale, and Force Color right from your notification shade — no need to open the app.

**One 60-second setup, then never again**
GrayGate uses Android's built-in grayscale filter, which needs one permission granted from a computer — once. Not at every reboot, not after updates. The app gives you a Copy button for the exact command, so you paste one line, unplug, and never think about it again. No root required. If you already use a colour-correction filter, GrayGate saves your setting and restores it whenever grayscale turns off.

**Privacy first:**
- Zero data collection
- No analytics or tracking
- No network requests
- No ads
- All data stays on your device

**Features:**
- Per-app grayscale control
- Quick Settings tile for instant override
- Search and filter your app list
- Auto-restart on device reboot
- Battery-friendly: detection pauses while your screen is off
- Battery optimization guidance
- Material You / Dynamic Colour support

**Why grayscale?**
Some apps are designed with vibrant colours to constantly grab your attention. Grayscale removes the visual noise, giving you a calmer, less distracting screen experience. Choose which apps stay colourful and which ones fade to gray — it's your screen, your rules.

---

## Category

Tools

## Content Rating

Everyone

## Tags

grayscale, focus, distraction, productivity, customization, per-app

---

## Screenshot plan

Screenshot order matters more than most of this copy — it's what people actually look at.

1. **The gray/colour split** — the same home screen side by side, one desaturated, one not.
   Caption: "Pick the apps that pull you in."
2. **The app list with a few apps toggled on** — social media, games, whatever's distracting.
   Caption: "Select the apps that go grayscale."
3. **The 60-second setup** — the terminal command with a stopwatch overlay.
   Caption: "One setup. Then never again."
4. **Quick Settings tile** — the shade pulled down mid-cycle.
   Caption: "Override any time, without opening the app."
5. **Privacy** — the zero-collection list.
   Caption: "No network requests. Nothing to leak."

### Existing screenshots that must be replaced before this ships

- `screenshots/screen2.png` shows a **"Detection Mode -> Fast (Accessibility)"** setting.
  That feature was **removed in v1.2.0** for Play compliance and no longer exists in the app.
  A live listing showing a removed feature is a misrepresentation risk — re-shoot or drop it.
- `screenshots/screen1.png` shows the app list's **Blacklist/Whitelist mode chips**, which were
  removed when whitelist mode was dropped from the app. Since 2026-10-23 the landing page uses
  `docs/assets/app-list-rows.png/.webp` instead: a crop of the same screenshot showing only the
  app rows (no header, no mode chips). It's still an old capture: re-shoot against the current
  app list and replace `screen1.png` and the crop.
- `screenshots/screen4_grayscale.png` is **not actually in grayscale** — the Chrome logo in it
  is full colour. As the "here's the effect" shot, it currently demonstrates the opposite.
  Re-shoot with grayscale actually active.

---

## Permission Justifications

### Usage Access (PACKAGE_USAGE_STATS)

GrayGate reads Usage Stats events solely to determine which app is currently in the foreground, so it can toggle the system grayscale filter for the apps the user selected. No usage data is stored, logged, or transmitted.

### App list (queries declaration)

GrayGate declares an intent query for launcher activities (it does not request QUERY_ALL_PACKAGES). The app list is shown in a selection UI so users can choose which apps to grayscale; it is never stored beyond the user's selections or transmitted.

### WRITE_SECURE_SETTINGS

Granted by the user via a one-time ADB command. Used exclusively to toggle the system daltonizer (grayscale) setting. The user's pre-existing daltonizer configuration is saved and restored.
