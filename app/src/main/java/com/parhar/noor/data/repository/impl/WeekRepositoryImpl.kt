package com.parhar.noor.data.repository.impl

import com.parhar.noor.data.remote.UserRemoteDataSource
import com.parhar.noor.data.remote.dto.RemoteUserMedals
import com.parhar.noor.data.remote.dto.RemoteWeekCycle
import com.parhar.noor.data.repository.WeekRepository
import com.parhar.noor.domain.model.UserMedals
import com.parhar.noor.domain.model.WeekCycle

class WeekRepositoryImpl(
    private val userRemote: UserRemoteDataSource,
) : WeekRepository {

    override suspend fun fetchWeeks(uid: String): Map<String, WeekCycle> =
        userRemote.fetchWeeks(uid).mapValues { (_, week) -> week.toDomain() }

    override suspend fun fetchWeek(uid: String, weekKey: String): WeekCycle? =
        userRemote.fetchWeek(uid, weekKey)?.toDomain()

    override suspend fun createWeekIfMissing(uid: String, week: WeekCycle) {
        userRemote.createWeekIfMissing(uid, week.toRemote())
    }

    override suspend fun updateWeek(uid: String, weekKey: String, fields: Map<String, Any>) {
        userRemote.updateWeek(uid, weekKey, fields)
    }

    override suspend fun fetchUserMedals(uid: String): UserMedals =
        userRemote.fetchUserMedals(uid).toDomain()

    override suspend fun incrementMedalTier(uid: String, position: Int) {
        userRemote.incrementMedalTier(uid, position)
    }

    private fun RemoteWeekCycle.toDomain(): WeekCycle = WeekCycle(
        weekKey = weekKey,
        joinedAt = joinedAt,
        title = title,
        start = start,
        end = end,
        endAt = endAt,
        myPosition = myPosition,
        points = points,
        done = done,
    )

    private fun WeekCycle.toRemote(): RemoteWeekCycle = RemoteWeekCycle(
        weekKey = weekKey,
        joinedAt = joinedAt,
        title = title,
        start = start,
        end = end,
        endAt = endAt,
        myPosition = myPosition,
        points = points,
        done = done,
    )

    private fun RemoteUserMedals.toDomain(): UserMedals = UserMedals(
        total1st = total1st,
        total2nd = total2nd,
        total3rd = total3rd,
        totalTop5 = totalTop5,
        totalTop10 = totalTop10,
    )
}
