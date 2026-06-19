package com.parhar.noor.domain.usecase

import com.parhar.noor.domain.model.LeaderboardEntry
import com.parhar.noor.utils.TaskStatsCalculator

class LeaderboardUseCase {

    fun buildEntries(
        currentUid: String,
        participantProfiles: Map<String, String>,
        participantAvatars: Map<String, com.parhar.noor.domain.model.UserAvatar?>,
        histories: Map<String, Map<String, Map<String, Int>>>,
        primaryTaskIds: Set<String>,
        todayKey: String,
        youLabel: String,
    ): List<LeaderboardEntry> {
        return buildEntriesForDateKeys(
            currentUid = currentUid,
            participantProfiles = participantProfiles,
            participantAvatars = participantAvatars,
            histories = histories,
            primaryTaskIds = primaryTaskIds,
            dateKeys = TaskStatsCalculator.fridayWeekDateKeys(todayKey),
            todayKeyForStreak = todayKey,
            youLabel = youLabel,
        )
    }

    fun buildEntriesForDateKeys(
        currentUid: String,
        participantProfiles: Map<String, String>,
        participantAvatars: Map<String, com.parhar.noor.domain.model.UserAvatar?>,
        histories: Map<String, Map<String, Map<String, Int>>>,
        primaryTaskIds: Set<String>,
        dateKeys: List<String>,
        todayKeyForStreak: String,
        youLabel: String,
    ): List<LeaderboardEntry> {
        return participantProfiles.map { (participantId, displayName) ->
            val history = histories[participantId].orEmpty()
            val avatar = participantAvatars[participantId]
            LeaderboardEntry(
                uid = participantId,
                rank = 0,
                initials = avatar?.text?.takeIf { it.isNotBlank() }
                    ?: TaskStatsCalculator.toInitials(displayName),
                name = if (participantId == currentUid) youLabel else displayName,
                points = TaskStatsCalculator.calculateWeeklyPointsForDateKeys(history, dateKeys),
                streak = TaskStatsCalculator.calculateStreak(history, primaryTaskIds, todayKeyForStreak),
                isCurrentUser = participantId == currentUid,
                avatar = avatar,
            )
        }.sortedByDescending { it.points }
            .mapIndexed { index, entry -> entry.copy(rank = index + 1) }
    }

    fun formatWeekRange(todayKey: String): String {
        return TaskStatsCalculator.formatFridayWeekRangeLabel(todayKey)
    }
}
