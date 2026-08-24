package com.divyasrikarri.migrainejournal.export

import com.divyasrikarri.migrainejournal.data.local.DailyLog
import com.divyasrikarri.migrainejournal.data.local.FoodEntry
import com.divyasrikarri.migrainejournal.data.local.MigraineEntry
import com.divyasrikarri.migrainejournal.util.DateUtils

/**
 * Builds raw, one-row-per-entry CSV text for each table. Pure string work so it can be
 * unit-tested without a device.
 */
object CsvExporter {

    fun migrainesCsv(entries: List<MigraineEntry>): String = buildCsv(
        header = listOf(
            "id", "start_date", "start_time", "end_date", "end_time", "duration_minutes",
            "pain_level", "pain_locations", "pain_type", "has_aura", "aura_types", "symptoms",
            "medication_name", "medication_dose", "medication_time", "medication_effectiveness",
            "notes"
        ),
        rows = entries.map { entry ->
            listOf(
                entry.id.toString(),
                DateUtils.keyOf(entry.startDateTime),
                DateUtils.formatTime(entry.startDateTime),
                entry.endDateTime?.let { DateUtils.keyOf(it) } ?: "",
                entry.endDateTime?.let { DateUtils.formatTime(it) } ?: "",
                entry.durationMinutes?.toString() ?: "",
                entry.painLevel.toString(),
                entry.painLocations.joinToString("; "),
                entry.painType,
                entry.hasAura.toString(),
                entry.auraTypes.joinToString("; "),
                entry.symptoms.joinToString("; "),
                entry.medicationName.orEmpty(),
                entry.medicationDose.orEmpty(),
                entry.medicationTime?.let { DateUtils.formatTime(it) } ?: "",
                entry.medicationEffectiveness?.toString() ?: "",
                entry.notes.orEmpty()
            )
        }
    )

    fun dailyLogsCsv(logs: List<DailyLog>): String = buildCsv(
        header = listOf(
            "date", "sleep_hours", "sleep_quality", "water_intake_ml", "stress_level",
            "exercised", "exercise_type", "exercise_duration_min", "weather_pressure",
            "menstrual_cycle_day", "screen_time_hours"
        ),
        rows = logs.map { log ->
            listOf(
                log.date,
                log.sleepHours?.toString() ?: "",
                log.sleepQuality?.toString() ?: "",
                log.waterIntakeMl?.toString() ?: "",
                log.stressLevel?.toString() ?: "",
                log.exercised.toString(),
                log.exerciseType.orEmpty(),
                log.exerciseDurationMin?.toString() ?: "",
                log.weatherPressure?.toString() ?: "",
                log.menstrualCycleDay?.toString() ?: "",
                log.screenTimeHours?.toString() ?: ""
            )
        }
    )

    fun foodEntriesCsv(entries: List<FoodEntry>): String = buildCsv(
        header = listOf("id", "date", "food_name", "meal_time", "is_common_trigger", "trigger_category"),
        rows = entries.map { entry ->
            listOf(
                entry.id.toString(),
                entry.date,
                entry.foodName,
                DateUtils.formatTime(entry.mealTime),
                entry.isCommonTrigger.toString(),
                entry.triggerCategory.orEmpty()
            )
        }
    )

    private fun buildCsv(header: List<String>, rows: List<List<String>>): String =
        buildString {
            append(header.joinToString(",") { escape(it) })
            append('\n')
            rows.forEach { row ->
                append(row.joinToString(",") { escape(it) })
                append('\n')
            }
        }

    /** RFC 4180 quoting: wrap when the value contains a comma, quote or newline. */
    internal fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
}
