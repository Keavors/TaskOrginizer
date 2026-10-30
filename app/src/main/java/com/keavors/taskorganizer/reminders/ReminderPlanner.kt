package com.keavors.taskorganizer.reminders

import com.keavors.taskorganizer.data.AppData
import com.keavors.taskorganizer.data.Item
import com.keavors.taskorganizer.data.ReminderSettings
import com.keavors.taskorganizer.ui.plural
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** An entry that can remind, with the moment it is due. */
data class ReminderTarget(val tabId: String, val tabTitle: String, val item: Item, val dueAt: Long, val timed: Boolean)

data class ReminderEvent(val target: ReminderTarget, val at: Long, val kind: Kind) {
    enum class Kind { BEFORE, DUE, REPEAT, SNOOZE }
}

/**
 * When entries remind, as pure functions of the data, the settings and the clock.
 * An entry reminds [ReminderSettings.offsets] minutes before it is due, then repeats
 * after the due moment while it is not done, plus once at a snooze time.
 */
object ReminderPlanner {
    const val MINUTE = 60_000L

    fun targets(data: AppData, s: ReminderSettings, zone: ZoneId): List<ReminderTarget> {
        if (!s.enabled) return emptyList()
        val out = ArrayList<ReminderTarget>()
        for (tab in data.tabs) {
            for (item in data.itemsOf(tab.id)) {
                if (item.done || item.priority < s.minPriority) continue
                val date = item.dueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: continue
                val time = item.dueTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
                val local = when {
                    time != null -> date.atTime(time)
                    s.allDayMinute >= 0 -> date.atStartOfDay().plusMinutes(s.allDayMinute.toLong())
                    else -> continue
                }
                out += ReminderTarget(tab.id, tab.title, item, local.atZone(zone).toInstant().toEpochMilli(), time != null)
            }
        }
        return out
    }

    /** The first reminder of [t] strictly after [after]. */
    fun firstAfter(t: ReminderTarget, s: ReminderSettings, snoozeUntil: Long?, after: Long): ReminderEvent? {
        var best: ReminderEvent? = null
        fun consider(at: Long, kind: ReminderEvent.Kind) {
            if (at > after && (best == null || at < best!!.at)) best = ReminderEvent(t, at, kind)
        }
        for (m in s.offsets) consider(t.dueAt - m * MINUTE, if (m == 0) ReminderEvent.Kind.DUE else ReminderEvent.Kind.BEFORE)
        if (s.repeatMinutes > 0) {
            val step = s.repeatMinutes * MINUTE
            val k = if (after < t.dueAt) 1 else (after - t.dueAt) / step + 1
            if (s.repeatCount == 0 || k <= s.repeatCount) consider(t.dueAt + k * step, ReminderEvent.Kind.REPEAT)
        }
        if (snoozeUntil != null) consider(snoozeUntil, ReminderEvent.Kind.SNOOZE)
        return best
    }

    /** The latest reminder of [t] in the window (from, to], if any. */
    fun latestIn(t: ReminderTarget, s: ReminderSettings, snoozeUntil: Long?, from: Long, to: Long): ReminderEvent? {
        var best: ReminderEvent? = null
        fun consider(at: Long, kind: ReminderEvent.Kind) {
            if (at > from && at <= to && (best == null || at > best!!.at)) best = ReminderEvent(t, at, kind)
        }
        for (m in s.offsets) consider(t.dueAt - m * MINUTE, if (m == 0) ReminderEvent.Kind.DUE else ReminderEvent.Kind.BEFORE)
        if (s.repeatMinutes > 0 && to >= t.dueAt) {
            val step = s.repeatMinutes * MINUTE
            var k = (to - t.dueAt) / step
            if (s.repeatCount > 0) k = minOf(k, s.repeatCount.toLong())
            if (k >= 1) consider(t.dueAt + k * step, ReminderEvent.Kind.REPEAT)
        }
        if (snoozeUntil != null) consider(snoozeUntil, ReminderEvent.Kind.SNOOZE)
        return best
    }

    /** When the next reminder of any entry happens after [after]; null = none. */
    fun next(targets: List<ReminderTarget>, s: ReminderSettings, snoozes: Map<String, Long>, after: Long): Long? =
        targets.mapNotNull { firstAfter(it, s, snoozes[it.item.id], after)?.at }.minOrNull()

    /** Reminders to show for the window (from, to]: at most one per entry, the latest. */
    fun due(targets: List<ReminderTarget>, s: ReminderSettings, snoozes: Map<String, Long>, from: Long, to: Long): List<ReminderEvent> =
        targets.mapNotNull { latestIn(it, s, snoozes[it.item.id], from, to) }
}

/** Notification texts. */
object ReminderText {
    private val ru = Locale.forLanguageTag("ru")
    private val clock = DateTimeFormatter.ofPattern("H:mm")
    private val dayMonth = DateTimeFormatter.ofPattern("d MMMM", ru)

    /** "15 мин", "2 ч 30 мин", "3 дня". */
    fun duration(minutes: Long): String = when {
        minutes < 60 -> "$minutes мин"
        minutes < 24 * 60 -> {
            val h = minutes / 60
            val m = minutes % 60
            if (m == 0L) "$h ч" else "$h ч $m мин"
        }
        minutes % (7 * 24 * 60) == 0L && minutes / (7 * 24 * 60) == 1L -> "неделю"
        else -> {
            val d = (minutes / (24 * 60)).toInt()
            "$d ${plural(d, "день", "дня", "дней")}"
        }
    }

    /** "Через 15 мин · 14:00", "Просрочено на 1 ч · 14:00", "Срок сегодня". */
    fun describe(t: ReminderTarget, now: Long, zone: ZoneId): String {
        val due = Instant.ofEpochMilli(t.dueAt).atZone(zone)
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val days = ChronoUnit.DAYS.between(today, due.toLocalDate())
        val day = when (days) {
            0L -> "сегодня"
            1L -> "завтра"
            -1L -> "вчера"
            else -> due.format(dayMonth)
        }
        if (!t.timed) return if (days < 0) "Просрочено: срок был $day" else "Срок $day"
        val time = due.format(clock)
        val left = t.dueAt - now
        return when {
            left >= 24 * 60 * ReminderPlanner.MINUTE -> "${day.replaceFirstChar { it.titlecase(ru) }} в $time"
            left >= ReminderPlanner.MINUTE -> "Через ${duration((left + ReminderPlanner.MINUTE - 1) / ReminderPlanner.MINUTE)} · $time"
            left > -ReminderPlanner.MINUTE -> "Сейчас · $time"
            -left < 24 * 60 * ReminderPlanner.MINUTE -> "Просрочено на ${duration(-left / ReminderPlanner.MINUTE)} · $time"
            else -> "Просрочено · $day в $time"
        }
    }
}
