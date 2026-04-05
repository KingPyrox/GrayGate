package com.graygate.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.graygate.data.model.DetectionEngine
import com.graygate.data.model.ManualOverride
import com.graygate.util.PermissionUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    detectionEngine: DetectionEngine,
    manualOverride: ManualOverride,
    hasUsageStats: Boolean,
    hasWriteSecure: Boolean,
    hasBatteryExempt: Boolean,
    onDetectionEngineChange: (DetectionEngine) -> Unit,
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
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // --- Detection Engine ---
            SectionHeader("Detection Mode")

            DetectionOptionCard(
                title = "Standard (Usage Stats)",
                description = "Polls every 1.5 seconds. Slower (~1–3s delay) but doesn't require an accessibility service. Recommended for most users.",
                isSelected = detectionEngine == DetectionEngine.USAGE_STATS,
                onClick = { onDetectionEngineChange(DetectionEngine.USAGE_STATS) }
            )

            DetectionOptionCard(
                title = "Fast (Accessibility)",
                description = "Event-driven detection with ~100ms latency. Requires enabling GrayGate in Settings > Accessibility.",
                isSelected = detectionEngine == DetectionEngine.ACCESSIBILITY,
                onClick = { onDetectionEngineChange(DetectionEngine.ACCESSIBILITY) }
            )

            if (detectionEngine == DetectionEngine.ACCESSIBILITY) {
                TextButton(
                    onClick = { context.startActivity(PermissionUtil.accessibilityIntent()) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Filled.Accessibility, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Open Accessibility Settings")
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

            Column(Modifier.padding(horizontal = 16.dp)) {
                overrideOptions.forEach { (value, label, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = manualOverride == value,
                            onClick = { onManualOverrideChange(value) }
                        )
                        Spacer(Modifier.width(8.dp))
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
                label = "Battery Optimization Disabled",
                isGranted = hasBatteryExempt,
                onFix = { context.startActivity(PermissionUtil.batteryOptimizationIntent(context)) }
            )

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            // --- About ---
            SectionHeader("About")

            ListItem(
                headlineContent = { Text("GrayGate v1.0.0") },
                supportingContent = { Text("Per-app grayscale for Android") },
                leadingContent = {
                    Icon(Icons.Filled.Info, contentDescription = null)
                }
            )

            ListItem(
                headlineContent = { Text("How it works") },
                supportingContent = {
                    Text(
                        "GrayGate toggles Android's system-wide daltonizer as you switch between apps, " +
                        "creating the effect of per-app grayscale. The entire screen (including status bar) " +
                        "will be affected when a selected app is in the foreground."
                    )
                },
                leadingContent = {
                    Icon(Icons.Filled.Help, contentDescription = null)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetectionOptionCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder() else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = isSelected, onClick = onClick)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(description, style = MaterialTheme.typography.bodySmall,
                     color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    label: String,
    isGranted: Boolean,
    onFix: () -> Unit
) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = {
            if (isGranted) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                TextButton(onClick = onFix) {
                    Text("Fix")
                }
            }
        },
        leadingContent = {
            Icon(
                if (isGranted) Icons.Filled.VerifiedUser else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (isGranted) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.error
            )
        }
    )
}
