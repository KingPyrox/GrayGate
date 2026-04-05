package com.graygate.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.graygate.data.model.DetectionEngine
import com.graygate.data.model.GrayscaleMode
import com.graygate.data.model.ManualOverride
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "graygate_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private val KEY_MODE = stringPreferencesKey("grayscale_mode")
        private val KEY_DETECTION = stringPreferencesKey("detection_engine")
        private val KEY_OVERRIDE = stringPreferencesKey("manual_override")
        private val KEY_ENABLED = booleanPreferencesKey("is_enabled")
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_complete")
    }

    val grayscaleMode: Flow<GrayscaleMode> = context.dataStore.data.map { prefs ->
        try {
            GrayscaleMode.valueOf(prefs[KEY_MODE] ?: GrayscaleMode.BLACKLIST.name)
        } catch (e: Exception) {
            GrayscaleMode.BLACKLIST
        }
    }

    val detectionEngine: Flow<DetectionEngine> = context.dataStore.data.map { prefs ->
        try {
            DetectionEngine.valueOf(prefs[KEY_DETECTION] ?: DetectionEngine.USAGE_STATS.name)
        } catch (e: Exception) {
            DetectionEngine.USAGE_STATS
        }
    }

    val manualOverride: Flow<ManualOverride> = context.dataStore.data.map { prefs ->
        try {
            ManualOverride.valueOf(prefs[KEY_OVERRIDE] ?: ManualOverride.AUTO.name)
        } catch (e: Exception) {
            ManualOverride.AUTO
        }
    }

    val isEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ENABLED] ?: true
    }

    val isOnboardingComplete: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_DONE] ?: false
    }

    suspend fun setGrayscaleMode(mode: GrayscaleMode) {
        context.dataStore.edit { it[KEY_MODE] = mode.name }
    }

    suspend fun setDetectionEngine(engine: DetectionEngine) {
        context.dataStore.edit { it[KEY_DETECTION] = engine.name }
    }

    suspend fun setManualOverride(override: ManualOverride) {
        context.dataStore.edit { it[KEY_OVERRIDE] = override.name }
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ENABLED] = enabled }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_DONE] = complete }
    }
}
