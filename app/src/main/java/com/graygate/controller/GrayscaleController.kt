package com.graygate.controller

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import android.util.Log
import com.graygate.data.model.GrayscaleMode
import com.graygate.data.model.ManualOverride

/**
 * Core grayscale toggle engine.
 *
 * This controller manages the system-wide daltonizer setting, toggling it
 * on/off as the foreground app changes. It includes debounce logic and a
 * package ignore list to prevent flickering from system UI transitions.
 *
 * The daltonizer approach is the only viable method for non-root, non-system
 * apps to achieve grayscale on Android. It applies to the ENTIRE display
 * (status bar, nav bar, etc.), not just the target app's window.
 */
object GrayscaleController {

    private const val TAG = "GrayscaleController"

    // Daltonizer Settings.Secure keys
    private const val DALTONIZER_ENABLED = "accessibility_display_daltonizer_enabled"
    private const val DALTONIZER_MODE = "accessibility_display_daltonizer"

    // Daltonizer mode values
    private const val DALTONIZER_GRAYSCALE = 0      // Monochromacy / full grayscale
    private const val DALTONIZER_DISABLED = -1      // Normal color

    // Anti-flicker: minimum time between state changes
    private const val DEBOUNCE_MS = 1500L

    // Packages that should NEVER trigger a grayscale toggle
    private val IGNORED_PACKAGES = setOf(
        "com.android.systemui",             // Status bar, volume panel, quick settings
        "android",                          // System dialogs
        "com.android.permissioncontroller", // Permission prompts
        "com.android.shell",                // Shell / ADB
        "com.android.inputmethod.latin",    // Google Keyboard
        "com.google.android.inputmethod.latin",
        "com.samsung.android.honeyboard",   // Samsung Keyboard
        "com.swiftkey.swiftkey",            // SwiftKey
        "com.android.incallui",             // Incoming call UI
        "com.android.dialer",               // Phone app during calls
        "com.google.android.dialer",
        "com.android.settings",             // System settings (e.g., Accessibility page)
        "com.google.android.gms",           // Google Play Services overlays
    )

    // State tracking
    private var currentGrayscaleState: Boolean = false
    private var lastToggleTime: Long = 0L
    private var lastForegroundPackage: String = ""

    // Configuration — updated by the app when settings change
    @Volatile var mode: GrayscaleMode = GrayscaleMode.BLACKLIST
    @Volatile var override: ManualOverride = ManualOverride.AUTO
    @Volatile var selectedPackages: Set<String> = emptySet()
    @Volatile var isEnabled: Boolean = true

    /**
     * Called by the detection service whenever the foreground app changes.
     */
    fun onForegroundAppChanged(packageName: String, context: Context) {
        if (!isEnabled) return
        if (packageName.isBlank()) return
        if (packageName == lastForegroundPackage) return

        // Check ignore list
        if (packageName in IGNORED_PACKAGES) {
            Log.d(TAG, "Ignoring system package: $packageName")
            return
        }

        // Ignore our own package
        if (packageName == context.packageName) {
            Log.d(TAG, "Ignoring self: $packageName")
            return
        }

        lastForegroundPackage = packageName

        // Handle manual overrides
        when (override) {
            ManualOverride.FORCE_GRAY -> {
                setGrayscaleIfChanged(context, true)
                return
            }
            ManualOverride.FORCE_COLOR -> {
                setGrayscaleIfChanged(context, false)
                return
            }
            ManualOverride.AUTO -> { /* Continue to per-app logic */ }
        }

        // Determine desired state based on mode and selected packages
        val shouldBeGray = when (mode) {
            GrayscaleMode.BLACKLIST -> packageName in selectedPackages
            GrayscaleMode.WHITELIST -> packageName !in selectedPackages
        }

        Log.d(TAG, "App: $packageName | Mode: $mode | ShouldGray: $shouldBeGray")

        // Apply debounce
        val now = System.currentTimeMillis()
        if (now - lastToggleTime < DEBOUNCE_MS && shouldBeGray != currentGrayscaleState) {
            Log.d(TAG, "Debounced — skipping toggle (${now - lastToggleTime}ms since last)")
            // Post a delayed re-check
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (lastForegroundPackage == packageName) {
                    setGrayscaleIfChanged(context, shouldBeGray)
                }
            }, DEBOUNCE_MS - (now - lastToggleTime) + 50)
            return
        }

        setGrayscaleIfChanged(context, shouldBeGray)
    }

    /**
     * Only write to Settings.Secure if the state actually needs to change.
     */
    private fun setGrayscaleIfChanged(context: Context, enabled: Boolean) {
        if (enabled == currentGrayscaleState) return
        setGrayscale(context.contentResolver, enabled)
        currentGrayscaleState = enabled
        lastToggleTime = System.currentTimeMillis()
        Log.d(TAG, "Grayscale ${if (enabled) "ON" else "OFF"}")
    }

    /**
     * Write to Settings.Secure to toggle the daltonizer.
     * Requires WRITE_SECURE_SETTINGS permission (granted via ADB).
     */
    private fun setGrayscale(resolver: ContentResolver, enabled: Boolean) {
        try {
            Settings.Secure.putInt(
                resolver,
                DALTONIZER_ENABLED,
                if (enabled) 1 else 0
            )
            Settings.Secure.putInt(
                resolver,
                DALTONIZER_MODE,
                if (enabled) DALTONIZER_GRAYSCALE else DALTONIZER_DISABLED
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "WRITE_SECURE_SETTINGS not granted!", e)
        }
    }

    /**
     * Force grayscale off — called when the service stops or app is paused.
     */
    fun forceColorMode(context: Context) {
        setGrayscale(context.contentResolver, false)
        currentGrayscaleState = false
        lastForegroundPackage = ""
        lastToggleTime = 0L
    }

    /**
     * Reset internal state when switching detection modes so the next
     * foreground event is processed cleanly without stale tracking data.
     */
    fun resetState(context: Context) {
        Log.d(TAG, "Resetting controller state")
        forceColorMode(context)
    }

    /**
     * Check if WRITE_SECURE_SETTINGS has been granted.
     */
    fun hasWriteSecureSettings(context: Context): Boolean {
        return try {
            // Try writing — if this doesn't throw, we have permission
            val current = Settings.Secure.getInt(context.contentResolver, DALTONIZER_ENABLED, 0)
            Settings.Secure.putInt(context.contentResolver, DALTONIZER_ENABLED, current)
            true
        } catch (e: Exception) {
            false
        }
    }
}
