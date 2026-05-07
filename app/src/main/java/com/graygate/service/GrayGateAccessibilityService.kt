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

        // Filter out clearly transient UI elements (dialogs, popups, toasts).
        // We do NOT require the class to be a registered Activity — many legitimate
        // foreground destinations (launchers, system UI, browsers) use classes that
        // aren't strictly Activities. Filtering on Activity-only would leave the
        // daltonizer stuck on when leaving a selected app.
        if (className != null && isTransientWindowClass(className)) {
            Log.d(TAG, "Ignoring transient window: $packageName / $className")
            return
        }

        Log.d(TAG, "Window changed: $packageName / $className")
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
     * Heuristic: identify clearly transient window classes (dialogs, popups,
     * toasts, input method UI) so we ignore them. Everything else — including
     * launchers, browsers, fragments, and unregistered Activity classes — is
     * treated as a real foreground change.
     */
    private fun isTransientWindowClass(className: String): Boolean {
        val lower = className.lowercase()
        return lower.contains("dialog") ||
                lower.contains("popup") ||
                lower.contains("toast") ||
                lower.endsWith("inputmethodservice") ||
                lower.endsWith("inputmethod")
    }
}
