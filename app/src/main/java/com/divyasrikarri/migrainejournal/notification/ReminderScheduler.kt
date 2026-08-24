package com.divyasrikarri.migrainejournal.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {

    /**
     * Schedules (or reschedules) the daily reminder for [hour]:[minute]. Uses a 24 hour
     * periodic request with an initial delay to the next occurrence of that time —
     * WorkManager's own drift is acceptable for a nightly nudge.
     */
    fun schedule(hour: Int, minute: Int) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayUntilNext(hour, minute).toMillis(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    companion object {
        const val WORK_NAME = "daily_checkin_reminder"

        internal fun delayUntilNext(
            hour: Int,
            minute: Int,
            now: LocalDateTime = LocalDateTime.now()
        ): Duration {
            var next = now.with(LocalTime.of(hour, minute))
            if (!next.isAfter(now)) next = next.plusDays(1)
            return Duration.between(now, next)
        }
    }
}
