package com.graygate.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.graygate.ui.screens.AppListScreen
import com.graygate.ui.screens.OnboardingScreen
import com.graygate.ui.screens.SettingsScreen
import com.graygate.ui.theme.GrayGateTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GrayGateTheme {
                GrayGateApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Permissions may have changed while we were in Settings
        // The ViewModel will refresh when the composable re-collects
    }
}

@Composable
fun GrayGateApp(viewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()

    val isOnboardingComplete by viewModel.isOnboardingComplete.collectAsState()
    val apps by viewModel.appList.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCount by viewModel.selectedCount.collectAsState()
    val grayscaleMode by viewModel.grayscaleMode.collectAsState()
    val detectionEngine by viewModel.detectionEngine.collectAsState()
    val manualOverride by viewModel.manualOverride.collectAsState()
    val isEnabled by viewModel.isEnabled.collectAsState()
    val hasUsageStats by viewModel.hasUsageStats.collectAsState()
    val hasWriteSecure by viewModel.hasWriteSecure.collectAsState()
    val hasBatteryExempt by viewModel.hasBatteryExempt.collectAsState()

    val startDestination = if (isOnboardingComplete) "app_list" else "onboarding"

    // Ensure the detection service is running whenever the app is launched
    // and onboarding is complete. Settings persist via DataStore, but the
    // service itself needs to be (re)started after process death or reboot.
    LaunchedEffect(isOnboardingComplete, detectionEngine, isEnabled) {
        if (isOnboardingComplete && isEnabled) {
            viewModel.startService()
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable("onboarding") {
            OnboardingScreen(
                hasUsageStats = hasUsageStats,
                hasWriteSecure = hasWriteSecure,
                hasBatteryExempt = hasBatteryExempt,
                onRefreshPermissions = { viewModel.refreshPermissions() },
                onComplete = {
                    viewModel.completeOnboarding()
                    viewModel.startService()
                    navController.navigate("app_list") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("app_list") {
            // Refresh permissions whenever we return to this screen
            LaunchedEffect(Unit) { viewModel.refreshPermissions() }

            AppListScreen(
                apps = apps,
                searchQuery = searchQuery,
                selectedCount = selectedCount,
                grayscaleMode = grayscaleMode,
                isEnabled = isEnabled,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                onToggleApp = { viewModel.toggleAppSelection(it) },
                onModeChange = { viewModel.setGrayscaleMode(it) },
                onEnabledChange = { viewModel.setEnabled(it) },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }

        composable("settings") {
            SettingsScreen(
                detectionEngine = detectionEngine,
                manualOverride = manualOverride,
                hasUsageStats = hasUsageStats,
                hasWriteSecure = hasWriteSecure,
                hasBatteryExempt = hasBatteryExempt,
                onDetectionEngineChange = { viewModel.setDetectionEngine(it) },
                onManualOverrideChange = { viewModel.setManualOverride(it) },
                onRefreshPermissions = { viewModel.refreshPermissions() },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
