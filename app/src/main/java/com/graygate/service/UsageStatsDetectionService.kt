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
import com.graygate.controller.ForegroundResolver
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

    // Last package the resolver decided is foreground. Re-reported every poll
    // so transiently dropped reports self-heal (see the poll loop).
    private var trackedForeground: String? = null

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
                    // Events are consumed at-most-once past the watermark, so a
                    // single dropped report (e.g., config still loading right
                    // after startup) must not be permanent: remember the last
                    // resolution and re-report it every poll. The controller
                    // dedupes unchanged packages, so repeats are cheap — and an
                    // override change that clears the dedupe key gets re-evaluated
                    // within one poll instead of waiting for the next real event.
                    getCurrentForegroundPackage(usageStatsManager)?.let { trackedForeground = it }
                    trackedForeground?.let {
                        GrayscaleController.onForegroundAppChanged(it, this@UsageStatsDetectionService)
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

    // Timestamp of the newest UsageStats event we have processed. Polling from
    // here (rather than a fixed lookback) means an event Android flushes late
    // is still picked up on a later poll instead of silently aging out — a
    // missed event used to strand the grayscale state permanently.
    private var eventWatermark = 0L

    // Default launcher package, resolved once. Reported as the foreground app
    // when the state-owning app is closed without anything else resuming.
    private var homePackageCache: String? = null

    /**
     * Query UsageStatsManager for events since the last poll and resolve the
     * current foreground app. Unlike the previous implementation this also
     * watches ACTIVITY_STOPPED: closing an app from recents usually produces
     * no RESUMED for the launcher (it was already resumed underneath), so the
     * STOPPED event of the closed app is the only signal that the user left.
     */
    private fun getCurrentForegroundPackage(usageStatsManager: UsageStatsManager): String? {
        val now = System.currentTimeMillis()
        // First poll looks back briefly; afterwards resume from the watermark,
        // capped at 60s so waking from a long screen-off doesn't replay stale
        // history (anything older than that has settled or been superseded).
        val since = if (eventWatermark == 0L) now - 5000
                    else maxOf(eventWatermark, now - 60_000)

        val events = usageStatsManager.queryEvents(since, now)
        val event = UsageEvents.Event()
        var resumedPackage: String? = null
        var resumedTime = 0L
        var stoppedPackage: String? = null
        var stoppedTime = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            // queryEvents' begin is inclusive — skip events already processed.
            if (eventWatermark != 0L && event.timeStamp <= since) continue

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED ->
                    if (event.timeStamp >= resumedTime) {
                        resumedTime = event.timeStamp
                        resumedPackage = event.packageName
                    }
                UsageEvents.Event.ACTIVITY_STOPPED ->
                    if (event.timeStamp >= stoppedTime) {
                        stoppedTime = event.timeStamp
                        stoppedPackage = event.packageName
                    }
            }
            if (event.timeStamp > eventWatermark) eventWatermark = event.timeStamp
        }

        val resolved = ForegroundResolver.resolve(
            resumedPackage = resumedPackage,
            resumedTime = resumedTime,
            stoppedPackage = stoppedPackage,
            stoppedTime = stoppedTime,
            stateOwnerPackage = GrayscaleController.lastDecidedPackage,
            selfPackageName = packageName,
            homePackage = homePackage()
        )
        // Diagnostic: one line per poll that saw anything (or resolved anything).
        if (resumedPackage != null || stoppedPackage != null || resolved != null) {
            Log.d(
                TAG,
                "poll: resumed=$resumedPackage stopped=$stoppedPackage resolved=$resolved " +
                        "tracked=$trackedForeground configLoaded=${GrayscaleController.configLoaded} " +
                        "watermark=$eventWatermark"
            )
        }
        return resolved
    }

    private fun homePackage(): String? {
        homePackageCache?.let { return it }
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)

        // resolveActivity() alone is NOT trustworthy here: it can return
        // Settings' hidden FallbackHome activity (the pre-unlock placeholder
        // home) instead of the real launcher — observed live on Pixel/Android 17.
        // Settings is on the controller's ignore list, so caching it would turn
        // the close-fallback into a silent no-op. Query all HOME activities,
        // refuse Settings, and prefer the system's pick when it survives.
        val candidates = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                homeIntent,
                android.content.pm.PackageManager.ResolveInfoFlags.of(
                    android.content.pm.PackageManager.MATCH_DEFAULT_ONLY.toLong()
                )
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(
                homeIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
            )
        }.mapNotNull { it.activityInfo?.packageName }
            .filter { it != "com.android.settings" }

        val preferred = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.resolveActivity(
                homeIntent,
                android.content.pm.PackageManager.ResolveInfoFlags.of(
                    android.content.pm.PackageManager.MATCH_DEFAULT_ONLY.toLong()
                )
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.resolveActivity(
                homeIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
            )
        }?.activityInfo?.packageName

        val home = if (preferred != null && preferred in candidates) preferred
                   else candidates.firstOrNull()
        return home?.also { homePackageCache = it }
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
