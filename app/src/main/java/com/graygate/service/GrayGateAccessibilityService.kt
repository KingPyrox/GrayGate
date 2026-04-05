package com.graygate.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.pm.PackageManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.graygate.controller.GrayscaleController

/**
 * AccessibilityService that listens for TYPE_WINDOW_STATE_CHANGED events
 * to detect foreground app changes with ~100ms latency.
 *
 * This is the "Fast" detection mode. It provides near-instant grayscale
 * switching but requires the user to manually enable this service in
 * Settings > Accessibility, and may face Play Store review scrutiny.
 *
 * The service requests only typeWindowStateChanged events and sets
 * canRetrieveWindowContent=false to minimize its permission footprint.
 */
class GrayGateAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "GrayGateA11y"
        var isRunning = false
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        Log.d(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        val className = event.className?.toString()

        // Validate that the class is an actual Activity, not a popup/overlay/dialog.
        // This prevents flickering from transient system UI elements.
        if (className != null && !isActivityClass(packageName, className)) {
            Log.d(TAG, "Ignoring non-Activity: $packageName / $className")
            return
        }

        GrayscaleController.onForegroundAppChanged(packageName, this)
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        GrayscaleController.forceColorMode(this)
        Log.d(TAG, "Accessibility service destroyed — grayscale disabled")
    }

    /**
     * Verify that the className from the accessibility event corresponds
     * to an actual Activity registered in the target app's manifest.
     * This filters out popups, input methods, overlays, and system dialogs
     * that would otherwise cause flickering.
     */
    private fun isActivityClass(packageName: String, className: String): Boolean {
        return try {
            val componentName = ComponentName(packageName, className)
            packageManager.getActivityInfo(componentName, PackageManager.GET_META_DATA)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        } catch (e: Exception) {
            // If we can't determine, assume it's valid to avoid suppressing real events
            true
        }
    }
}
