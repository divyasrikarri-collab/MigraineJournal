package com.divyasrikarri.migrainejournal.ui.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divyasrikarri.migrainejournal.data.model.VolumeUnit
import com.divyasrikarri.migrainejournal.data.repository.AppSettings
import com.divyasrikarri.migrainejournal.data.repository.MigraineRepository
import com.divyasrikarri.migrainejournal.data.repository.SettingsRepository
import com.divyasrikarri.migrainejournal.export.DataExporter
import com.divyasrikarri.migrainejournal.notification.ReminderScheduler
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One-shot things the screen has to act on: launch a share sheet, show a message. */
sealed interface SettingsEvent {
    data class Share(val intent: Intent, val message: String) : SettingsEvent
    data class Message(val text: String) : SettingsEvent
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val repository: MigraineRepository,
    private val reminderScheduler: ReminderScheduler,
    private val dataExporter: DataExporter
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _exporting = MutableStateFlow(false)
    val exporting: StateFlow<Boolean> = _exporting.asStateFlow()

    private val eventChannel = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = eventChannel.receiveAsFlow()

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setReminderEnabled(enabled)
            val current = settingsRepository.current()
            if (enabled) {
                reminderScheduler.schedule(current.reminderHour, current.reminderMinute)
            } else {
                reminderScheduler.cancel()
            }
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.setReminderTime(hour, minute)
            if (settingsRepository.current().reminderEnabled) {
                reminderScheduler.schedule(hour, minute)
            }
        }
    }

    /** Re-applies the schedule after the user grants (or declines) notification permission. */
    fun onNotificationPermissionResult(granted: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationRationaleShown(true)
            if (granted) {
                val current = settingsRepository.current()
                if (current.reminderEnabled) {
                    reminderScheduler.schedule(current.reminderHour, current.reminderMinute)
                }
            } else {
                eventChannel.send(
                    SettingsEvent.Message(
                        "Reminders need notification permission. You can grant it later in " +
                            "system settings."
                    )
                )
            }
        }
    }

    fun setTrackMenstrualCycle(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setTrackMenstrualCycle(enabled) }
    }

    fun setVolumeUnit(unit: VolumeUnit) {
        viewModelScope.launch { settingsRepository.setVolumeUnit(unit) }
    }

    fun exportCsv() = export { dataExporter.exportCsv() }

    fun exportPdf() = export { dataExporter.exportPdf() }

    private fun export(block: suspend () -> com.divyasrikarri.migrainejournal.export.ExportResult) {
        if (_exporting.value) return
        viewModelScope.launch {
            _exporting.value = true
            val result = runCatching { block() }
            _exporting.value = false
            result.onSuccess {
                eventChannel.send(SettingsEvent.Share(it.intent, "Sharing ${it.fileNames.size} file(s)"))
            }.onFailure {
                eventChannel.send(SettingsEvent.Message("Export failed: ${it.message ?: "unknown error"}"))
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            eventChannel.send(SettingsEvent.Message("All entries deleted."))
        }
    }
}
