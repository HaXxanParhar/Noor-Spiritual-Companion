package com.parhar.noor.data.repository.impl

import com.parhar.noor.data.local.dao.DailyTaskEntryDao
import com.parhar.noor.data.local.dao.FriendDao
import com.parhar.noor.data.local.dao.UserDao
import com.parhar.noor.data.local.dao.UserPreferencesDao
import com.parhar.noor.data.local.mapper.EntityMappers.toTaskHistory
import com.parhar.noor.data.repository.LeaderboardRepository
import com.parhar.noor.data.sync.ConnectivityMonitor
import com.parhar.noor.domain.model.BoardState
import com.parhar.noor.domain.usecase.LeaderboardUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class LeaderboardRepositoryImpl(
    private val friendDao: FriendDao,
    private val userDao: UserDao,
    private val dailyTaskEntryDao: DailyTaskEntryDao,
    private val userPreferencesDao: UserPreferencesDao,
    private val connectivityMonitor: ConnectivityMonitor,
    private val leaderboardUseCase: LeaderboardUseCase,
) : LeaderboardRepository {

    override fun observeBoardState(
        currentUid: String,
        todayKey: String,
        youLabel: String,
    ): Flow<BoardState> {
        return combine(
            friendDao.observeFriendIds(currentUid),
            dailyTaskEntryDao.observeAll(),
            userDao.observeAll(),
            userPreferencesDao.observe(currentUid),
            connectivityMonitor.isOnline,
        ) { friendIds, allEntries, allUsers, preferences, isOnline ->
            if (friendIds.isEmpty()) {
                return@combine BoardState(
                    dateRangeLabel = leaderboardUseCase.formatWeekRange(todayKey),
                    entries = emptyList(),
                    hasFriends = false,
                    isOffline = !isOnline,
                )
            }

            val participantIds = (friendIds + currentUid).distinct()
            val profiles = allUsers
                .filter { it.uid in participantIds }
                .associate { user -> user.uid to user.name.ifBlank { user.uid } }

            val histories = allEntries
                .filter { it.userUid in participantIds }
                .groupBy { it.userUid }
                .mapValues { (_, entries) -> entries.toTaskHistory() }

            val primaryIds = preferences
                ?.primaryTaskIds
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.toSet()
                .orEmpty()

            val completeProfiles = participantIds.associateWith { uid ->
                profiles[uid] ?: uid
            }

            val entries = leaderboardUseCase.buildEntries(
                currentUid = currentUid,
                participantProfiles = completeProfiles,
                histories = histories,
                primaryTaskIds = primaryIds,
                todayKey = todayKey,
                youLabel = youLabel,
            )

            BoardState(
                dateRangeLabel = leaderboardUseCase.formatWeekRange(todayKey),
                entries = entries,
                hasFriends = true,
                isOffline = !isOnline,
            )
        }
    }
}
