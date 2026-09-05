package com.graygate.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.graygate.data.model.ManualOverride
import com.graygate.data.model.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "graygate_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_preference")
        private val KEY_OVERRIDE = stringPreferencesKey("manual_override")
        private val KEY_ENABLED = booleanPreferencesKey("is_enabled")
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_complete")
    }

    // Fall back to defaults if the DataStore file can't be read — a corrupt
    // file must not kill collectors (the app-wide settings sync depends on
    // these flows staying alive).
    private val prefsFlow: Flow<Preferences> = context.dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val themePreference: Flow<ThemePreference> = prefsFlow.map { prefs ->
        try {
            ThemePreference.valueOf(prefs[KEY_THEME] ?: ThemePreference.SYSTEM.name)
        } catch (e: Exception) {
            ThemePreference.SYSTEM
        }
    }

    val manualOverride: Flow<ManualOverride> = prefsFlow.map { prefs ->
        try {
            ManualOverride.valueOf(prefs[KEY_OVERRIDE] ?: ManualOverride.AUTO.name)
        } catch (e: Exception) {
            ManualOverride.AUTO
        }
    }

    val isEnabled: Flow<Boolean> = prefsFlow.map { prefs ->
        prefs[KEY_ENABLED] ?: true
    }

    val isOnboardingComplete: Flow<Boolean> = prefsFlow.map { prefs ->
        prefs[KEY_ONBOARDING_DONE] ?: false
    }

    suspend fun setThemePreference(theme: ThemePreference) {
        context.dataStore.edit { it[KEY_THEME] = theme.name }
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
