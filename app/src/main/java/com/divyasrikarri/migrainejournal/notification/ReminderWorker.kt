package com.divyasrikarri.migrainejournal.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.divyasrikarri.migrainejournal.data.repository.SettingsRepository

/**
 * Fires the daily check-in reminder. Re-reads the setting before posting so a reminder
 * turned off between scheduling and firing stays silent.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = SettingsRepository(applicationContext).current()
        if (settings.reminderEnabled) {
            NotificationHelper.showCheckInReminder(applicationContext)
        }
        return Result.success()
    }
}
