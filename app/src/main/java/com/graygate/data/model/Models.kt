package com.graygate.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "selected_apps")
data class SelectedApp(
    @PrimaryKey val packageName: String,
    val appName: String,
    val addedAt: Long = System.currentTimeMillis()
)

enum class GrayscaleMode {
    BLACKLIST,  // Selected apps are grayscaled
    WHITELIST   // Everything EXCEPT selected apps is grayscaled
}

enum class ManualOverride {
    AUTO,           // Follow per-app rules
    FORCE_GRAY,     // Force grayscale on everything
    FORCE_COLOR     // Force color on everything
}

data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSelected: Boolean = false
)
