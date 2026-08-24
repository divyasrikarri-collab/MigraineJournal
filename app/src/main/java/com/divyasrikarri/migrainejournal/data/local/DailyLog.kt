package com.divyasrikarri.migrainejournal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per calendar day, keyed by an ISO `yyyy-MM-dd` date string. Every field is
 * nullable so a partially filled check-in is still worth saving.
 */
@Entity(tableName = "daily_logs")
data class DailyLog(
    @PrimaryKey val date: String,
    val sleepHours: Float? = null,
    val sleepQuality: Int? = null,
    val waterIntakeMl: Int? = null,
    val stressLevel: Int? = null,
    val exercised: Boolean = false,
    val exerciseType: String? = null,
    val exerciseDurationMin: Int? = null,
    val weatherPressure: Float? = null,
    val menstrualCycleDay: Int? = null,
    val screenTimeHours: Float? = null
) {
    /** True once the user has entered anything at all, used for the "check-in done" badge. */
    val hasContent: Boolean
        get() = sleepHours != null || sleepQuality != null || waterIntakeMl != null ||
            stressLevel != null || exercised || weatherPressure != null ||
            menstrualCycleDay != null || screenTimeHours != null
}
