package com.graygate.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.graygate.data.repository.SettingsRepository
import com.graygate.service.UsageStatsDetectionService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * Restarts the detection service after device boot.
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
                val onboardingComplete = repo.isOnboardingComplete.first()

                if (isEnabled && onboardingComplete) {
                    UsageStatsDetectionService.start(context)
                    Log.d("BootReceiver", "Detection service started")
                }
            } catch (e: Exception) {
                Log.e("BootReceiver", "Error starting service on boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
