package com.graygate.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
}

@Composable
fun GrayGateApp(viewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()

    val isOnboardingComplete by viewModel.isOnboardingComplete.collectAsState()
    val apps by viewModel.appList.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCount by viewModel.selectedCount.collectAsState()
    val grayscaleMode by viewModel.grayscaleMode.collectAsState()
    val manualOverride by viewModel.manualOverride.collectAsState()
    val isEnabled by viewModel.isEnabled.collectAsState()
    val hasUsageStats by viewModel.hasUsageStats.collectAsState()
    val hasWriteSecure by viewModel.hasWriteSecure.collectAsState()
    val hasBatteryExempt by viewModel.hasBatteryExempt.collectAsState()
    val hasNotifications by viewModel.hasNotifications.collectAsState()

    // Permission states can change while the user is away in system Settings —
    // re-check them every time the activity returns to the foreground.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Wait for DataStore before choosing the start destination — otherwise
    // returning users see the onboarding flow flash before the real value
    // arrives.
    val onboardingComplete = isOnboardingComplete ?: return

    val startDestination = if (onboardingComplete) "app_list" else "onboarding"

    // Ensure the detection service is running whenever the app is launched
    // and onboarding is complete. Settings persist via DataStore, but the
    // service itself needs to be (re)started after process death or reboot.
    LaunchedEffect(onboardingComplete, isEnabled) {
        if (onboardingComplete && isEnabled) {
            viewModel.startService()
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable("onboarding") {
            OnboardingScreen(
                hasUsageStats = hasUsageStats,
                hasWriteSecure = hasWriteSecure,
                hasBatteryExempt = hasBatteryExempt,
                hasNotifications = hasNotifications,
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
                manualOverride = manualOverride,
                hasUsageStats = hasUsageStats,
                hasWriteSecure = hasWriteSecure,
                hasBatteryExempt = hasBatteryExempt,
                hasNotifications = hasNotifications,
                onManualOverrideChange = { viewModel.setManualOverride(it) },
                onRefreshPermissions = { viewModel.refreshPermissions() },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
