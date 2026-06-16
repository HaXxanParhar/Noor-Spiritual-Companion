package com.parhar.noor.data.repository

import com.parhar.noor.domain.model.BoardState
import kotlinx.coroutines.flow.Flow

interface LeaderboardRepository {
    fun observeBoardState(currentUid: String, todayKey: String, youLabel: String): Flow<BoardState>
}
