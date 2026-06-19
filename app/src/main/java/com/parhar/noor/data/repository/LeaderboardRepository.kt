package com.parhar.noor.data.repository

import com.parhar.noor.domain.model.BoardState
import com.parhar.noor.domain.model.LeaderboardEntry
import kotlinx.coroutines.flow.Flow

interface LeaderboardRepository {
    fun observeBoardState(currentUid: String, todayKey: String, youLabel: String): Flow<BoardState>
    fun observeLeaderboardForDateKeys(
        currentUid: String,
        dateKeys: List<String>,
        todayKeyForStreak: String,
        youLabel: String,
    ): Flow<List<LeaderboardEntry>>
}
