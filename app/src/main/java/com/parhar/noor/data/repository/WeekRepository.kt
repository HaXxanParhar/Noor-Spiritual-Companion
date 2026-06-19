package com.parhar.noor.data.repository

import com.parhar.noor.domain.model.ActiveWeekUi
import com.parhar.noor.domain.model.UserMedals
import com.parhar.noor.domain.model.WeekCycle
import com.parhar.noor.domain.model.WeekResultSummary

interface WeekRepository {
    suspend fun fetchWeeks(uid: String): Map<String, WeekCycle>
    suspend fun fetchWeek(uid: String, weekKey: String): WeekCycle?
    suspend fun createWeekIfMissing(uid: String, week: WeekCycle)
    suspend fun updateWeek(uid: String, weekKey: String, fields: Map<String, Any>)
    suspend fun fetchUserMedals(uid: String): UserMedals
    suspend fun incrementMedalTier(uid: String, position: Int)
}

data class BoardWeekPreparation(
    val activeWeek: ActiveWeekUi?,
    val pendingWeekResult: WeekResultSummary?,
)
