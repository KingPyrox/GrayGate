package com.graygate.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.graygate.R
import com.graygate.controller.GrayscaleController
import com.graygate.data.model.ManualOverride
import com.graygate.data.repository.SettingsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * Quick Settings tile that cycles through:
 *   Auto → Force Grayscale → Force Color → Auto
 *
 * Provides a fast way to override per-app behavior without opening the app.
 */
class GrayscaleTileService : TileService() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val repo = SettingsRepository(applicationContext)
            val current = repo.manualOverride.first()

            val next = when (current) {
                ManualOverride.AUTO -> ManualOverride.FORCE_GRAY
                ManualOverride.FORCE_GRAY -> ManualOverride.FORCE_COLOR
                ManualOverride.FORCE_COLOR -> ManualOverride.AUTO
            }

            repo.setManualOverride(next)
            GrayscaleController.override = next

            // Apply immediately
            when (next) {
                ManualOverride.FORCE_GRAY -> {
                    GrayscaleController.override = ManualOverride.FORCE_GRAY
                    GrayscaleController.onForegroundAppChanged("__force__", applicationContext)
                }
                ManualOverride.FORCE_COLOR -> {
                    GrayscaleController.forceColorMode(applicationContext)
                    GrayscaleController.override = ManualOverride.FORCE_COLOR
                }
                ManualOverride.AUTO -> {
                    GrayscaleController.override = ManualOverride.AUTO
                }
            }

            updateTileState()
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        scope.launch {
            val repo = SettingsRepository(applicationContext)
            val override = repo.manualOverride.first()

            tile.state = when (override) {
                ManualOverride.AUTO -> Tile.STATE_INACTIVE
                ManualOverride.FORCE_GRAY -> Tile.STATE_ACTIVE
                ManualOverride.FORCE_COLOR -> Tile.STATE_INACTIVE
            }

            tile.label = when (override) {
                ManualOverride.AUTO -> getString(R.string.tile_auto)
                ManualOverride.FORCE_GRAY -> getString(R.string.tile_force_gray)
                ManualOverride.FORCE_COLOR -> getString(R.string.tile_force_color)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = when (override) {
                    ManualOverride.AUTO -> "Per-app"
                    ManualOverride.FORCE_GRAY -> "All apps"
                    ManualOverride.FORCE_COLOR -> "Disabled"
                }
            }

            tile.updateTile()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
