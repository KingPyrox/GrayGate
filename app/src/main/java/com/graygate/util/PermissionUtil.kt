package com.graygate.util

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.graygate.controller.GrayscaleController
import com.graygate.service.GrayGateAccessibilityService

/**
 * Utility for checking and requesting the various permissions
 * and settings that GrayGate needs to function.
 */
object PermissionUtil {

    /**
     * Check if PACKAGE_USAGE_STATS has been granted.
     * This requires the user to enable it in Settings > Usage Access.
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Check if WRITE_SECURE_SETTINGS has been granted (via ADB).
     */
    fun hasWriteSecureSettings(context: Context): Boolean {
        return GrayscaleController.hasWriteSecureSettings(context)
    }

    /**
     * Check if the AccessibilityService is currently enabled.
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        return GrayGateAccessibilityService.isRunning
    }

    /**
     * Check if battery optimization is disabled for this app.
     */
    fun isBatteryOptimizationDisabled(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Intent to open Usage Access settings.
     */
    fun usageAccessIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    }

    /**
     * Intent to open Accessibility settings.
     */
    fun accessibilityIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }

    /**
     * Intent to disable battery optimization for this app.
     */
    fun batteryOptimizationIntent(context: Context): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    /**
     * Get the ADB command to grant WRITE_SECURE_SETTINGS.
     */
    fun getAdbCommand(context: Context): String {
        return "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"
    }
}
