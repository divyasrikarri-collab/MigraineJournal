package com.divyasrikarri.migrainejournal.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Conversions between the two time representations in the schema: epoch millis for point
 * events (migraine start/end, meal times) and `yyyy-MM-dd` strings for whole-day rows.
 *
 * Everything resolves against the device's current zone, which is what a user comparing
 * "the night I slept badly" to "the morning it started" expects.
 */
object DateUtils {

    val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.getDefault())

    fun zone(): ZoneId = ZoneId.systemDefault()

    fun today(): LocalDate = LocalDate.now(zone())

    fun todayKey(): String = today().format(ISO_DATE)

    fun toKey(date: LocalDate): String = date.format(ISO_DATE)

    fun parseKey(key: String): LocalDate = LocalDate.parse(key, ISO_DATE)

    fun parseKeyOrNull(key: String): LocalDate? = runCatching { parseKey(key) }.getOrNull()

    /** The `yyyy-MM-dd` key of the day [epochMillis] falls on. */
    fun keyOf(epochMillis: Long): String = toKey(localDateOf(epochMillis))

    fun localDateOf(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone()).toLocalDate()

    fun localDateTimeOf(epochMillis: Long): LocalDateTime =
        Instant.ofEpochMilli(epochMillis).atZone(zone()).toLocalDateTime()

    fun toEpochMillis(dateTime: LocalDateTime): Long =
        dateTime.atZone(zone()).toInstant().toEpochMilli()

    /** Midnight at the start of [date], in epoch millis. */
    fun startOfDay(date: LocalDate): Long =
        date.atStartOfDay(zone()).toInstant().toEpochMilli()

    /** Midnight at the start of the *following* day — the exclusive end of [date]. */
    fun endOfDay(date: LocalDate): Long = startOfDay(date.plusDays(1))

    fun formatTime(epochMillis: Long): String = localDateTimeOf(epochMillis).format(timeFormatter)

    fun formatDate(epochMillis: Long): String = localDateTimeOf(epochMillis).format(dateFormatter)

    fun formatDate(date: LocalDate): String = date.format(dateFormatter)

    fun formatDateTime(epochMillis: Long): String =
        localDateTimeOf(epochMillis).format(dateTimeFormatter)

    fun formatTimeOfDay(hour: Int, minute: Int): String =
        String.format(Locale.getDefault(), "%02d:%02d", hour, minute)

    fun monthLabel(date: LocalDate): String =
        "${date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${date.year}"

    fun shortMonthLabel(date: LocalDate): String =
        date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())

    /** Human duration for a migraine, e.g. `6h 15m`. */
    fun formatDuration(minutes: Long): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }
}
