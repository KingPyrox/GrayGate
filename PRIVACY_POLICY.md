# Privacy Policy — GrayGate

**Last updated:** April 5, 2026

## Overview

GrayGate is a local-only Android application. It does not collect, store, or transmit any personal data. All data remains on your device.

## Data Collection

**GrayGate does not collect any data.** Specifically:

- No personal information is collected
- No usage analytics or tracking
- No crash reporting to external services
- No network requests of any kind
- No advertisements

## Data Stored on Device

GrayGate stores the following data **locally on your device only**:

- **Selected apps list:** The apps you choose to apply grayscale to, stored in a local database
- **App preferences:** Your settings (grayscale mode, detection engine, manual override), stored in local preferences

This data never leaves your device and is deleted when you uninstall the app.

## Permissions

GrayGate requests the following permissions, used exclusively for core functionality:

- **Usage Access:** To detect which app is currently in the foreground. No usage data is stored or transmitted.
- **Write Secure Settings (via ADB):** To toggle the system grayscale filter. This is a one-time setup step.
- **Query Installed Apps:** To display your installed apps so you can select which ones to grayscale. The app list is not stored or transmitted.
- **Foreground Service:** To run the app detection service in the background.
- **Boot Completed:** To restart the detection service after device reboot.
- **Notifications:** To display a persistent notification while the service is running.

## Third-Party Services

GrayGate does not integrate with any third-party services, SDKs, or APIs.

## Children's Privacy

GrayGate does not collect data from anyone, including children under 13.

## Changes to This Policy

If this policy changes, the updated version will be posted here with a new date.

## Contact

If you have questions about this privacy policy, please open an issue on the project's GitHub repository.
