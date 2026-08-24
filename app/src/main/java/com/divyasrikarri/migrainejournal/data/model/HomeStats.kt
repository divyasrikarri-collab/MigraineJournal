package com.divyasrikarri.migrainejournal.data.model

/** Quick-stat block on the dashboard. */
data class HomeStats(
    val migrainesThisMonth: Int = 0,
    val averagePainLevel: Float? = null,
    /** Days since the last migraine started; null when nothing has ever been logged. */
    val currentStreakDays: Int? = null
)
