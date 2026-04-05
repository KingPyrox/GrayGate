package com.graygate.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.graygate.data.db.GrayGateDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Cleans up the selected_apps table when a package is uninstalled.
 */
class PackageChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_PACKAGE_REMOVED) return
        if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return // Update, not removal

        val packageName = intent.data?.schemeSpecificPart ?: return
        Log.d("PackageChange", "Package removed: $packageName")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = GrayGateDatabase.getInstance(context).selectedAppDao()
                dao.deleteByPackageName(packageName)
                Log.d("PackageChange", "Cleaned up $packageName from selected apps")
            } catch (e: Exception) {
                Log.e("PackageChange", "Error cleaning up package", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
