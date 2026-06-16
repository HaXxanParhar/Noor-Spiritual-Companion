package com.parhar.noor.domain.usecase

import com.parhar.noor.domain.model.UserTaskStats
import com.parhar.noor.utils.TaskStatsCalculator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TaskStatsUseCase {

    fun calculateStats(
        taskHistory: Map<String, Map<String, Int>>,
        primaryTaskIds: Set<String>,
        todayKey: String,
        todayTaskPoints: Map<String, Int>,
        checkedTaskIds: Set<String>,
        taskPointLookup: Map<String, Int>,
    ): UserTaskStats {
        val mergedHistory = taskHistory.toMutableMap()
        mergedHistory[todayKey] = todayTaskPoints

        val todayPoints = checkedTaskIds.sumOf { taskId ->
            todayTaskPoints[taskId] ?: taskPointLookup[taskId] ?: 0
        }.coerceAtLeast(0)

        val weeklyPoints = calculateRollingWeeklyPoints(mergedHistory, todayKey)
        val allTimePoints = mergedHistory.values.sumOf { day ->
            day.values.sumOf { points -> points.coerceAtLeast(0) }
        }
        val streak = TaskStatsCalculator.calculateStreak(mergedHistory, primaryTaskIds, todayKey)

        return UserTaskStats(
            todayPoints = todayPoints,
            weeklyPoints = weeklyPoints,
            allTimePoints = allTimePoints,
            streak = streak,
        )
    }

    private fun calculateRollingWeeklyPoints(
        taskHistory: Map<String, Map<String, Int>>,
        todayKey: String,
    ): Int {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.time = dateFormat.parse(todayKey) ?: Date()

        var total = 0
        repeat(WEEKLY_DAYS_COUNT) {
            val dateKey = dateFormat.format(calendar.time)
            total += taskHistory[dateKey].orEmpty().values.sumOf { points -> points.coerceAtLeast(0) }
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return total
    }

    private companion object {
        private const val WEEKLY_DAYS_COUNT = 7
    }
}
