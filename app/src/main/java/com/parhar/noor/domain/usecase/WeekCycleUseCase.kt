package com.parhar.noor.domain.usecase

import com.parhar.noor.data.repository.BoardWeekPreparation
import com.parhar.noor.data.repository.WeekRepository
import com.parhar.noor.domain.model.ActiveWeekUi
import com.parhar.noor.domain.model.WeekCycle
import com.parhar.noor.domain.model.WeekResultSummary
import com.parhar.noor.utils.TaskStatsCalculator
import com.parhar.noor.utils.WeekCycleUtils

class WeekCycleUseCase(
    private val weekRepository: WeekRepository,
    private val leaderboardUseCase: LeaderboardUseCase,
) {

    suspend fun prepareBoardWeekState(
        uid: String,
        todayKey: String,
        participantIds: List<String>,
        participantProfiles: Map<String, String>,
        participantAvatars: Map<String, com.parhar.noor.domain.model.UserAvatar?>,
        histories: Map<String, Map<String, Map<String, Int>>>,
        primaryTaskIds: Set<String>,
        youLabel: String,
        isOnline: Boolean,
    ): BoardWeekPreparation {
        if (!isOnline || uid.isBlank()) {
            return buildOfflineFallback(todayKey)
        }

        return runCatching {
            var pendingResult: WeekResultSummary? = null

            val expiredWeek = findOldestExpiredWeek(weekRepository.fetchWeeks(uid))
            if (expiredWeek != null) {
                pendingResult = finalizeWeek(
                    uid = uid,
                    week = expiredWeek,
                    participantIds = participantIds,
                    participantProfiles = participantProfiles,
                    participantAvatars = participantAvatars,
                    histories = histories,
                    primaryTaskIds = primaryTaskIds,
                    todayKey = todayKey,
                    youLabel = youLabel,
                )
            }

            val defaults = WeekCycleUtils.buildWeekCycleDefaults(todayKey)
            weekRepository.createWeekIfMissing(
                uid = uid,
                week = WeekCycle(
                    weekKey = defaults.weekKey,
                    joinedAt = defaults.joinedAt,
                    title = defaults.title,
                    start = defaults.start,
                    end = defaults.end,
                    endAt = defaults.endAt,
                    myPosition = -1,
                    points = 0,
                    done = false,
                ),
            )

            var currentWeek = weekRepository.fetchWeek(uid, defaults.weekKey)
            if (currentWeek != null && !currentWeek.done && currentWeek.endAt < System.currentTimeMillis()) {
                pendingResult = finalizeWeek(
                    uid = uid,
                    week = currentWeek,
                    participantIds = participantIds,
                    participantProfiles = participantProfiles,
                    participantAvatars = participantAvatars,
                    histories = histories,
                    primaryTaskIds = primaryTaskIds,
                    todayKey = todayKey,
                    youLabel = youLabel,
                )
                val refreshedDefaults = WeekCycleUtils.buildWeekCycleDefaults(todayKey)
                weekRepository.createWeekIfMissing(
                    uid = uid,
                    week = WeekCycle(
                        weekKey = refreshedDefaults.weekKey,
                        joinedAt = refreshedDefaults.joinedAt,
                        title = refreshedDefaults.title,
                        start = refreshedDefaults.start,
                        end = refreshedDefaults.end,
                        endAt = refreshedDefaults.endAt,
                        myPosition = -1,
                        points = 0,
                        done = false,
                    ),
                )
                currentWeek = weekRepository.fetchWeek(uid, refreshedDefaults.weekKey)
            }

            val activeWeek = currentWeek?.let { week ->
                syncCurrentWeekPoints(uid, week, histories[uid].orEmpty(), todayKey)
                ActiveWeekUi(
                    weekKey = week.weekKey,
                    title = week.title,
                    endAtMillis = week.endAt,
                    countdownText = WeekCycleUtils.formatCountdown(week.endAt),
                )
            }

            BoardWeekPreparation(
                activeWeek = activeWeek,
                pendingWeekResult = pendingResult,
            )
        }.getOrElse {
            buildOfflineFallback(todayKey)
        }
    }

    private suspend fun finalizeWeek(
        uid: String,
        week: WeekCycle,
        participantIds: List<String>,
        participantProfiles: Map<String, String>,
        participantAvatars: Map<String, com.parhar.noor.domain.model.UserAvatar?>,
        histories: Map<String, Map<String, Map<String, Int>>>,
        primaryTaskIds: Set<String>,
        todayKey: String,
        youLabel: String,
    ): WeekResultSummary {
        val range = WeekCycleUtils.parseWeekKey(week.weekKey)
        val dateKeys = range?.let { (friday, thursday) ->
            WeekCycleUtils.weekDateKeys(friday, thursday)
        }.orEmpty()

        val completeProfiles = participantIds.associateWith { participantId ->
            participantProfiles[participantId] ?: participantId
        }

        val entries = leaderboardUseCase.buildEntriesForDateKeys(
            currentUid = uid,
            participantProfiles = completeProfiles,
            participantAvatars = participantIds.associateWith { participantId ->
                participantAvatars[participantId]
            },
            histories = histories,
            primaryTaskIds = primaryTaskIds,
            dateKeys = dateKeys,
            todayKeyForStreak = todayKey,
            youLabel = youLabel,
        )

        val myEntry = entries.firstOrNull { it.isCurrentUser }
        val myPoints = myEntry?.points ?: TaskStatsCalculator.calculateWeeklyPointsForDateKeys(
            histories[uid].orEmpty(),
            dateKeys,
        )
        val myPosition = myEntry?.rank ?: 1

        weekRepository.updateWeek(
            uid = uid,
            weekKey = week.weekKey,
            fields = mapOf(
                "points" to myPoints,
                "myPosition" to myPosition,
                "done" to true,
            ),
        )
        weekRepository.incrementMedalTier(uid, myPosition)

        return buildWeekResultSummary(
            weekKey = week.weekKey,
            title = week.title,
            points = myPoints,
            position = myPosition,
        )
    }

    private suspend fun syncCurrentWeekPoints(
        uid: String,
        week: WeekCycle,
        userHistory: Map<String, Map<String, Int>>,
        todayKey: String,
    ) {
        if (week.done) return
        val dateKeys = TaskStatsCalculator.fridayWeekDateKeys(todayKey)
        val livePoints = TaskStatsCalculator.calculateWeeklyPointsForDateKeys(userHistory, dateKeys)
        if (livePoints != week.points) {
            weekRepository.updateWeek(
                uid = uid,
                weekKey = week.weekKey,
                fields = mapOf("points" to livePoints),
            )
        }
    }

    private fun findOldestExpiredWeek(weeks: Map<String, WeekCycle>): WeekCycle? {
        val now = System.currentTimeMillis()
        return weeks.values
            .filter { !it.done && it.endAt < now }
            .minByOrNull { it.endAt }
    }

    private fun buildWeekResultSummary(
        weekKey: String,
        title: String,
        points: Int,
        position: Int,
    ): WeekResultSummary {
        val medalEmoji = when (position) {
            1 -> "🥇"
            2 -> "🥈"
            3 -> "🥉"
            else -> null
        }
        val congratsMessage = when (position) {
            1 -> "Congratulations on 1st place!"
            2 -> "Congratulations on 2nd place!"
            3 -> "Congratulations on 3rd place!"
            else -> null
        }
        return WeekResultSummary(
            weekKey = weekKey,
            title = title,
            points = points,
            position = position,
            medalEmoji = medalEmoji,
            congratsMessage = congratsMessage,
        )
    }

    private fun buildOfflineFallback(todayKey: String): BoardWeekPreparation {
        val defaults = WeekCycleUtils.buildWeekCycleDefaults(todayKey)
        return BoardWeekPreparation(
            activeWeek = ActiveWeekUi(
                weekKey = defaults.weekKey,
                title = defaults.title,
                endAtMillis = defaults.endAt,
                countdownText = WeekCycleUtils.formatCountdown(defaults.endAt),
            ),
            pendingWeekResult = null,
        )
    }
}
