package com.divyasrikarri.migrainejournal

import android.content.Context
import com.divyasrikarri.migrainejournal.data.local.MigraineDatabase
import com.divyasrikarri.migrainejournal.data.repository.MigraineRepository
import com.divyasrikarri.migrainejournal.data.repository.SettingsRepository
import com.divyasrikarri.migrainejournal.export.DataExporter
import com.divyasrikarri.migrainejournal.notification.ReminderScheduler

/**
 * Hand-rolled dependency container. The graph is small enough that a DI framework would be
 * more machinery than the app needs.
 */
class AppContainer(private val context: Context) {

    private val database: MigraineDatabase by lazy { MigraineDatabase.getInstance(context) }

    val repository: MigraineRepository by lazy {
        MigraineRepository(
            migraineDao = database.migraineDao(),
            dailyLogDao = database.dailyLogDao(),
            foodEntryDao = database.foodEntryDao()
        )
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(context) }

    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(context) }

    val dataExporter: DataExporter by lazy { DataExporter(context, repository) }
}
