package com.parhar.noor.domain.usecase

import com.parhar.noor.domain.model.LeaderboardEntry
import com.parhar.noor.utils.TaskStatsCalculator

class LeaderboardUseCase {

    fun buildEntries(
        currentUid: String,
        participantProfiles: Map<String, String>,
        histories: Map<String, Map<String, Map<String, Int>>>,
        primaryTaskIds: Set<String>,
        todayKey: String,
        youLabel: String,
    ): List<LeaderboardEntry> {
        return participantProfiles.map { (participantId, displayName) ->
            val history = histories[participantId].orEmpty()
            LeaderboardEntry(
                rank = 0,
                initials = TaskStatsCalculator.toInitials(displayName),
                name = if (participantId == currentUid) youLabel else displayName,
                points = TaskStatsCalculator.calculateFridayWeeklyPoints(history, todayKey),
                streak = TaskStatsCalculator.calculateStreak(history, primaryTaskIds, todayKey),
                isCurrentUser = participantId == currentUid,
            )
        }.sortedByDescending { it.points }
            .mapIndexed { index, entry -> entry.copy(rank = index + 1) }
    }

    fun formatWeekRange(todayKey: String): String {
        return TaskStatsCalculator.formatFridayWeekRangeLabel(todayKey)
    }
}
