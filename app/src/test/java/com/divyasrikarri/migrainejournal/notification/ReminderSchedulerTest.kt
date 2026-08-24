package com.divyasrikarri.migrainejournal.notification

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class ReminderSchedulerTest {

    @Test
    fun `delays to later the same day when the time has not passed`() {
        val now = LocalDateTime.of(2026, 3, 4, 14, 0)
        val delay = ReminderScheduler.delayUntilNext(20, 0, now)
        assertEquals(6 * 60, delay.toMinutes())
    }

    @Test
    fun `rolls over to tomorrow when the time has already passed`() {
        val now = LocalDateTime.of(2026, 3, 4, 21, 30)
        val delay = ReminderScheduler.delayUntilNext(20, 0, now)
        assertEquals(22 * 60 + 30, delay.toMinutes())
    }

    @Test
    fun `rolls over rather than firing instantly at exactly the reminder time`() {
        val now = LocalDateTime.of(2026, 3, 4, 20, 0)
        val delay = ReminderScheduler.delayUntilNext(20, 0, now)
        assertEquals(24 * 60, delay.toMinutes())
    }
}
