package com.parhar.noor.domain.usecase

import com.parhar.noor.data.local.dao.DailyTaskEntryDao
import com.parhar.noor.data.local.dao.UserPreferencesDao
import com.parhar.noor.data.local.mapper.EntityMappers.toTaskHistory
import com.parhar.noor.data.remote.UserRemoteDataSource
import com.parhar.noor.data.remote.dto.RemoteSteakPeriod
import com.parhar.noor.data.remote.dto.RemoteSteakSnapshot
import com.parhar.noor.utils.TaskStatsCalculator

class StreakSyncUseCase(
    private val dailyTaskEntryDao: DailyTaskEntryDao,
    private val userPreferencesDao: UserPreferencesDao,
    private val userRemote: UserRemoteDataSource,
) {

    suspend fun syncSteakForUser(userUid: String, todayKey: String) {
        val taskHistory = dailyTaskEntryDao.getForUser(userUid).toTaskHistory()
        val primaryTaskIds = userPreferencesDao.get(userUid)
            ?.primaryTaskIds
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.toSet()
            .orEmpty()

        val currentPeriod = TaskStatsCalculator.calculateStreakPeriod(
            taskHistory = taskHistory,
            primaryTaskIds = primaryTaskIds,
            todayKey = todayKey,
        )
        val current = currentPeriod.toRemote()

        val existing = userRemote.fetchSteak(userUid)
        val highest = resolveHighest(current, existing?.highest)

        userRemote.pushSteak(
            userUid,
            RemoteSteakSnapshot(
                current = current,
                highest = highest,
            ),
        )
    }

    private fun resolveHighest(
        current: RemoteSteakPeriod,
        existingHighest: RemoteSteakPeriod?,
    ): RemoteSteakPeriod {
        if (existingHighest == null || existingHighest.count <= 0L) {
            return current
        }
        return if (current.count > existingHighest.count) {
            current
        } else {
            existingHighest
        }
    }

    private fun TaskStatsCalculator.StreakPeriod.toRemote(): RemoteSteakPeriod {
        return RemoteSteakPeriod(
            count = count.toLong(),
            start = startMillis,
            end = endMillis,
        )
    }
}
