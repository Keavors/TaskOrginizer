package com.keavors.taskorganizer

import com.keavors.taskorganizer.data.AppData
import com.keavors.taskorganizer.data.Item
import com.keavors.taskorganizer.data.Priority
import com.keavors.taskorganizer.data.ReminderSettings
import com.keavors.taskorganizer.data.createTab
import com.keavors.taskorganizer.reminders.ReminderPlanner
import com.keavors.taskorganizer.reminders.ReminderText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderPlannerTest {
    private val zone = ZoneId.of("Europe/Moscow")
    private val tab = createTab("Задачи", now = 0)

    private fun data(vararg items: Item) = AppData(tabs = listOf(tab), items = mapOf(tab.id to items.toList()), activeTabId = tab.id)

    private fun item(id: String = "a", date: String? = "2026-11-02", time: String? = "14:00", done: Boolean = false, priority: Priority = Priority.NORMAL) =
        Item(id = id, text = "Позвонить", done = done, priority = priority, dueDate = date, dueTime = time)

    private fun at(time: String, date: String = "2026-11-02") = LocalDateTime.parse("${date}T$time").atZone(zone).toInstant().toEpochMilli()

    private fun targets(s: ReminderSettings, vararg items: Item) = ReminderPlanner.targets(data(*items), s, zone)

    @Test fun remindsBeforeAndAtTheDueTime() {
        val s = ReminderSettings(offsets = listOf(0, 15))
        val t = targets(s, item())
        assertEquals(at("13:45"), ReminderPlanner.next(t, s, emptyMap(), at("10:00")))
        assertEquals(at("14:00"), ReminderPlanner.next(t, s, emptyMap(), at("13:45")))
        assertNull(ReminderPlanner.next(t, s, emptyMap(), at("14:00")))
    }

    @Test fun repeatsAfterTheDueTimeAsManyTimesAsSet() {
        val s = ReminderSettings(offsets = listOf(0), repeatMinutes = 10, repeatCount = 2)
        val t = targets(s, item())
        assertEquals(at("14:10"), ReminderPlanner.next(t, s, emptyMap(), at("14:00")))
        assertEquals(at("14:20"), ReminderPlanner.next(t, s, emptyMap(), at("14:10")))
        assertNull(ReminderPlanner.next(t, s, emptyMap(), at("14:20")))
    }

    @Test fun repeatsUntilDoneWhenCountIsZero() {
        val s = ReminderSettings(offsets = emptyList(), repeatMinutes = 30, repeatCount = 0)
        val t = targets(s, item())
        assertEquals(at("14:00", "2026-11-03"), ReminderPlanner.next(t, s, emptyMap(), at("13:45", "2026-11-03")))
    }

    @Test fun onlyPendingDatedAndImportantEnoughEntriesRemind() {
        val s = ReminderSettings(minPriority = Priority.MEDIUM)
        val t = targets(
            s,
            item("done", done = true, priority = Priority.HIGH),
            item("undated", date = null, priority = Priority.HIGH),
            item("gray"),
            item("yellow", priority = Priority.MEDIUM),
            item("red", priority = Priority.HIGH)
        )
        assertEquals(listOf("yellow", "red"), t.map { it.item.id })
        assertTrue(targets(ReminderSettings(enabled = false), item()).isEmpty())
    }

    @Test fun entriesWithoutTimeRemindOnlyIfAskedTo() {
        assertTrue(targets(ReminderSettings(), item(time = null)).isEmpty())
        assertEquals(at("09:00"), targets(ReminderSettings(allDayMinute = 9 * 60), item(time = null)).single().dueAt)
    }

    @Test fun snoozeAddsOneMoreReminder() {
        val s = ReminderSettings(offsets = listOf(0))
        val t = targets(s, item())
        assertEquals(at("14:10"), ReminderPlanner.next(t, s, mapOf("a" to at("14:10")), at("14:00")))
    }

    @Test fun showsOnlyTheLatestReminderOfAnEntry() {
        val s = ReminderSettings(offsets = listOf(0, 15, 30))
        val t = targets(s, item())
        assertEquals(listOf(at("13:45")), ReminderPlanner.due(t, s, emptyMap(), at("13:00"), at("13:50")).map { it.at })
        assertTrue(ReminderPlanner.due(t, s, emptyMap(), at("13:45"), at("13:50")).isEmpty())
    }

    @Test fun describesTheTimeLeft() {
        val target = targets(ReminderSettings(), item()).single()
        assertEquals("Через 15 мин · 14:00", ReminderText.describe(target, at("13:45"), zone))
        assertEquals("Сейчас · 14:00", ReminderText.describe(target, at("14:00"), zone))
        assertEquals("Просрочено на 1 ч 5 мин · 14:00", ReminderText.describe(target, at("15:05"), zone))
        assertEquals("Завтра в 14:00", ReminderText.describe(target, at("12:00", "2026-11-01"), zone))
    }
}
