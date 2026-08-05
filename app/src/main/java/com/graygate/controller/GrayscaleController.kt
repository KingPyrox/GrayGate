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

    // Delay before restoring the remembered daltonizer mode after disabling
    // the filter — long enough for the system's filter transition to settle.
    private const val MODE_RESTORE_DELAY_MS = 500L

    private val handler = Handler(Looper.getMainLooper())
    private var pendingRecheck: Runnable? = null

    // State tracking. currentGrayscaleState is lazily initialized from the
    // real Settings.Secure values so a process restart can't leave the
    // screen stuck gray with a cached "already color" assumption.
    private var stateInitialized = false
    private var currentGrayscaleState: Boolean = false
    private var lastToggleTime: Long = 0L
    private var lastForegroundPackage: String = ""

    // The last package that actually decided the grayscale state — i.e. the most
    // recent NON-ignored foreground app. Distinct from [lastForegroundPackage],
    // which is only a dedupe key and gets cleared to force a re-poll.
    //
    // Overrides need this: returning to AUTO has no fixed target, so it has to
    // recompute from whichever app currently owns the state. It cannot use the
    // live foreground package, because when the user flips the override from
    // GrayGate's own Settings screen the foreground app *is* GrayGate — and
    // ignored packages (self, keyboards, system UI) deliberately resolve to null.
    //
    // Read by the detection service too: ForegroundResolver needs to know which
    // app owns the state to recognize "that app just stopped" as a close.
    @Volatile
    var lastDecidedPackage: String = ""
        private set

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
        lastDecidedPackage = packageName
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
        val appContext = context.applicationContext
        override = newOverride
        cancelPendingRecheck()

        // Always let the next poll through, even if the foreground app never
        // changed — the rules it evaluates against just did.
        lastForegroundPackage = ""

        when (newOverride) {
            // Never gray the screen while the master toggle is off — nothing
            // would be running to manage or revert it. Turning color ON is
            // always safe.
            ManualOverride.FORCE_GRAY -> {
                if (isEnabled && configLoaded) setGrayscaleIfChanged(appContext, true)
            }
            ManualOverride.FORCE_COLOR -> setGrayscaleIfChanged(appContext, false)
            // AUTO has no fixed target, so it must recompute from the app that
            // currently owns the state and apply the result NOW.
            //
            // Deferring to the next poll (what this used to do) left the screen
            // stuck: the poll reports GrayGate itself while the user is in the
            // Settings screen, self is an ignored package, so desiredState()
            // returned null and nothing was ever applied. Coming back from
            // FORCE_GRAY therefore stayed gray — and in WHITELIST mode leaving
            // the app didn't fix it either, since the launcher is also gray.
            ManualOverride.AUTO -> reapplyAutoRules(appContext)
        }
    }

    /**
     * Recompute and apply the per-app rule for whichever app currently owns the
     * grayscale state. No-op when nothing has been decided yet, or when the
     * owning app resolves to "don't change" (disabled / ignored).
     */
    private fun reapplyAutoRules(appContext: Context) {
        if (!isEnabled || !configLoaded) return
        val owner = lastDecidedPackage
        if (owner.isBlank()) return

        val desired = GrayscaleRules.desiredState(
            packageName = owner,
            selfPackageName = appContext.packageName,
            isEnabled = isEnabled,
            override = ManualOverride.AUTO,
            mode = mode,
            selectedPackages = selectedPackages
        ) ?: return

        Log.d(TAG, "AUTO restored | Owner: $owner | ShouldGray: $desired")
        setGrayscaleIfChanged(appContext, desired)
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
        lastDecidedPackage = ""
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
            // Mode BEFORE enabled: enabling first briefly applies whatever
            // stale mode is in the setting (e.g., the user's color-correction
            // preset) until the grayscale mode lands — a visible colored flash,
            // and the racing writes can wedge the display transform (seen live
            // on Android 17).
            Settings.Secure.putInt(resolver, DALTONIZER_MODE, DALTONIZER_GRAYSCALE)
            Settings.Secure.putInt(resolver, DALTONIZER_ENABLED, 1)
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
            if (!p.getBoolean(PREF_ORIGINAL_SAVED, false)) {
                Settings.Secure.putInt(resolver, DALTONIZER_ENABLED, 0)
                return
            }

            val origEnabled = p.getInt(PREF_ORIGINAL_ENABLED, 0)
            val origMode = p.getInt(PREF_ORIGINAL_MODE, -1)

            if (origEnabled == 1) {
                // Filter stays on, only the mode changes (user runs a
                // color-correction preset). Mode first, then enabled — same
                // ordering rule as applyGrayscale.
                Settings.Secure.putInt(resolver, DALTONIZER_MODE, origMode)
                Settings.Secure.putInt(resolver, DALTONIZER_ENABLED, 1)
            } else {
                // Filter goes off. Disable first, restore the remembered mode
                // AFTER the transition settles — writing both back-to-back races
                // the system's filter animation, which can re-apply the old mode
                // matrix while the setting reads "off" (screen stuck showing
                // color correction; observed live on Android 17).
                Settings.Secure.putInt(resolver, DALTONIZER_ENABLED, 0)
                handler.postDelayed({
                    try {
                        Settings.Secure.putInt(resolver, DALTONIZER_MODE, origMode)
                    } catch (e: SecurityException) {
                        Log.e(TAG, "WRITE_SECURE_SETTINGS not granted!", e)
                    }
                }, MODE_RESTORE_DELAY_MS)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "WRITE_SECURE_SETTINGS not granted!", e)
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
