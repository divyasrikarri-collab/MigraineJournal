package com.divyasrikarri.migrainejournal.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val INSIGHTS = "insights"
    const val SETTINGS = "settings"

    const val ARG_ENTRY_ID = "entryId"
    const val ARG_DATE = "date"

    private const val LOG_MIGRAINE_BASE = "log_migraine"
    const val LOG_MIGRAINE = "$LOG_MIGRAINE_BASE?$ARG_ENTRY_ID={$ARG_ENTRY_ID}"

    private const val CHECK_IN_BASE = "check_in"
    const val CHECK_IN = "$CHECK_IN_BASE?$ARG_DATE={$ARG_DATE}"

    /** [entryId] of 0 starts a new migraine; anything else edits that entry. */
    fun logMigraine(entryId: Long = 0L) = "$LOG_MIGRAINE_BASE?$ARG_ENTRY_ID=$entryId"

    /** A null [dateKey] means today. */
    fun checkIn(dateKey: String? = null) =
        if (dateKey == null) CHECK_IN_BASE else "$CHECK_IN_BASE?$ARG_DATE=$dateKey"
}

enum class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    Home(Routes.HOME, "Home", Icons.Filled.Home),
    History(Routes.HISTORY, "History", Icons.Filled.CalendarMonth),
    Insights(Routes.INSIGHTS, "Insights", Icons.Filled.Insights),
    Settings(Routes.SETTINGS, "Settings", Icons.Filled.Settings)
}
