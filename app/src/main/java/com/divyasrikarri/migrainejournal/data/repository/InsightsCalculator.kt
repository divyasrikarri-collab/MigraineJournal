package com.divyasrikarri.migrainejournal.data.repository

import com.divyasrikarri.migrainejournal.data.local.DailyLog
import com.divyasrikarri.migrainejournal.data.local.FoodEntry
import com.divyasrikarri.migrainejournal.data.local.MigraineEntry
import com.divyasrikarri.migrainejournal.data.model.Correlation
import com.divyasrikarri.migrainejournal.data.model.FrequencyBucket
import com.divyasrikarri.migrainejournal.data.model.FrequencyGrouping
import com.divyasrikarri.migrainejournal.data.model.InsightsData
import com.divyasrikarri.migrainejournal.data.model.PainPoint
import com.divyasrikarri.migrainejournal.util.DateUtils
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Rule-based insights. Everything here is a count or an average over what the user actually
 * logged — no model, no inference. Each callout carries its own sample size so a "67%" built
 * on three entries reads as what it is.
 *
 * Pure functions with no Android dependencies, so they are unit-testable directly.
 */
object InsightsCalculator {

    /** Below this many entries a percentage is too noisy to be worth showing. */
    const val MIN_SAMPLE = 3

    private const val SHORT_SLEEP_HOURS = 6f
    private const val HIGH_STRESS_LEVEL = 4

    fun compute(
        migraines: List<MigraineEntry>,
        dailyLogs: List<DailyLog>,
        foods: List<FoodEntry>,
        from: LocalDate,
        toInclusive: LocalDate,
        grouping: FrequencyGrouping
    ): InsightsData {
        val durations = migraines.mapNotNull { it.durationMinutes }
        return InsightsData(
            frequency = frequencyBuckets(migraines, from, toInclusive, grouping),
            painTrend = painTrend(migraines),
            correlations = correlations(migraines, dailyLogs, foods),
            totalMigraines = migraines.size,
            averagePainLevel = migraines.takeIf { it.isNotEmpty() }
                ?.map { it.painLevel }?.average()?.toFloat(),
            averageDurationMinutes = durations.takeIf { it.isNotEmpty() }
                ?.average()?.roundToInt()?.toLong()
        )
    }

    // --- Charts ----------------------------------------------------------------------

    fun frequencyBuckets(
        migraines: List<MigraineEntry>,
        from: LocalDate,
        toInclusive: LocalDate,
        grouping: FrequencyGrouping
    ): List<FrequencyBucket> {
        val counts = migraines.groupingBy { entry ->
            bucketStart(DateUtils.localDateOf(entry.startDateTime), grouping)
        }.eachCount()

        val buckets = mutableListOf<FrequencyBucket>()
        var cursor = bucketStart(from, grouping)
        val end = bucketStart(toInclusive, grouping)
        while (!cursor.isAfter(end)) {
            buckets += FrequencyBucket(bucketLabel(cursor, grouping), counts[cursor] ?: 0)
            cursor = when (grouping) {
                FrequencyGrouping.WEEKLY -> cursor.plusWeeks(1)
                FrequencyGrouping.MONTHLY -> cursor.plusMonths(1)
            }
        }
        return buckets
    }

    /** Weeks start on Monday; months on the 1st. */
    private fun bucketStart(date: LocalDate, grouping: FrequencyGrouping): LocalDate =
        when (grouping) {
            FrequencyGrouping.WEEKLY -> date.minusDays((date.dayOfWeek.value - 1).toLong())
            FrequencyGrouping.MONTHLY -> date.withDayOfMonth(1)
        }

    private fun bucketLabel(start: LocalDate, grouping: FrequencyGrouping): String =
        when (grouping) {
            FrequencyGrouping.WEEKLY -> "${start.dayOfMonth} ${DateUtils.shortMonthLabel(start)}"
            FrequencyGrouping.MONTHLY -> DateUtils.shortMonthLabel(start)
        }

    /** One point per migraine, oldest first — this is a trend of attacks, not of days. */
    fun painTrend(migraines: List<MigraineEntry>): List<PainPoint> =
        migraines.sortedBy { it.startDateTime }.map { entry ->
            PainPoint(
                label = DateUtils.formatDate(entry.startDateTime),
                painLevel = entry.painLevel.toFloat()
            )
        }

    // --- Correlation callouts ---------------------------------------------------------

    fun correlations(
        migraines: List<MigraineEntry>,
        dailyLogs: List<DailyLog>,
        foods: List<FoodEntry>
    ): List<Correlation> {
        if (migraines.isEmpty()) return emptyList()

        val logsByDate = dailyLogs.associateBy { it.date }
        val migraineDates = migraines.map { DateUtils.keyOf(it.startDateTime) }
        val results = mutableListOf<Correlation>()

        sleepCallout(migraineDates, logsByDate)?.let { results += it }
        stressCallout(migraineDates, logsByDate)?.let { results += it }
        topSymptomCallout(migraines)?.let { results += it }
        topTriggerFoodCallout(migraineDates, foods)?.let { results += it }
        timeOfDayCallout(migraines)?.let { results += it }
        durationCallout(migraines)?.let { results += it }

        return results
    }

    private fun sleepCallout(
        migraineDates: List<String>,
        logsByDate: Map<String, DailyLog>
    ): Correlation? {
        val withSleep = migraineDates.mapNotNull { logsByDate[it]?.sleepHours }
        if (withSleep.size < MIN_SAMPLE) return null
        val short = withSleep.count { it < SHORT_SLEEP_HOURS }
        val percent = percent(short, withSleep.size)
        return Correlation(
            headline = "$percent% of migraines followed a night under ${SHORT_SLEEP_HOURS.toInt()} hours of sleep",
            detail = "$short of ${withSleep.size} migraines that had a sleep log recorded for the same day."
        )
    }

    private fun stressCallout(
        migraineDates: List<String>,
        logsByDate: Map<String, DailyLog>
    ): Correlation? {
        val withStress = migraineDates.mapNotNull { logsByDate[it]?.stressLevel }
        if (withStress.size < MIN_SAMPLE) return null
        val high = withStress.count { it >= HIGH_STRESS_LEVEL }
        val percent = percent(high, withStress.size)
        return Correlation(
            headline = "$percent% of migraines happened on days you rated stress $HIGH_STRESS_LEVEL or higher",
            detail = "$high of ${withStress.size} migraines that had a stress rating for the same day."
        )
    }

    private fun topSymptomCallout(migraines: List<MigraineEntry>): Correlation? {
        val counts = migraines.flatMap { it.symptoms }.groupingBy { it }.eachCount()
        val top = counts.maxByOrNull { it.value } ?: return null
        return Correlation(
            headline = "Most logged symptom: ${top.key.lowercase(Locale.getDefault())}",
            detail = "Recorded in ${top.value} of ${migraines.size} migraines."
        )
    }

    private fun topTriggerFoodCallout(
        migraineDates: List<String>,
        foods: List<FoodEntry>
    ): Correlation? {
        val migraineDateSet = migraineDates.toSet()
        val triggersOnMigraineDays = foods.filter {
            it.isCommonTrigger && it.date in migraineDateSet
        }
        if (triggersOnMigraineDays.isEmpty()) return null
        val top = triggersOnMigraineDays
            .groupingBy { it.triggerCategory ?: it.foodName }
            .eachCount()
            .maxByOrNull { it.value } ?: return null
        val daysWithThat = triggersOnMigraineDays
            .filter { (it.triggerCategory ?: it.foodName) == top.key }
            .map { it.date }
            .distinct()
            .size
        return Correlation(
            headline = "Most frequently tagged trigger on migraine days: ${top.key.lowercase(Locale.getDefault())}",
            detail = "Logged on $daysWithThat of ${migraineDateSet.size} days with a migraine. " +
                "This is a co-occurrence count, not a cause."
        )
    }

    private fun timeOfDayCallout(migraines: List<MigraineEntry>): Correlation? {
        if (migraines.size < MIN_SAMPLE) return null
        val buckets = migraines.groupingBy { entry ->
            when (DateUtils.localDateTimeOf(entry.startDateTime).hour) {
                in 5..11 -> "morning"
                in 12..16 -> "afternoon"
                in 17..21 -> "evening"
                else -> "night"
            }
        }.eachCount()
        val top = buckets.maxByOrNull { it.value } ?: return null
        return Correlation(
            headline = "Migraines most often start in the ${top.key}",
            detail = "${top.value} of ${migraines.size} migraines started then."
        )
    }

    private fun durationCallout(migraines: List<MigraineEntry>): Correlation? {
        val durations = migraines.mapNotNull { it.durationMinutes }
        if (durations.size < MIN_SAMPLE) return null
        val average = durations.average().roundToInt().toLong()
        return Correlation(
            headline = "Average migraine lasts ${DateUtils.formatDuration(average)}",
            detail = "Across ${durations.size} migraines with both a start and an end time."
        )
    }

    private fun percent(part: Int, total: Int): Int =
        if (total == 0) 0 else ((part * 100f) / total).roundToInt()

    /** Inclusive day span, used to pick a sensible default grouping for a range. */
    fun defaultGrouping(from: LocalDate, toInclusive: LocalDate): FrequencyGrouping =
        if (ChronoUnit.DAYS.between(from, toInclusive) > 120) {
            FrequencyGrouping.MONTHLY
        } else {
            FrequencyGrouping.WEEKLY
        }
}
