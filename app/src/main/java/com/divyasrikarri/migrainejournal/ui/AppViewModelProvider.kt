package com.divyasrikarri.migrainejournal.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.divyasrikarri.migrainejournal.MigraineApplication
import com.divyasrikarri.migrainejournal.ui.dailycheckin.DailyCheckInViewModel
import com.divyasrikarri.migrainejournal.ui.history.HistoryViewModel
import com.divyasrikarri.migrainejournal.ui.home.HomeViewModel
import com.divyasrikarri.migrainejournal.ui.insights.InsightsViewModel
import com.divyasrikarri.migrainejournal.ui.logmigraine.LogMigraineViewModel
import com.divyasrikarri.migrainejournal.ui.settings.SettingsViewModel

/**
 * ViewModel factories. Screen arguments (entry id, date) are passed explicitly rather than
 * read back out of a SavedStateHandle, so each factory reads as a plain constructor call.
 */
object AppViewModelProvider {

    val Factory = viewModelFactory {
        initializer { HomeViewModel(app().container.repository) }
        initializer {
            val container = app().container
            HistoryViewModel(container.repository, container.settingsRepository)
        }
        initializer { InsightsViewModel(app().container.repository) }
        initializer {
            val container = app().container
            SettingsViewModel(
                settingsRepository = container.settingsRepository,
                repository = container.repository,
                reminderScheduler = container.reminderScheduler,
                dataExporter = container.dataExporter
            )
        }
    }

    fun logMigraineFactory(entryId: Long) = viewModelFactory {
        initializer { LogMigraineViewModel(app().container.repository, entryId) }
    }

    fun dailyCheckInFactory(dateKey: String?) = viewModelFactory {
        initializer {
            val container = app().container
            DailyCheckInViewModel(
                repository = container.repository,
                settingsRepository = container.settingsRepository,
                dateKey = dateKey
            )
        }
    }

    private fun CreationExtras.app(): MigraineApplication =
        this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MigraineApplication
}
