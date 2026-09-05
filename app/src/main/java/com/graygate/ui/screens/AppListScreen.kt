package com.graygate.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.graygate.data.model.AppInfo
import com.graygate.ui.theme.GateGraySide
import com.graygate.ui.theme.gateAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    apps: List<AppInfo>,
    searchQuery: String,
    selectedCount: Int,
    isEnabled: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onToggleApp: (AppInfo) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val accent = gateAccent

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("GrayGate", fontWeight = FontWeight.Bold)
                        Text(
                            text = "$selectedCount app${if (selectedCount != 1) "s" else ""} selected",
                            style = MaterialTheme.typography.bodySmall,
                            // No alpha: onPrimaryContainer is already the muted token for this
                            // surface, and stacking 0.7 on top dropped it to 4.32:1 light /
                            // 4.16:1 dark — under the 4.5:1 floor. Size carries the hierarchy.
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                actions = {
                    // Master enable/disable toggle
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = onEnabledChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = accent,
                            checkedTrackColor = accent.copy(alpha = 0.3f),
                            uncheckedThumbColor = GateGraySide,
                            uncheckedTrackColor = GateGraySide.copy(alpha = 0.2f),
                        ),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search apps\u2026") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                )
            )

            // First-run guidance only — once anything is selected the top bar's
            // count says the same thing, so this stops earning its space.
            if (selectedCount == 0) {
                Text(
                    text = "Pick the apps you want in grayscale",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(4.dp))

            // App list
            if (apps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No apps found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Only show the package name where the label alone is ambiguous
                // (e.g. three different apps all called "Authenticator"). Showing
                // it on every row turns the whole list into two-line noise.
                val ambiguousLabels = remember(apps) {
                    apps.groupBy { it.appName.lowercase() }
                        .filterValues { it.size > 1 }
                        .keys
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = apps,
                        key = { it.packageName }
                    ) { app ->
                        AppListItem(
                            app = app,
                            showPackageName = app.appName.lowercase() in ambiguousLabels,
                            onToggle = { onToggleApp(app) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppListItem(
    app: AppInfo,
    showPackageName: Boolean,
    onToggle: () -> Unit
) {
    val context = LocalContext.current
    val accent = gateAccent
    // Load the icon off the main thread — getApplicationIcon is a binder
    // call and toBitmap allocates; doing either during composition janks
    // fast scrolling through the list.
    val icon by produceState<ImageBitmap?>(initialValue = null, app.packageName) {
        value = withContext(Dispatchers.IO) {
            try {
                context.packageManager.getApplicationIcon(app.packageName)
                    .toBitmap(96, 96)
                    .asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }

    val grayscaleFilter = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }

    ListItem(
        headlineContent = {
            Text(
                text = app.appName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (app.isSelected) FontWeight.Medium else FontWeight.Normal
            )
        },
        supportingContent = if (showPackageName) {
            {
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // Same reason as the top bar subtitle: onSurfaceVariant is already
                    // muted; 0.7 on top put it at 3.36:1 in light theme.
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else null,
        leadingContent = {
            val iconBitmap = icon
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = app.appName,
                    modifier = Modifier.size(40.dp),
                    colorFilter = if (app.isSelected) grayscaleFilter else null
                )
            } else {
                Icon(
                    Icons.Filled.Android,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = if (app.isSelected) GateGraySide else LocalContentColor.current
                )
            }
        },
        trailingContent = {
            // onCheckedChange = null: the whole row owns the click (see toggleable
            // below), so the Switch must not also handle it or the two fight.
            Switch(
                checked = app.isSelected,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accent,
                    checkedTrackColor = accent.copy(alpha = 0.3f),
                    uncheckedThumbColor = GateGraySide,
                    uncheckedTrackColor = GateGraySide.copy(alpha = 0.2f),
                )
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = app.isSelected,
                onValueChange = { onToggle() },
                role = Role.Switch
            )
    )
}
