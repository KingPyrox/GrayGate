package com.graygate.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.graygate.ui.theme.BrandDarkSlate
import com.graygate.ui.theme.GateColorSide
import com.graygate.util.PermissionUtil
import kotlinx.coroutines.launch

data class OnboardingStep(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val actionLabel: String?,
    val isComplete: () -> Boolean,
    val onAction: (() -> Unit)?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    hasUsageStats: Boolean,
    hasWriteSecure: Boolean,
    hasBatteryExempt: Boolean,
    onRefreshPermissions: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val steps = remember(hasUsageStats, hasWriteSecure, hasBatteryExempt) {
        listOf(
            OnboardingStep(
                icon = Icons.Filled.FilterBAndW,
                title = "Welcome to GrayGate",
                description = "Take control of your screen. Choose which apps appear in grayscale to reduce distractions and stay focused.\n\nSwipe through the next steps to set up permissions.",
                actionLabel = null,
                isComplete = { true },
                onAction = null
            ),
            OnboardingStep(
                icon = Icons.Filled.QueryStats,
                title = "Usage Access",
                description = "GrayGate needs usage access to detect which app is currently in the foreground. This lets us know when to switch grayscale on or off.\n\nTap below to open Settings and enable usage access for GrayGate.",
                actionLabel = if (hasUsageStats) "Granted ✓" else "Open Usage Settings",
                isComplete = { hasUsageStats },
                onAction = {
                    context.startActivity(PermissionUtil.usageAccessIntent())
                }
            ),
            OnboardingStep(
                icon = Icons.Filled.Terminal,
                title = "Enable Grayscale Control",
                description = "GrayGate needs a special permission to toggle your display's grayscale mode. This requires a one-time ADB command from a computer.\n\nConnect your phone via USB, enable USB debugging, then run the command below:",
                actionLabel = if (hasWriteSecure) "Granted ✓" else "Copy ADB Command",
                isComplete = { hasWriteSecure },
                onAction = {
                    val cmd = PermissionUtil.getAdbCommand(context)
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", cmd))
                    Toast.makeText(context, "Command copied!", Toast.LENGTH_SHORT).show()
                }
            ),
            OnboardingStep(
                icon = Icons.Filled.BatteryChargingFull,
                title = "Battery Optimization",
                description = "To keep GrayGate running reliably in the background, disable battery optimization for this app. Otherwise, your phone's power management may stop it.\n\nThis is especially important on Samsung, Xiaomi, and OnePlus devices.",
                actionLabel = if (hasBatteryExempt) "Disabled ✓" else "Disable Optimization",
                isComplete = { hasBatteryExempt },
                onAction = {
                    context.startActivity(PermissionUtil.batteryOptimizationIntent(context))
                }
            ),
            OnboardingStep(
                icon = Icons.Filled.Accessibility,
                title = "Optional: Fast Mode (Accessibility)",
                description = "GrayGate offers an OPTIONAL Fast Mode that uses Android's Accessibility Service to detect app changes with ~100ms latency.\n\nThe service ONLY reads the package name of the foreground app. It does NOT read screen content, text, images, or any personal data. No data leaves your device.\n\nFast Mode is OFF by default. You can enable it later in Settings — a full disclosure and consent prompt will appear at that time.\n\nThe app works fully without it.",
                actionLabel = null,
                isComplete = { true },
                onAction = null
            ),
            OnboardingStep(
                icon = Icons.Filled.CheckCircle,
                title = "You're All Set!",
                description = "GrayGate is ready to go. Head to the app list to choose which apps should appear in grayscale.\n\nYou can always change your settings later.",
                actionLabel = "Get Started",
                isComplete = { true },
                onAction = null // Handled separately
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { steps.size })

    // Refresh permissions when user returns from settings
    LaunchedEffect(pagerState.currentPage) {
        onRefreshPermissions()
    }

    Scaffold(
        containerColor = BrandDarkSlate
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { (pagerState.currentPage + 1).toFloat() / steps.size },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = GateColorSide,
                trackColor = Color.White.copy(alpha = 0.15f),
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val step = steps[page]
                OnboardingPage(
                    step = step,
                    isLastPage = page == steps.size - 1,
                    adbCommand = if (page == 2) PermissionUtil.getAdbCommand(context) else null,
                    onAction = {
                        if (page == steps.size - 1) {
                            onComplete()
                        } else {
                            step.onAction?.invoke()
                        }
                    }
                )
            }

            // Navigation row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    TextButton(onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    }) {
                        Text("Back", color = Color.White.copy(alpha = 0.6f))
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                // Page dots
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(steps.size) { index ->
                        val isActive = index == pagerState.currentPage
                        Surface(
                            modifier = Modifier.size(if (isActive) 10.dp else 8.dp),
                            shape = CircleShape,
                            color = if (isActive) GateColorSide
                                    else Color.White.copy(alpha = 0.3f)
                        ) {}
                    }
                }

                if (pagerState.currentPage < steps.size - 1) {
                    TextButton(onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }) {
                        Text("Next", color = Color.White)
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }
            }
        }
    }
}

@Composable
private fun OnboardingPage(
    step: OnboardingStep,
    isLastPage: Boolean,
    adbCommand: String?,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = step.icon,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = if (step.isComplete()) GateColorSide
                   else Color.White.copy(alpha = 0.5f)
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = step.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color.White
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = step.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = Color.White.copy(alpha = 0.75f)
        )

        // Show ADB command box on the ADB step
        if (adbCommand != null) {
            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = Color.Black.copy(alpha = 0.3f),
            ) {
                Text(
                    text = adbCommand,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = GateColorSide
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        if (step.actionLabel != null) {
            Button(
                onClick = onAction,
                enabled = !step.isComplete() || isLastPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = if (step.isComplete() && !isLastPage) {
                    ButtonDefaults.buttonColors(
                        disabledContainerColor = GateColorSide.copy(alpha = 0.3f),
                        disabledContentColor = Color.White.copy(alpha = 0.7f)
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = GateColorSide,
                        contentColor = BrandDarkSlate
                    )
                }
            ) {
                Text(step.actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
