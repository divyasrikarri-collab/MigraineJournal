package com.divyasrikarri.migrainejournal

import android.app.Application
import com.divyasrikarri.migrainejournal.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MigraineApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannel(this)
        syncReminderSchedule()
    }

    /**
     * Reconciles WorkManager with the stored preference on every launch. `enqueueUniquePeriodicWork`
     * with UPDATE makes this idempotent, and it covers the first run, where the reminder is on by
     * default but nothing has scheduled it yet.
     */
    private fun syncReminderSchedule() {
        applicationScope.launch {
            val settings = container.settingsRepository.current()
            if (settings.reminderEnabled) {
                container.reminderScheduler.schedule(settings.reminderHour, settings.reminderMinute)
            } else {
                container.reminderScheduler.cancel()
            }
        }
    }
}
