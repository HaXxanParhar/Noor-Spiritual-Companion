package com.parhar.noor.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object WeekCycleUtils {

    private const val DATE_KEY_FORMAT = "yyyy-MM-dd"
    private const val DATETIME_FORMAT = "yyyy-MM-dd_HH-mm"
    private const val TITLE_DAY_FORMAT = "EEE dd"
    private const val TITLE_YEAR_FORMAT = "EEE dd, yyyy"

    fun currentWeekKey(todayKey: String): String {
        val range = TaskStatsCalculator.getFridayWeekRange(todayKey)
        return weekKeyFromRange(range.fridayKey, range.thursdayKey)
    }

    fun weekKeyFromRange(fridayKey: String, thursdayKey: String): String =
        "${fridayKey}_$thursdayKey"

    fun parseWeekKey(weekKey: String): Pair<String, String>? {
        val parts = weekKey.split("_")
        if (parts.size != 2) return null
        val fridayKey = parts[0]
        val thursdayKey = parts[1]
        if (!isValidDateKey(fridayKey) || !isValidDateKey(thursdayKey)) return null
        return fridayKey to thursdayKey
    }

    fun weekDateKeys(fridayKey: String, thursdayKey: String): List<String> {
        val dateFormat = dateFormatter()
        val fridayCalendar = calendarForDateKey(fridayKey)
        val thursdayCalendar = calendarForDateKey(thursdayKey)
        val dateKeys = mutableListOf<String>()
        val cursor = fridayCalendar.clone() as Calendar
        while (!cursor.after(thursdayCalendar)) {
            dateKeys.add(dateFormat.format(cursor.time))
            cursor.add(Calendar.DAY_OF_YEAR, 1)
        }
        return dateKeys
    }

    fun buildWeekTitle(fridayKey: String, thursdayKey: String): String {
        val dateFormat = dateFormatter()
        val fridayDate = dateFormat.parse(fridayKey) ?: Date()
        val thursdayDate = dateFormat.parse(thursdayKey) ?: Date()
        val fridayCalendar = Calendar.getInstance().apply { time = fridayDate }
        val thursdayCalendar = Calendar.getInstance().apply { time = thursdayDate }
        val sameYear = fridayCalendar.get(Calendar.YEAR) == thursdayCalendar.get(Calendar.YEAR)
        return if (sameYear) {
            val dayFormat = SimpleDateFormat(TITLE_DAY_FORMAT, Locale.getDefault())
            "${dayFormat.format(fridayDate)} - ${dayFormat.format(thursdayDate)}"
        } else {
            val yearFormat = SimpleDateFormat(TITLE_YEAR_FORMAT, Locale.getDefault())
            "${yearFormat.format(fridayDate)} - ${yearFormat.format(thursdayDate)}"
        }
    }

    fun buildStartEndStrings(fridayKey: String, thursdayKey: String): Pair<String, String> {
        val formatter = SimpleDateFormat(DATETIME_FORMAT, Locale.getDefault())
        val fridayStart = calendarForDateKey(fridayKey).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }
        val thursdayEnd = calendarForDateKey(thursdayKey).apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
        }
        return formatter.format(fridayStart.time) to formatter.format(thursdayEnd.time)
    }

    fun buildEndAtMillis(thursdayKey: String): Long {
        val calendar = calendarForDateKey(thursdayKey)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    fun buildWeekCycleDefaults(
        todayKey: String,
        joinedAt: Long = System.currentTimeMillis()
    ): WeekCycleDefaults {
        val range = TaskStatsCalculator.getFridayWeekRange(todayKey)
        val (start, end) = buildStartEndStrings(range.fridayKey, range.thursdayKey)
        return WeekCycleDefaults(
            weekKey = weekKeyFromRange(range.fridayKey, range.thursdayKey),
            joinedAt = joinedAt,
            title = buildWeekTitle(range.fridayKey, range.thursdayKey),
            start = start,
            end = end,
            endAt = buildEndAtMillis(range.thursdayKey),
            fridayKey = range.fridayKey,
            thursdayKey = range.thursdayKey,
        )
    }

    fun formatCountdown(endAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val remainingMillis = (endAtMillis - nowMillis).coerceAtLeast(0L)
        val totalMinutes = remainingMillis / 60_000L
        val days = (totalMinutes / (24 * 60)).toInt()
        val hours = ((totalMinutes % (24 * 60)) / 60).toInt()
        val minutes = (totalMinutes % 60).toInt()

        val dayLabel = if (days == 1) "day" else "days"
        val hourLabel = if (hours == 1) "hr" else "hrs"
        val minuteLabel = if (minutes == 1) "min" else "mins"

        val message = if (days == 0) {
            "Ending in $hours $hourLabel $minutes $minuteLabel"
        } else {
            "Ending in $days $dayLabel $hours $hourLabel $minutes $minuteLabel"
        }
        return message
    }

    data class WeekCycleDefaults(
        val weekKey: String,
        val joinedAt: Long,
        val title: String,
        val start: String,
        val end: String,
        val endAt: Long,
        val fridayKey: String,
        val thursdayKey: String,
    )

    private fun isValidDateKey(dateKey: String): Boolean {
        return runCatching {
            dateFormatter().parse(dateKey)
        }.getOrNull() != null
    }

    private fun dateFormatter(): SimpleDateFormat =
        SimpleDateFormat(DATE_KEY_FORMAT, Locale.getDefault())

    private fun calendarForDateKey(dateKey: String): Calendar {
        val calendar = Calendar.getInstance()
        calendar.time = dateFormatter().parse(dateKey) ?: Date()
        return calendar
    }
}
