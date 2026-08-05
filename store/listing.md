# GrayGate — Play Store Listing

Two complete listing variants for a Play Console **store listing experiment**.

- **Variant A (control)** — the current pitch: pick the apps you want grayed out.
- **Variant B (challenger)** — the inverse pitch: the phone is calm by default, and you
  hand color back to a short list. Also reframes the ADB step from "you need a computer"
  to "one 60-second setup, then never again."

Run them against each other on **short description + full description** first
(icon and screenshots held constant), so the copy is the only variable. Notes on
running the experiment are at the bottom.

---

## App Title (30 char max — shared by both variants)

```
GrayGate — Per-App Grayscale
```

---

# VARIANT A — control

### Short Description (80 char max)

> ⚠ The previous version of this line was **83 characters** — over Play's 80-char limit.
> Whatever is currently live on the store has therefore drifted from this file. Trimmed to
> exactly 80 below — if you edit it, re-count, there is no headroom left.

```
Automatically switch selected apps to grayscale. Cut distractions, stay focused.
```

### Full Description

Take control of your screen with GrayGate — the app that lets you choose which apps appear in grayscale and which stay in full color.

**How it works:**
GrayGate checks which app is in the foreground (using Android's Usage Stats, roughly every 1.5 seconds) and automatically toggles your display's grayscale mode. Select the apps you want desaturated — like social media or games — and GrayGate handles the rest. When you switch to an app not on your list, full color returns within a couple of seconds.

GrayGate does NOT use Android's AccessibilityService API. Foreground app detection is done entirely through the Usage Stats permission, which you grant in system settings during onboarding.

**Two modes to fit your style:**
- Blacklist Mode: Selected apps go grayscale, everything else stays colorful
- Whitelist Mode: Everything is grayscale except your selected apps

**Quick Settings Tile:**
Toggle between Auto, Force Grayscale, and Force Color right from your notification shade — no need to open the app.

**One 60-second setup, then never again**
GrayGate uses Android's built-in grayscale filter, which needs one permission granted from a computer — once. Not at every reboot, not after updates. The app gives you a Copy button for the exact command, so you paste one line, unplug, and never think about it again. No root required. If you already use a color-correction filter, GrayGate saves your setting and restores it whenever grayscale turns off.

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

---

# VARIANT B — challenger (inverse-mode pitch)

### Short Description (80 char max)

```
Your phone stays calm. Your camera and maps stay colorful. One-time setup.
```

Backup short descriptions if you want a second round:

```
Grayscale your whole phone. Keep camera, maps and photos in full color.
```

```
A calm gray phone by default. Pick the few apps that earn their color back.
```

### Full Description

Your phone stays calm. Your camera stays colorful.

GrayGate drains the color out of your phone by default — then hands it back to the handful of apps that actually need it. Camera, Maps, Photos: untouched. Everything else: quiet.

**Pick the short list, not the long one**
Most people find it easier to name the five apps that deserve color than to hunt down every app that doesn't. Turn on Whitelist mode, choose your exceptions, and you're done. New apps you install are gray automatically — so the calm doesn't erode over time.

Prefer the other direction? Blacklist mode still works exactly how you'd expect: your phone stays in color and you gray out the specific apps you're fighting with.

**One 60-second setup. Then never again.**
Android doesn't let a normal app touch the display filter, so GrayGate needs one permission granted from a computer. You do it once — not at every reboot, not after updates, not ever again.

1. Turn on USB debugging (about 30 seconds)
2. Plug into any computer — Mac, Windows, or Linux
3. Tap "Copy command" in the app, paste it, press Enter

Then unplug and forget this ever happened. No root. No accessibility service. If you already use a color-correction filter for color blindness, GrayGate saves your setting and puts it back.

**It cannot leak what it never collects**
GrayGate makes zero network requests. There is no analytics SDK, no crash reporter, no ad library. Your app list and your selections never leave your device.

- Zero data collection
- No analytics or tracking
- No network requests
- No ads
- No AccessibilityService

**What else is in there**
- Quick Settings tile: cycle Auto, Force Gray, and Force Color from your shade
- Survives reboots — the service comes back on its own
- Sleeps when you do: detection pauses while the screen is off
- Anti-flicker debouncing so system UI transitions don't strobe the screen
- Search and filter your app list
- Material You dynamic color, light and dark

**Where it falls short**
Being straight with you before you install: Android exposes no true per-app filter to third-party apps, so grayscale covers the whole screen — status bar and nav bar included. Every app in this category shares that limit. There's also about a 1–2 second delay when you switch apps, because detection polls rather than hooking into the system.

**Why grayscale?**
Color is the hook. Apps are designed with saturated reds and blues precisely because they pull your eye back to the screen. Take the color away and a lot of that pull just stops. GrayGate lets you keep it exactly where it's useful and remove it everywhere else.

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
Recommended order for **Variant B**:

1. **The gray/color split** — the same home screen side by side, one desaturated, one not.
   Caption: "Calm by default. Color where it counts."
2. **Whitelist mode in the app list** — with Camera / Maps / Photos toggled on.
   Caption: "Pick the few apps that keep their color."
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
- `screenshots/screen4_grayscale.png` is **not actually in grayscale** — the Chrome logo in it
  is full color. As the "here's the effect" shot, it currently demonstrates the opposite.
  Re-shoot with grayscale actually active.

---

## Running the experiment

Play Console → Grow → Store presence → Store listing experiments.

- Test **short + full description together**; hold icon and screenshots constant so the
  copy is the only variable.
- 50/50 split. Let it reach statistical significance on its own — Play flags this — rather
  than calling it early on a few days of noise.
- Watch **install rate from store listing page**, not raw installs.
- Secondary signal worth checking afterwards: whether Variant B changes *uninstall* rate.
  It sets a more accurate expectation about the ADB step, so a lower-volume, better-retained
  cohort is a win even if the install rate is flat.
- If B wins on copy, follow up with a screenshot experiment using the order above.

---

## Permission Justifications

### Usage Access (PACKAGE_USAGE_STATS)

GrayGate reads Usage Stats events solely to determine which app is currently in the foreground, so it can toggle the system grayscale filter for the apps the user selected. No usage data is stored, logged, or transmitted.

### App list (queries declaration)

GrayGate declares an intent query for launcher activities (it does not request QUERY_ALL_PACKAGES). The app list is shown in a selection UI so users can choose which apps to grayscale; it is never stored beyond the user's selections or transmitted.

### WRITE_SECURE_SETTINGS

Granted by the user via a one-time ADB command. Used exclusively to toggle the system daltonizer (grayscale) setting. The user's pre-existing daltonizer configuration is saved and restored.
