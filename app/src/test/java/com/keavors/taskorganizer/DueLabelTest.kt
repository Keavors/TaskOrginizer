package com.keavors.taskorganizer

import com.keavors.taskorganizer.ui.Due
import com.keavors.taskorganizer.ui.DueTone
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class DueLabelTest {
    private val now = LocalDateTime.parse("2026-11-02T12:00")

    @Test fun dateOnly() {
        assertEquals("Сегодня" to DueTone.TODAY, Due.label("2026-11-02", null, now))
        assertEquals("Завтра" to DueTone.SOON, Due.label("2026-11-03", null, now))
        assertEquals("Вчера" to DueTone.OVERDUE, Due.label("2026-11-01", null, now))
    }

    @Test fun withTime() {
        assertEquals("Сегодня, 14:00" to DueTone.TODAY, Due.label("2026-11-02", "14:00", now))
        assertEquals("Сегодня, 9:30" to DueTone.OVERDUE, Due.label("2026-11-02", "09:30", now))
        assertEquals("Завтра, 8:00" to DueTone.SOON, Due.label("2026-11-03", "08:00", now))
        assertEquals("9:05", Due.time("09:05"))
    }
}
