package com.divyasrikarri.migrainejournal.data.repository

import com.divyasrikarri.migrainejournal.data.local.DailyLog
import com.divyasrikarri.migrainejournal.data.local.FoodEntry
import com.divyasrikarri.migrainejournal.data.local.MigraineEntry
import com.divyasrikarri.migrainejournal.data.model.FrequencyGrouping
import com.divyasrikarri.migrainejournal.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InsightsCalculatorTest {

    private val base: LocalDate = LocalDate.of(2026, 3, 2) // a Monday

    private fun migraine(
        day: LocalDate,
        hour: Int = 9,
        painLevel: Int = 6,
        symptoms: List<String> = emptyList(),
        durationMinutes: Long? = null,
        id: Long = day.toEpochDay() * 100 + hour
    ): MigraineEntry {
        val start = DateUtils.startOfDay(day) + hour * 3_600_000L
        return MigraineEntry(
            id = id,
            startDateTime = start,
            endDateTime = durationMinutes?.let { start + it * 60_000L },
            painLevel = painLevel,
            symptoms = symptoms
        )
    }

    private fun log(day: LocalDate, sleepHours: Float? = null, stressLevel: Int? = null) =
        DailyLog(date = DateUtils.toKey(day), sleepHours = sleepHours, stressLevel = stressLevel)

    private fun food(day: LocalDate, name: String, category: String?) = FoodEntry(
        id = 0,
        date = DateUtils.toKey(day),
        foodName = name,
        mealTime = DateUtils.startOfDay(day),
        isCommonTrigger = category != null,
        triggerCategory = category
    )

    // --- Frequency buckets ------------------------------------------------------------

    @Test
    fun `weekly buckets cover the whole range including empty weeks`() {
        val migraines = listOf(migraine(base), migraine(base.plusDays(1), hour = 14))

        val buckets = InsightsCalculator.frequencyBuckets(
            migraines = migraines,
            from = base,
            toInclusive = base.plusDays(20),
            grouping = FrequencyGrouping.WEEKLY
        )

        assertEquals(3, buckets.size)
        assertEquals(listOf(2, 0, 0), buckets.map { it.count })
    }

    @Test
    fun `weekly buckets start on Monday so a Sunday attack lands in the week before`() {
        val sunday = base.plusDays(6)
        val buckets = InsightsCalculator.frequencyBuckets(
            migraines = listOf(migraine(sunday)),
            from = base,
            toInclusive = base.plusDays(13),
            grouping = FrequencyGrouping.WEEKLY
        )

        assertEquals(listOf(1, 0), buckets.map { it.count })
    }

    @Test
    fun `monthly buckets group by calendar month`() {
        val buckets = InsightsCalculator.frequencyBuckets(
            migraines = listOf(
                migraine(LocalDate.of(2026, 1, 5)),
                migraine(LocalDate.of(2026, 1, 20), hour = 11),
                migraine(LocalDate.of(2026, 3, 3))
            ),
            from = LocalDate.of(2026, 1, 1),
            toInclusive = LocalDate.of(2026, 3, 31),
            grouping = FrequencyGrouping.MONTHLY
        )

        assertEquals(3, buckets.size)
        assertEquals(listOf(2, 0, 1), buckets.map { it.count })
    }

    @Test
    fun `pain trend is ordered oldest first`() {
        val points = InsightsCalculator.painTrend(
            listOf(
                migraine(base.plusDays(2), painLevel = 9),
                migraine(base, painLevel = 3)
            )
        )

        assertEquals(listOf(3f, 9f), points.map { it.painLevel })
    }

    // --- Correlations -----------------------------------------------------------------

    @Test
    fun `sleep callout uses only migraines that have a sleep log that day`() {
        val migraines = listOf(
            migraine(base),
            migraine(base.plusDays(1)),
            migraine(base.plusDays(2)),
            migraine(base.plusDays(3)) // no daily log for this one
        )
        val logs = listOf(
            log(base, sleepHours = 4.5f),
            log(base.plusDays(1), sleepHours = 5.0f),
            log(base.plusDays(2), sleepHours = 8.0f)
        )

        val callout = InsightsCalculator.correlations(migraines, logs, emptyList())
            .first { it.headline.contains("sleep") }

        assertTrue(callout.headline.startsWith("67%"))
        assertTrue(callout.detail.contains("2 of 3"))
    }

    @Test
    fun `callouts needing a percentage are suppressed below the minimum sample`() {
        val migraines = listOf(migraine(base), migraine(base.plusDays(1)))
        val logs = listOf(log(base, sleepHours = 4f), log(base.plusDays(1), sleepHours = 4f))

        val callouts = InsightsCalculator.correlations(migraines, logs, emptyList())

        assertNull(callouts.firstOrNull { it.headline.contains("sleep") })
    }

    @Test
    fun `most logged symptom reports the count out of all migraines`() {
        val migraines = listOf(
            migraine(base, symptoms = listOf("Nausea", "Dizziness")),
            migraine(base.plusDays(1), symptoms = listOf("Nausea")),
            migraine(base.plusDays(2), symptoms = listOf("Dizziness"))
        )

        val callout = InsightsCalculator.correlations(migraines, emptyList(), emptyList())
            .first { it.headline.startsWith("Most logged symptom") }

        assertTrue(callout.headline.contains("nausea") || callout.headline.contains("dizziness"))
        assertTrue(callout.detail.contains("of 3 migraines"))
    }

    @Test
    fun `trigger food callout only counts trigger foods eaten on migraine days`() {
        val migraines = listOf(migraine(base), migraine(base.plusDays(1)))
        val foods = listOf(
            food(base, "Red wine", "Alcohol"),
            food(base.plusDays(1), "Red wine", "Alcohol"),
            // A trigger on a day with no migraine must not appear.
            food(base.plusDays(5), "Cheddar", "Aged cheese"),
            food(base, "Porridge", null)
        )

        val callout = InsightsCalculator.correlations(migraines, emptyList(), foods)
            .first { it.headline.contains("trigger") }

        assertTrue(callout.headline.contains("alcohol"))
        assertTrue(callout.detail.contains("2 of 2 days"))
        assertTrue(callout.detail.contains("not a cause"))
    }

    @Test
    fun `trigger callout is absent when nothing tagged was eaten on a migraine day`() {
        val callouts = InsightsCalculator.correlations(
            migraines = listOf(migraine(base)),
            dailyLogs = emptyList(),
            foods = listOf(food(base, "Porridge", null))
        )

        assertNull(callouts.firstOrNull { it.headline.contains("trigger") })
    }

    @Test
    fun `stress callout counts ratings of four and above`() {
        val migraines = (0..3).map { migraine(base.plusDays(it.toLong())) }
        val logs = listOf(
            log(base, stressLevel = 5),
            log(base.plusDays(1), stressLevel = 4),
            log(base.plusDays(2), stressLevel = 3),
            log(base.plusDays(3), stressLevel = 1)
        )

        val callout = InsightsCalculator.correlations(migraines, logs, emptyList())
            .first { it.headline.contains("stress") }

        assertTrue(callout.headline.startsWith("50%"))
        assertTrue(callout.detail.contains("2 of 4"))
    }

    @Test
    fun `no migraines produces no callouts at all`() {
        assertEquals(
            emptyList<Any>(),
            InsightsCalculator.correlations(emptyList(), listOf(log(base, 4f)), emptyList())
        )
    }

    // --- compute() aggregate ----------------------------------------------------------

    @Test
    fun `compute reports totals averages and duration`() {
        val migraines = listOf(
            migraine(base, painLevel = 4, durationMinutes = 60),
            migraine(base.plusDays(1), painLevel = 8, durationMinutes = 180),
            migraine(base.plusDays(2), painLevel = 6) // still ongoing
        )

        val data = InsightsCalculator.compute(
            migraines = migraines,
            dailyLogs = emptyList(),
            foods = emptyList(),
            from = base,
            toInclusive = base.plusDays(6),
            grouping = FrequencyGrouping.WEEKLY
        )

        assertEquals(3, data.totalMigraines)
        assertEquals(6f, data.averagePainLevel!!, 0.001f)
        assertEquals(120L, data.averageDurationMinutes)
        assertEquals(3, data.painTrend.size)
        assertNotNull(data.frequency.firstOrNull())
    }

    @Test
    fun `default grouping switches to monthly for long ranges`() {
        assertEquals(
            FrequencyGrouping.WEEKLY,
            InsightsCalculator.defaultGrouping(base, base.plusDays(89))
        )
        assertEquals(
            FrequencyGrouping.MONTHLY,
            InsightsCalculator.defaultGrouping(base, base.plusDays(364))
        )
    }
}
