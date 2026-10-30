package com.keavors.taskorganizer.ui

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Russian plural forms: plural(5, "запись", "записи", "записей") = "записей". */
fun plural(n: Int, one: String, few: String, many: String): String {
    val mod10 = n % 10
    val mod100 = n % 100
    return when {
        mod10 == 1 && mod100 != 11 -> one
        mod10 in 2..4 && (mod100 < 12 || mod100 > 14) -> few
        else -> many
    }
}

fun activeCount(n: Int) = "$n ${plural(n, "активная", "активные", "активных")}"
fun doneCount(n: Int) = "$n ${plural(n, "выполнена", "выполнены", "выполнено")}"
fun notesCount(n: Int) = "$n ${plural(n, "заметка", "заметки", "заметок")}"
fun entriesCount(n: Int) = "$n ${plural(n, "запись", "записи", "записей")}"

fun formatClock(seconds: Long): String = "%d:%02d".format(seconds / 60, seconds % 60)

/** Due date chip: text and how urgent it is. */
enum class DueTone { OVERDUE, TODAY, SOON, LATER }

object Due {
    private val ru = Locale("ru")
    private val short = DateTimeFormatter.ofPattern("d MMM", ru)
    private val withYear = DateTimeFormatter.ofPattern("d MMM yyyy", ru)

    fun today(): LocalDate = LocalDate.now()

    fun label(iso: String, today: LocalDate = today()): Pair<String, DueTone> {
        val date = runCatching { LocalDate.parse(iso) }.getOrNull() ?: return iso to DueTone.LATER
        val diff = ChronoUnit.DAYS.between(today, date)
        return when (diff) {
            0L -> "Сегодня" to DueTone.TODAY
            1L -> "Завтра" to DueTone.SOON
            -1L -> "Вчера" to DueTone.OVERDUE
            else -> {
                val text = (if (date.year == today.year) short else withYear).format(date).replace(".", "")
                text to when {
                    diff < 0 -> DueTone.OVERDUE
                    diff <= 7 -> DueTone.SOON
                    else -> DueTone.LATER
                }
            }
        }
    }
}

/** "5 мин назад", "вчера" for the trash. */
fun ago(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val minutes = (now - timestamp) / 60_000
    return when {
        minutes < 1 -> "только что"
        minutes < 60 -> "$minutes мин назад"
        minutes < 24 * 60 -> "${minutes / 60} ч назад"
        minutes < 48 * 60 -> "вчера"
        else -> {
            val days = (minutes / (24 * 60)).toInt()
            "$days ${plural(days, "день", "дня", "дней")} назад"
        }
    }
}
