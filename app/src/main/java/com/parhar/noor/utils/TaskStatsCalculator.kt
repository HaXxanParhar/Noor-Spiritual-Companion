package com.parhar.noor.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TaskStatsCalculator {

    fun calculateStreak(
        taskHistory: Map<String, Map<String, Int>>,
        primaryTaskIds: Set<String>,
        todayKey: String,
    ): Int = calculateStreakPeriod(taskHistory, primaryTaskIds, todayKey).count

    fun calculateStreakPeriod(
        taskHistory: Map<String, Map<String, Int>>,
        primaryTaskIds: Set<String>,
        todayKey: String,
    ): StreakPeriod {
        if (primaryTaskIds.isEmpty()) {
            return StreakPeriod(count = 0, startMillis = 0L, endMillis = 0L)
        }

        val dateFormat = dateFormatter()
        val calendar = Calendar.getInstance()
        calendar.time = dateFormat.parse(todayKey) ?: Date()

        val isTodayComplete = primaryTaskIds.any { taskId ->
            (taskHistory[todayKey]?.get(taskId) ?: 0) > 0
        }
        if (!isTodayComplete) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        val endCalendar = calendar.clone() as Calendar
        val checkCalendar = calendar.clone() as Calendar
        var streak = 0
        while (true) {
            val dateKey = dateFormat.format(checkCalendar.time)
            val dayTasks = taskHistory[dateKey].orEmpty()
            val isDayComplete = primaryTaskIds.any { taskId ->
                (dayTasks[taskId] ?: 0) > 0
            }
            if (!isDayComplete) break
            streak++
            checkCalendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        if (streak == 0) {
            return StreakPeriod(count = 0, startMillis = 0L, endMillis = 0L)
        }

        val startCalendar = endCalendar.clone() as Calendar
        startCalendar.add(Calendar.DAY_OF_YEAR, -(streak - 1))

        return StreakPeriod(
            count = streak,
            startMillis = startOfDayMillis(startCalendar),
            endMillis = startOfDayMillis(endCalendar),
        )
    }

    data class StreakPeriod(
        val count: Int,
        val startMillis: Long,
        val endMillis: Long,
    )

    fun calculateFridayWeeklyPoints(
        taskHistory: Map<String, Map<String, Int>>,
        todayKey: String,
    ): Int {
        return fridayWeekDateKeys(todayKey).sumOf { dateKey ->
            taskHistory[dateKey].orEmpty().values.sumOf { points -> points.coerceAtLeast(0) }
        }
    }

    /**
     * Date keys from the current week's Friday through today (inclusive).
     * Friday = today only; Thursday = full Fri–Thu week; days in between = Fri through today.
     */
    fun fridayWeekDateKeys(todayKey: String): List<String> {
        val dateFormat = dateFormatter()
        val todayCalendar = calendarForDateKey(todayKey)
        val fridayCalendar = weekStartFridayCalendar(todayKey)
        val dateKeys = mutableListOf<String>()
        val cursor = fridayCalendar.clone() as Calendar
        while (!cursor.after(todayCalendar)) {
            dateKeys.add(dateFormat.format(cursor.time))
            cursor.add(Calendar.DAY_OF_YEAR, 1)
        }
        return dateKeys
    }

    /** Full leaderboard week boundaries: Friday start through Thursday end (always 7 days). */
    fun getFridayWeekRange(todayKey: String): FridayWeekRange {
        val dateFormat = dateFormatter()
        val fridayCalendar = weekStartFridayCalendar(todayKey)
        val thursdayCalendar = fridayCalendar.clone() as Calendar
        thursdayCalendar.add(Calendar.DAY_OF_YEAR, 6)
        return FridayWeekRange(
            fridayKey = dateFormat.format(fridayCalendar.time),
            thursdayKey = dateFormat.format(thursdayCalendar.time),
        )
    }

    fun formatFridayWeekRangeLabel(todayKey: String): String {
        val range = getFridayWeekRange(todayKey)
        val labelFormat = SimpleDateFormat("EEE dd", Locale.getDefault())
        val dateFormat = dateFormatter()
        val fridayDate = dateFormat.parse(range.fridayKey) ?: Date()
        val thursdayDate = dateFormat.parse(range.thursdayKey) ?: Date()
        return "🗓 ${labelFormat.format(fridayDate)} – ${labelFormat.format(thursdayDate)}"
    }

    data class FridayWeekRange(
        val fridayKey: String,
        val thursdayKey: String,
    )

    private fun weekStartFridayCalendar(todayKey: String): Calendar {
        val todayCalendar = calendarForDateKey(todayKey)
        val daysSinceFriday = (todayCalendar.get(Calendar.DAY_OF_WEEK) - Calendar.FRIDAY + 7) % 7
        val fridayCalendar = todayCalendar.clone() as Calendar
        fridayCalendar.add(Calendar.DAY_OF_YEAR, -daysSinceFriday)
        return fridayCalendar
    }

    private fun calendarForDateKey(dateKey: String): Calendar {
        val calendar = Calendar.getInstance()
        calendar.time = dateFormatter().parse(dateKey) ?: Date()
        return calendar
    }

    fun parseDailyTaskPoints(rawValue: String?): Map<String, Int> {
        if (rawValue.isNullOrBlank()) return emptyMap()

        return rawValue.split(",")
            .mapNotNull { pair ->
                val parts = pair.split("=", ":", limit = 2)
                val taskId = parts.getOrNull(0)?.trim().orEmpty()
                val points = parts.getOrNull(1)?.trim()?.toIntOrNull()
                if (taskId.isBlank() || points == null) {
                    null
                } else {
                    taskId to points
                }
            }
            .toMap()
    }

    fun toInitials(name: String): String {
        val parts = name.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        return when {
            parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase(Locale.getDefault())
            parts.isNotEmpty() -> parts[0].take(2).uppercase(Locale.getDefault())
            else -> "?"
        }
    }

    private fun dateFormatter(): SimpleDateFormat {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }

    private fun startOfDayMillis(calendar: Calendar): Long {
        val day = calendar.clone() as Calendar
        day.set(Calendar.HOUR_OF_DAY, 0)
        day.set(Calendar.MINUTE, 0)
        day.set(Calendar.SECOND, 0)
        day.set(Calendar.MILLISECOND, 0)
        return day.timeInMillis
    }
}
