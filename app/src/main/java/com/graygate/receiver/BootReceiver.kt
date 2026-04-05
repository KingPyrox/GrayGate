package com.graygate.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.graygate.data.model.DetectionEngine
import com.graygate.data.repository.SettingsRepository
import com.graygate.service.UsageStatsDetectionService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * Restarts the detection service after device boot.
 * Only starts the UsageStats service — the AccessibilityService
 * is automatically restarted by the system if it was enabled.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON") {
            return
        }

        Log.d("BootReceiver", "Boot completed — checking if service should start")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = SettingsRepository(context)
                val isEnabled = repo.isEnabled.first()
                val engine = repo.detectionEngine.first()

                if (isEnabled && engine == DetectionEngine.USAGE_STATS) {
                    UsageStatsDetectionService.start(context)
                    Log.d("BootReceiver", "UsageStats detection service started")
                }
                // AccessibilityService restarts automatically if user enabled it
            } catch (e: Exception) {
                Log.e("BootReceiver", "Error starting service on boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
