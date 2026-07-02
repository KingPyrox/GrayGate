package com.graygate.controller

import com.graygate.data.model.GrayscaleMode
import com.graygate.data.model.ManualOverride

/**
 * Pure decision logic for the grayscale toggle, kept free of Android
 * dependencies so it can be unit tested on the JVM.
 */
object GrayscaleRules {

    // Packages that should NEVER trigger a grayscale toggle
    val IGNORED_PACKAGES = setOf(
        "com.android.systemui",             // Status bar, volume panel, quick settings
        "android",                          // System dialogs
        "com.android.permissioncontroller", // Permission prompts
        "com.android.shell",                // Shell / ADB
        "com.android.inputmethod.latin",    // AOSP keyboard
        "com.google.android.inputmethod.latin", // Gboard
        "com.samsung.android.honeyboard",   // Samsung Keyboard
        "com.touchtype.swiftkey",           // SwiftKey
        "com.android.incallui",             // Incoming call UI
        "com.android.dialer",               // Phone app during calls
        "com.google.android.dialer",
        "com.android.settings",             // System settings (e.g., Accessibility page)
        "com.google.android.gms",           // Google Play Services overlays
    )

    /**
     * Should [packageName] be ignored entirely (no toggle either way)?
     */
    fun isIgnored(packageName: String, selfPackageName: String): Boolean {
        return packageName.isBlank() ||
                packageName == selfPackageName ||
                packageName in IGNORED_PACKAGES
    }

    /**
     * Desired grayscale state for [packageName], or null when the foreground
     * app should not change the current state (disabled / ignored package).
     */
    fun desiredState(
        packageName: String,
        selfPackageName: String,
        isEnabled: Boolean,
        override: ManualOverride,
        mode: GrayscaleMode,
        selectedPackages: Set<String>
    ): Boolean? {
        if (!isEnabled) return null
        if (isIgnored(packageName, selfPackageName)) return null
        return when (override) {
            ManualOverride.FORCE_GRAY -> true
            ManualOverride.FORCE_COLOR -> false
            ManualOverride.AUTO -> when (mode) {
                GrayscaleMode.BLACKLIST -> packageName in selectedPackages
                GrayscaleMode.WHITELIST -> packageName !in selectedPackages
            }
        }
    }
}
