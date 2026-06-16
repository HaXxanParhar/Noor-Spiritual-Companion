package com.parhar.noor.ui.board

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parhar.noor.R
import com.parhar.noor.data.repository.LeaderboardRepository
import com.parhar.noor.domain.model.BoardState
import com.parhar.noor.utils.SessionManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BoardViewModel(
    application: Application,
    private val leaderboardRepository: LeaderboardRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val youLabel = application.getString(R.string.leaderboard_you)

    private val todayKey: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    val boardState: StateFlow<BoardState> = leaderboardRepository.observeBoardState(
        currentUid = sessionManager.getUserId().orEmpty(),
        todayKey = todayKey,
        youLabel = youLabel,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BoardState(
            dateRangeLabel = "",
            entries = emptyList(),
            hasFriends = false,
        ),
    )
}
