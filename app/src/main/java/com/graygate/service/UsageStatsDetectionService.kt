package com.graygate.service

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.graygate.R
import com.graygate.controller.GrayscaleController
import com.graygate.ui.MainActivity
import kotlinx.coroutines.*

/**
 * Foreground service that polls UsageStatsManager every 1.5 seconds
 * to detect the current foreground app. This is the "Standard" detection
 * mode — slower than AccessibilityService but doesn't require enabling
 * an accessibility service, making it safer for Play Store distribution.
 */
class UsageStatsDetectionService : Service() {

    companion object {
        private const val TAG = "UsageStatsDetection"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "graygate_status"
        private const val POLL_INTERVAL_MS = 1500L

        fun start(context: Context) {
            val intent = Intent(context, UsageStatsDetectionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, UsageStatsDetectionService::class.java))
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isPolling = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isPolling) {
            isPolling = true
            startPolling()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isPolling = false
        serviceScope.cancel()
        GrayscaleController.forceColorMode(this)
        Log.d(TAG, "Service destroyed — grayscale disabled")
    }

    private fun startPolling() {
        serviceScope.launch {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            if (usageStatsManager == null) {
                Log.e(TAG, "UsageStatsManager not available")
                return@launch
            }

            while (isActive && isPolling) {
                try {
                    val foregroundPackage = getCurrentForegroundPackage(usageStatsManager)
                    if (foregroundPackage != null) {
                        GrayscaleController.onForegroundAppChanged(foregroundPackage, this@UsageStatsDetectionService)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Polling error", e)
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    /**
     * Query UsageStatsManager for the most recent MOVE_TO_FOREGROUND event
     * within the last 5 seconds to determine the current foreground app.
     */
    private fun getCurrentForegroundPackage(usageStatsManager: UsageStatsManager): String? {
        val now = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(now - 5000, now)
        val event = UsageEvents.Event()
        var latestPackage: String? = null
        var latestTime = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                if (event.timeStamp > latestTime) {
                    latestTime = event.timeStamp
                    latestPackage = event.packageName
                }
            }
        }

        return latestPackage
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text_standard))
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(openPending)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
