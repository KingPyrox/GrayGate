package com.graygate.controller

import android.content.Context
import android.os.Handler
import android.os.Looper
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
 *
 * The user's pre-existing daltonizer values (e.g., a color-correction filter
 * for color blindness) are saved before the first toggle and restored
 * whenever grayscale is turned off, so GrayGate never clobbers them.
 *
 * Configuration fields are kept in sync with Room/DataStore by an
 * application-scoped collector in [com.graygate.GrayGateApp], so they are
 * correct in every process entry point (boot, tile, service restart) —
 * not just when the UI is open.
 */
object GrayscaleController {

    private const val TAG = "GrayscaleController"

    // Daltonizer Settings.Secure keys
    private const val DALTONIZER_ENABLED = "accessibility_display_daltonizer_enabled"
    private const val DALTONIZER_MODE = "accessibility_display_daltonizer"

    // Daltonizer mode value for monochromacy / full grayscale
    private const val DALTONIZER_GRAYSCALE = 0

    // Where the user's original daltonizer values are persisted
    private const val PREFS_NAME = "graygate_daltonizer"
    private const val PREF_ORIGINAL_SAVED = "original_saved"
    private const val PREF_ORIGINAL_ENABLED = "original_enabled"
    private const val PREF_ORIGINAL_MODE = "original_mode"

    // Anti-flicker: minimum time between state changes
    private const val DEBOUNCE_MS = 1500L

    private val handler = Handler(Looper.getMainLooper())
    private var pendingRecheck: Runnable? = null

    // State tracking. currentGrayscaleState is lazily initialized from the
    // real Settings.Secure values so a process restart can't leave the
    // screen stuck gray with a cached "already color" assumption.
    private var stateInitialized = false
    private var currentGrayscaleState: Boolean = false
    private var lastToggleTime: Long = 0L
    private var lastForegroundPackage: String = ""

    // Configuration — synced from Room/DataStore by GrayGateApp
    @Volatile var mode: GrayscaleMode = GrayscaleMode.BLACKLIST
    @Volatile var override: ManualOverride = ManualOverride.AUTO
    @Volatile var selectedPackages: Set<String> = emptySet()
    @Volatile var isEnabled: Boolean = true

    // True once the first config sync has completed. Until then, foreground
    // events are ignored so a freshly started process can't act on defaults.
    @Volatile var configLoaded: Boolean = false

    /**
     * Called by the detection service whenever the foreground app changes.
     */
    @Synchronized
    fun onForegroundAppChanged(packageName: String, context: Context) {
        if (!configLoaded) return
        if (packageName == lastForegroundPackage) return

        val appContext = context.applicationContext
        val shouldBeGray = GrayscaleRules.desiredState(
            packageName = packageName,
            selfPackageName = appContext.packageName,
            isEnabled = isEnabled,
            override = override,
            mode = mode,
            selectedPackages = selectedPackages
        ) ?: return

        lastForegroundPackage = packageName
        Log.d(TAG, "App: $packageName | Mode: $mode | ShouldGray: $shouldBeGray")

        // Apply debounce
        val now = System.currentTimeMillis()
        if (now - lastToggleTime < DEBOUNCE_MS && shouldBeGray != currentGrayscaleState) {
            Log.d(TAG, "Debounced — skipping toggle (${now - lastToggleTime}ms since last)")
            scheduleRecheck(appContext, packageName, DEBOUNCE_MS - (now - lastToggleTime) + 50)
            return
        }

        setGrayscaleIfChanged(appContext, shouldBeGray)
    }

    /**
     * Apply a manual override immediately (Quick Settings tile / Settings screen).
     */
    @Synchronized
    fun applyOverride(context: Context, newOverride: ManualOverride) {
        override = newOverride
        cancelPendingRecheck()
        when (newOverride) {
            // Never gray the screen while the master toggle is off — nothing
            // would be running to manage or revert it. Turning color ON is
            // always safe.
            ManualOverride.FORCE_GRAY -> {
                if (isEnabled && configLoaded) setGrayscaleIfChanged(context.applicationContext, true)
            }
            ManualOverride.FORCE_COLOR -> setGrayscaleIfChanged(context.applicationContext, false)
            // Clear the tracked package so the next poll re-evaluates the
            // current foreground app against the per-app rules.
            ManualOverride.AUTO -> lastForegroundPackage = ""
        }
    }

    /**
     * Force grayscale off — called when the service stops or the app is disabled.
     * Restores the user's original daltonizer settings.
     */
    @Synchronized
    fun forceColorMode(context: Context) {
        cancelPendingRecheck()
        ensureStateInitialized(context)
        if (currentGrayscaleState) {
            restoreOriginalSettings(context.applicationContext)
        }
        currentGrayscaleState = false
        lastForegroundPackage = ""
        lastToggleTime = 0L
    }

    // --- Internals ---

    private fun scheduleRecheck(context: Context, packageName: String, delayMs: Long) {
        cancelPendingRecheck()
        val recheck = Runnable {
            synchronized(this) {
                // Re-evaluate against current config — settings or overrides
                // may have changed during the debounce window.
                if (lastForegroundPackage == packageName) {
                    val desired = GrayscaleRules.desiredState(
                        packageName, context.packageName, isEnabled, override, mode, selectedPackages
                    )
                    if (desired != null) setGrayscaleIfChanged(context, desired)
                }
            }
        }
        pendingRecheck = recheck
        handler.postDelayed(recheck, delayMs)
    }

    private fun cancelPendingRecheck() {
        pendingRecheck?.let { handler.removeCallbacks(it) }
        pendingRecheck = null
    }

    /**
     * Only write to Settings.Secure if the state actually needs to change.
     */
    private fun setGrayscaleIfChanged(context: Context, enabled: Boolean) {
        ensureStateInitialized(context)
        if (enabled == currentGrayscaleState) return
        if (enabled) applyGrayscale(context) else restoreOriginalSettings(context)
        currentGrayscaleState = enabled
        lastToggleTime = System.currentTimeMillis()
        Log.d(TAG, "Grayscale ${if (enabled) "ON" else "OFF"}")
    }

    /**
     * Read the actual daltonizer state once per process so we never trust a
     * stale in-memory assumption after the process was killed and restarted.
     */
    private fun ensureStateInitialized(context: Context) {
        if (stateInitialized) return
        stateInitialized = true
        currentGrayscaleState = try {
            val resolver = context.contentResolver
            Settings.Secure.getInt(resolver, DALTONIZER_ENABLED, 0) == 1 &&
                    Settings.Secure.getInt(resolver, DALTONIZER_MODE, -1) == DALTONIZER_GRAYSCALE
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Turn grayscale on, saving the user's pre-existing daltonizer values
     * first so they can be restored (e.g., a color-correction filter).
     * Requires WRITE_SECURE_SETTINGS permission (granted via ADB).
     */
    private fun applyGrayscale(context: Context) {
        try {
            val resolver = context.contentResolver
            val curEnabled = Settings.Secure.getInt(resolver, DALTONIZER_ENABLED, 0)
            val curMode = Settings.Secure.getInt(resolver, DALTONIZER_MODE, -1)
            if (!(curEnabled == 1 && curMode == DALTONIZER_GRAYSCALE)) {
                prefs(context).edit()
                    .putBoolean(PREF_ORIGINAL_SAVED, true)
                    .putInt(PREF_ORIGINAL_ENABLED, curEnabled)
                    .putInt(PREF_ORIGINAL_MODE, curMode)
                    .apply()
            }
            Settings.Secure.putInt(resolver, DALTONIZER_ENABLED, 1)
            Settings.Secure.putInt(resolver, DALTONIZER_MODE, DALTONIZER_GRAYSCALE)
        } catch (e: SecurityException) {
            Log.e(TAG, "WRITE_SECURE_SETTINGS not granted!", e)
        }
    }

    /**
     * Restore whatever daltonizer configuration the user had before GrayGate
     * first toggled grayscale. Falls back to simply disabling the daltonizer
     * (without touching the mode) if nothing was ever saved.
     */
    private fun restoreOriginalSettings(context: Context) {
        try {
            val resolver = context.contentResolver
            val p = prefs(context)
            if (p.getBoolean(PREF_ORIGINAL_SAVED, false)) {
                Settings.Secure.putInt(resolver, DALTONIZER_ENABLED, p.getInt(PREF_ORIGINAL_ENABLED, 0))
                Settings.Secure.putInt(resolver, DALTONIZER_MODE, p.getInt(PREF_ORIGINAL_MODE, -1))
            } else {
                Settings.Secure.putInt(resolver, DALTONIZER_ENABLED, 0)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "WRITE_SECURE_SETTINGS not granted!", e)
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
