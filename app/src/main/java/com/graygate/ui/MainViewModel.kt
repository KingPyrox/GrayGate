package com.graygate.ui

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.graygate.controller.GrayscaleController
import com.graygate.data.db.GrayGateDatabase
import com.graygate.data.model.*
import com.graygate.data.repository.SettingsRepository
import com.graygate.service.UsageStatsDetectionService
import com.graygate.util.PermissionUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = GrayGateDatabase.getInstance(application).selectedAppDao()
    private val settingsRepo = SettingsRepository(application)
    private val pm = application.packageManager

    // --- Settings flows ---
    val grayscaleMode = settingsRepo.grayscaleMode.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), GrayscaleMode.BLACKLIST
    )
    val detectionEngine = settingsRepo.detectionEngine.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), DetectionEngine.USAGE_STATS
    )
    val manualOverride = settingsRepo.manualOverride.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ManualOverride.AUTO
    )
    val isEnabled = settingsRepo.isEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )
    val isOnboardingComplete = settingsRepo.isOnboardingComplete.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    // --- App list ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _showSystemApps = MutableStateFlow(true)
    val showSystemApps: StateFlow<Boolean> = _showSystemApps

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())

    private val selectedPackages = dao.observeSelectedPackageNames().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val selectedCount = dao.getSelectedCount().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )

    /**
     * Combined flow: installed apps + selected packages + search filter = final display list
     */
    val appList: StateFlow<List<AppInfo>> = combine(
        _installedApps,
        selectedPackages,
        _searchQuery,
        _showSystemApps
    ) { apps, selected, query, showSystem ->
        val selectedSet = selected.toSet()
        apps
            .filter { app ->
                if (!showSystem && isSystemApp(app.packageName)) false
                else true
            }
            .map { it.copy(isSelected = it.packageName in selectedSet) }
            .filter { app ->
                if (query.isBlank()) true
                else app.appName.contains(query, ignoreCase = true) ||
                     app.packageName.contains(query, ignoreCase = true)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Permission states (manually refreshed) ---
    private val _hasUsageStats = MutableStateFlow(false)
    val hasUsageStats: StateFlow<Boolean> = _hasUsageStats

    private val _hasWriteSecure = MutableStateFlow(false)
    val hasWriteSecure: StateFlow<Boolean> = _hasWriteSecure

    private val _hasBatteryExempt = MutableStateFlow(false)
    val hasBatteryExempt: StateFlow<Boolean> = _hasBatteryExempt

    init {
        loadInstalledApps()
        refreshPermissions()
        // Keep GrayscaleController in sync with settings
        viewModelScope.launch {
            combine(selectedPackages, grayscaleMode, manualOverride, isEnabled) { pkgs, mode, ovr, enabled ->
                GrayscaleController.selectedPackages = pkgs.toSet()
                GrayscaleController.mode = mode
                GrayscaleController.override = ovr
                GrayscaleController.isEnabled = enabled
            }.collect()
        }
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                val resolveInfos = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0))
                } else {
                    pm.queryIntentActivities(mainIntent, 0)
                }
                resolveInfos
                    .mapNotNull { resolveInfo ->
                        val appInfo = resolveInfo.activityInfo?.applicationInfo ?: return@mapNotNull null
                        AppInfo(
                            packageName = appInfo.packageName,
                            appName = appInfo.loadLabel(pm).toString()
                        )
                    }
                    .distinctBy { it.packageName }
                    .sortedBy { it.appName.lowercase() }
            }
            _installedApps.value = apps
        }
    }

    fun refreshPermissions() {
        val ctx = getApplication<Application>()
        _hasUsageStats.value = PermissionUtil.hasUsageStatsPermission(ctx)
        _hasWriteSecure.value = PermissionUtil.hasWriteSecureSettings(ctx)
        _hasBatteryExempt.value = PermissionUtil.isBatteryOptimizationDisabled(ctx)
    }

    fun toggleAppSelection(appInfo: AppInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            if (appInfo.isSelected) {
                dao.deleteByPackageName(appInfo.packageName)
            } else {
                dao.insert(SelectedApp(appInfo.packageName, appInfo.appName))
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setShowSystemApps(show: Boolean) {
        _showSystemApps.value = show
    }

    fun setGrayscaleMode(mode: GrayscaleMode) {
        viewModelScope.launch { settingsRepo.setGrayscaleMode(mode) }
    }

    fun setDetectionEngine(engine: DetectionEngine) {
        viewModelScope.launch {
            settingsRepo.setDetectionEngine(engine)
            val ctx = getApplication<Application>()
            // Reset controller state so we don't carry over stale tracking
            // (e.g., daltonizer stuck ON from previous mode).
            GrayscaleController.resetState(ctx)
            // Start/stop appropriate service
            when (engine) {
                DetectionEngine.USAGE_STATS -> {
                    UsageStatsDetectionService.start(ctx)
                }
                DetectionEngine.ACCESSIBILITY -> {
                    UsageStatsDetectionService.stop(ctx)
                    // User must enable accessibility service manually
                }
            }
        }
    }

    fun setManualOverride(override: ManualOverride) {
        viewModelScope.launch { settingsRepo.setManualOverride(override) }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setEnabled(enabled)
            val ctx = getApplication<Application>()
            if (!enabled) {
                GrayscaleController.forceColorMode(ctx)
                UsageStatsDetectionService.stop(ctx)
            } else {
                if (detectionEngine.value == DetectionEngine.USAGE_STATS) {
                    UsageStatsDetectionService.start(ctx)
                }
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch { settingsRepo.setOnboardingComplete(true) }
    }

    fun startService() {
        val ctx = getApplication<Application>()
        when (detectionEngine.value) {
            DetectionEngine.USAGE_STATS -> {
                UsageStatsDetectionService.start(ctx)
            }
            DetectionEngine.ACCESSIBILITY -> {
                // The system rebinds enabled accessibility services automatically.
                // Ensure UsageStats isn't lingering from a previous mode.
                UsageStatsDetectionService.stop(ctx)
            }
        }
    }

    private fun isSystemApp(packageName: String): Boolean {
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (e: Exception) {
            false
        }
    }
}
