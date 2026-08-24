package com.divyasrikarri.migrainejournal.export

import com.divyasrikarri.migrainejournal.data.local.DailyLog
import com.divyasrikarri.migrainejournal.data.local.MigraineEntry
import com.divyasrikarri.migrainejournal.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CsvExporterTest {

    private val day: LocalDate = LocalDate.of(2026, 3, 2)
    private val start = DateUtils.startOfDay(day) + 9 * 3_600_000L

    @Test
    fun `escapes commas quotes and newlines`() {
        assertEquals("plain", CsvExporter.escape("plain"))
        assertEquals("\"a,b\"", CsvExporter.escape("a,b"))
        assertEquals("\"he said \"\"ow\"\"\"", CsvExporter.escape("he said \"ow\""))
        assertEquals("\"line1\nline2\"", CsvExporter.escape("line1\nline2"))
    }

    @Test
    fun `migraine csv has a header and one row per entry`() {
        val csv = CsvExporter.migrainesCsv(
            listOf(
                MigraineEntry(id = 1, startDateTime = start, painLevel = 7),
                MigraineEntry(
                    id = 2,
                    startDateTime = start,
                    endDateTime = start + 90 * 60_000L,
                    painLevel = 3
                )
            )
        )
        val lines = csv.trim().lines()

        assertEquals(3, lines.size)
        assertTrue(lines.first().startsWith("id,start_date,start_time"))
        assertTrue(lines[1].contains(DateUtils.toKey(day)))
        // Ongoing entries leave duration blank; closed ones report minutes.
        assertTrue(lines[2].contains(",90,"))
    }

    @Test
    fun `list columns are joined with semicolons so the comma stays the delimiter`() {
        val csv = CsvExporter.migrainesCsv(
            listOf(
                MigraineEntry(
                    id = 1,
                    startDateTime = start,
                    painLevel = 5,
                    symptoms = listOf("Nausea", "Dizziness")
                )
            )
        )

        assertTrue(csv.contains("Nausea; Dizziness"))
    }

    @Test
    fun `notes containing a comma are quoted so the column count stays stable`() {
        val csv = CsvExporter.migrainesCsv(
            listOf(
                MigraineEntry(
                    id = 1,
                    startDateTime = start,
                    painLevel = 5,
                    notes = "Bad one, lasted all evening"
                )
            )
        )

        assertTrue(csv.contains("\"Bad one, lasted all evening\""))
    }

    @Test
    fun `daily log csv leaves unset fields empty rather than writing null`() {
        val csv = CsvExporter.dailyLogsCsv(
            listOf(DailyLog(date = "2026-03-02", sleepHours = 6.5f))
        )
        val row = csv.trim().lines()[1]

        assertTrue(row.startsWith("2026-03-02,6.5,"))
        assertTrue(!row.contains("null"))
    }

    @Test
    fun `an empty table still emits its header`() {
        assertEquals(1, CsvExporter.dailyLogsCsv(emptyList()).trim().lines().size)
    }
}
