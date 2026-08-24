package com.divyasrikarri.migrainejournal.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DateUtilsTest {

    private val day: LocalDate = LocalDate.of(2026, 3, 2)

    @Test
    fun `day bounds are half open`() {
        val start = DateUtils.startOfDay(day)
        val end = DateUtils.endOfDay(day)

        assertEquals(DateUtils.toKey(day), DateUtils.keyOf(start))
        assertEquals(DateUtils.toKey(day), DateUtils.keyOf(end - 1))
        assertEquals(DateUtils.toKey(day.plusDays(1)), DateUtils.keyOf(end))
    }

    @Test
    fun `keys round trip`() {
        assertEquals(day, DateUtils.parseKey(DateUtils.toKey(day)))
        assertEquals("2026-03-02", DateUtils.toKey(day))
    }

    @Test
    fun `malformed keys parse to null instead of throwing`() {
        assertNull(DateUtils.parseKeyOrNull("not-a-date"))
        assertNull(DateUtils.parseKeyOrNull(""))
    }

    @Test
    fun `durations read as hours and minutes`() {
        assertEquals("45m", DateUtils.formatDuration(45))
        assertEquals("2h", DateUtils.formatDuration(120))
        assertEquals("6h 15m", DateUtils.formatDuration(375))
        assertEquals("0m", DateUtils.formatDuration(0))
    }
}
