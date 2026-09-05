package com.graygate.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.graygate.BuildConfig
import com.graygate.data.model.ManualOverride
import com.graygate.data.model.ThemePreference
import com.graygate.ui.theme.gateAccent
import com.graygate.util.PermissionUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themePreference: ThemePreference,
    onThemePreferenceChange: (ThemePreference) -> Unit,
    manualOverride: ManualOverride,
    hasUsageStats: Boolean,
    hasWriteSecure: Boolean,
    hasBatteryExempt: Boolean,
    hasNotifications: Boolean,
    onManualOverrideChange: (ManualOverride) -> Unit,
    onRefreshPermissions: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // Refresh permissions when screen opens
    LaunchedEffect(Unit) { onRefreshPermissions() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // --- Appearance ---
            SectionHeader("Appearance")

            val themeOptions = listOf(
                ThemePreference.SYSTEM to "System",
                ThemePreference.LIGHT to "Light",
                ThemePreference.DARK to "Dark",
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                themeOptions.forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = themePreference == value,
                        onClick = { onThemePreferenceChange(value) },
                        shape = SegmentedButtonDefaults.itemShape(index, themeOptions.size),
                        label = { Text(label) }
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            // --- Manual Override ---
            SectionHeader("Manual Override")

            val overrideOptions = listOf(
                Triple(ManualOverride.AUTO, "Auto", "Follow per-app rules"),
                Triple(ManualOverride.FORCE_GRAY, "Force Grayscale", "Grayscale everything"),
                Triple(ManualOverride.FORCE_COLOR, "Force Color", "Disable all grayscale"),
            )

            Column(Modifier.selectableGroup()) {
                overrideOptions.forEach { (value, label, desc) ->
                    // The whole row is the target, not just the 20dp circle.
                    // onClick = null on the RadioButton so the row owns the click.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = manualOverride == value,
                                onClick = { onManualOverrideChange(value) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = manualOverride == value,
                            onClick = null
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            // --- Permission Status ---
            SectionHeader("Permissions")

            PermissionStatusRow(
                label = "Usage Access",
                isGranted = hasUsageStats,
                onFix = { context.startActivity(PermissionUtil.usageAccessIntent()) }
            )

            PermissionStatusRow(
                label = "Grayscale Control (ADB)",
                isGranted = hasWriteSecure,
                onFix = {
                    val cmd = PermissionUtil.getAdbCommand(context)
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("ADB", cmd))
                    Toast.makeText(context, "ADB command copied", Toast.LENGTH_SHORT).show()
                }
            )

            if (!hasWriteSecure) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = PermissionUtil.getAdbCommand(context),
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            PermissionStatusRow(
                label = "Notifications",
                isGranted = hasNotifications,
                onFix = { context.startActivity(PermissionUtil.notificationSettingsIntent(context)) }
            )

            PermissionStatusRow(
                label = "Battery Optimization Disabled",
                isGranted = hasBatteryExempt,
                onFix = { context.startActivity(PermissionUtil.batteryOptimizationIntent(context)) }
            )

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            // --- About ---
            SectionHeader("About")

            ListItem(
                headlineContent = { Text("GrayGate v${BuildConfig.VERSION_NAME}") },
                supportingContent = { Text("Per-app grayscale for Android") },
                leadingContent = {
                    Icon(Icons.Filled.Info, contentDescription = null)
                }
            )

            ListItem(
                headlineContent = { Text("How it works") },
                supportingContent = {
                    Text(
                        "GrayGate watches the foreground app and toggles Android's system " +
                        "daltonizer to match. Grayscale covers the whole screen, status bar " +
                        "included — Android offers no true per-app filter."
                    )
                },
                leadingContent = {
                    Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null)
                }
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun PermissionStatusRow(
    label: String,
    isGranted: Boolean,
    onFix: () -> Unit
) {
    // One status indicator, not two. The leading icon carries the state; the
    // trailing slot is reserved for the action, and only appears when there
    // actually is one to take.
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = {
            Icon(
                if (isGranted) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                contentDescription = if (isGranted) "Granted" else "Not granted",
                tint = if (isGranted) gateAccent
                       else MaterialTheme.colorScheme.error
            )
        },
        trailingContent = if (isGranted) null else {
            {
                TextButton(onClick = onFix) { Text("Fix") }
            }
        }
    )
}
