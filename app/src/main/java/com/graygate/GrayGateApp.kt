package com.graygate

import android.app.Application
import android.util.Log
import com.graygate.controller.GrayscaleController
import com.graygate.data.db.GrayGateDatabase
import com.graygate.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class GrayGateApp : Application() {

    // Application-lifetime scope: keeps GrayscaleController's configuration
    // in sync with Room/DataStore for the whole life of the process. This
    // runs in EVERY process entry point (boot receiver, QS tile, service
    // restart), not just when the UI is open — without it, a rebooted
    // device would run the detection service against an empty app list.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        val dao = GrayGateDatabase.getInstance(this).selectedAppDao()
        val settingsRepo = SettingsRepository(this)

        appScope.launch {
            combine(
                dao.observeSelectedPackageNames(),
                settingsRepo.grayscaleMode,
                settingsRepo.manualOverride,
                settingsRepo.isEnabled
            ) { packages, mode, override, enabled ->
                GrayscaleController.selectedPackages = packages.toSet()
                GrayscaleController.mode = mode
                GrayscaleController.override = override
                GrayscaleController.isEnabled = enabled
                GrayscaleController.configLoaded = true
            }.catch { e ->
                // Never let a storage error kill the sync — without it the
                // controller would silently stop tracking settings changes.
                Log.e("GrayGateApp", "Settings sync failed", e)
            }.collect {}
        }
    }
}
