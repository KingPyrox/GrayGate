package com.graygate.service

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.graygate.R
import com.graygate.controller.GrayscaleController
import com.graygate.ui.MainActivity
import kotlinx.coroutines.*

/**
 * Foreground service that polls UsageStatsManager every 1.5 seconds
 * to detect the current foreground app. Polling is paused while the
 * screen is off to avoid needless battery drain.
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
    private var pollJob: Job? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(TAG, "Screen off — pausing polling")
                    stopPolling()
                }
                Intent.ACTION_SCREEN_ON -> {
                    Log.d(TAG, "Screen on — resuming polling")
                    startPolling()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (powerManager.isInteractive) {
            startPolling()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenReceiver)
        serviceScope.cancel()
        GrayscaleController.forceColorMode(this)
        Log.d(TAG, "Service destroyed — grayscale disabled")
    }

    private fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = serviceScope.launch {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            if (usageStatsManager == null) {
                Log.e(TAG, "UsageStatsManager not available")
                return@launch
            }

            while (isActive) {
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

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    /**
     * Query UsageStatsManager for the most recent app-resume event
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
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
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
