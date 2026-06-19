package com.parhar.noor.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateKeyUtils {

    private val keyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
    private val displayFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", Locale.getDefault())

    fun todayKey(): String = LocalDate.now().format(keyFormatter)

    fun yesterdayKey(): String = LocalDate.now().minusDays(1).format(keyFormatter)

    fun parseDateKey(dateKey: String): LocalDate? {
        return runCatching { LocalDate.parse(dateKey, keyFormatter) }.getOrNull()
    }

    fun daysFromToday(dateKey: String): Int? {
        val date = parseDateKey(dateKey) ?: return null
        return (date.toEpochDay() - LocalDate.now().toEpochDay()).toInt()
    }

    fun isToday(dateKey: String): Boolean = daysFromToday(dateKey) == 0

    fun isYesterday(dateKey: String): Boolean = daysFromToday(dateKey) == -1

    /** Only today and yesterday allow checking/unchecking tasks on Home. */
    fun canCheckTasksForDate(dateKey: String): Boolean {
        val offset = daysFromToday(dateKey) ?: return false
        return offset == 0 || offset == -1
    }

    fun offsetDateKey(dateKey: String, dayOffset: Int): String {
        val date = parseDateKey(dateKey) ?: LocalDate.now()
        return date.plusDays(dayOffset.toLong()).format(keyFormatter)
    }

    fun formatDisplayDate(dateKey: String): String {
        val date = parseDateKey(dateKey) ?: return dateKey
        val label = when (daysFromToday(dateKey)) {
            0 -> "Today"
            -1 -> "Yesterday"
            else -> null
        }
        val formatted = date.format(displayFormatter)
        return if (label != null) "$label • $formatted" else formatted
    }

    /** @deprecated Use [canCheckTasksForDate] — kept for friend task viewer (today/yesterday only). */
    fun isAllowedHomeDate(dateKey: String): Boolean = canCheckTasksForDate(dateKey)

    fun clampToAllowedHomeDate(dateKey: String): String {
        return if (canCheckTasksForDate(dateKey)) dateKey else todayKey()
    }

    fun previousAllowedDate(dateKey: String): String? {
        return when (daysFromToday(dateKey)) {
            0 -> yesterdayKey()
            else -> null
        }
    }

    fun nextAllowedDate(dateKey: String): String? {
        return when (daysFromToday(dateKey)) {
            -1 -> todayKey()
            else -> null
        }
    }
}
