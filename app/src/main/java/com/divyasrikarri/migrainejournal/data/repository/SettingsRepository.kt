package com.divyasrikarri.migrainejournal.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.divyasrikarri.migrainejournal.data.model.VolumeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("settings")

data class AppSettings(
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val trackMenstrualCycle: Boolean = false,
    val volumeUnit: VolumeUnit = VolumeUnit.MILLILITRES,
    /** True once the user has been shown the notification-permission rationale. */
    val notificationRationaleShown: Boolean = false
)

class SettingsRepository(private val context: Context) {

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            reminderEnabled = prefs[Keys.REMINDER_ENABLED] ?: true,
            reminderHour = prefs[Keys.REMINDER_HOUR] ?: 20,
            reminderMinute = prefs[Keys.REMINDER_MINUTE] ?: 0,
            trackMenstrualCycle = prefs[Keys.TRACK_CYCLE] ?: false,
            volumeUnit = VolumeUnit.fromName(prefs[Keys.VOLUME_UNIT]),
            notificationRationaleShown = prefs[Keys.RATIONALE_SHOWN] ?: false
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setReminderEnabled(enabled: Boolean) = edit { it[Keys.REMINDER_ENABLED] = enabled }

    suspend fun setReminderTime(hour: Int, minute: Int) = edit {
        it[Keys.REMINDER_HOUR] = hour
        it[Keys.REMINDER_MINUTE] = minute
    }

    suspend fun setTrackMenstrualCycle(enabled: Boolean) = edit { it[Keys.TRACK_CYCLE] = enabled }

    suspend fun setVolumeUnit(unit: VolumeUnit) = edit { it[Keys.VOLUME_UNIT] = unit.name }

    suspend fun setNotificationRationaleShown(shown: Boolean) =
        edit { it[Keys.RATIONALE_SHOWN] = shown }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.settingsDataStore.edit(block)
    }

    private object Keys {
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val TRACK_CYCLE = booleanPreferencesKey("track_menstrual_cycle")
        val VOLUME_UNIT = stringPreferencesKey("volume_unit")
        val RATIONALE_SHOWN = booleanPreferencesKey("notification_rationale_shown")
    }
}
