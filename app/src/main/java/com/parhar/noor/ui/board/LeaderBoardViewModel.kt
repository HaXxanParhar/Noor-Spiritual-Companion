package com.parhar.noor.ui.board

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parhar.noor.R
import com.parhar.noor.data.repository.LeaderboardRepository
import com.parhar.noor.domain.model.LeaderboardEntry
import com.parhar.noor.utils.SessionManager
import com.parhar.noor.utils.WeekCycleUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LeaderBoardViewModel(
    application: Application,
    private val leaderboardRepository: LeaderboardRepository,
    private val sessionManager: SessionManager,
    private val weekKey: String,
) : ViewModel() {

    private val youLabel = application.getString(R.string.leaderboard_you)

    private val todayKey: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    val weekTitle: String = weekKey.let { key ->
        WeekCycleUtils.parseWeekKey(key)?.let { (friday, thursday) ->
            WeekCycleUtils.buildWeekTitle(friday, thursday)
        }.orEmpty()
    }

    val entries: StateFlow<List<LeaderboardEntry>> = leaderboardRepository.observeLeaderboardForDateKeys(
        currentUid = sessionManager.getUserId().orEmpty(),
        dateKeys = WeekCycleUtils.parseWeekKey(weekKey)?.let { (friday, thursday) ->
            WeekCycleUtils.weekDateKeys(friday, thursday)
        }.orEmpty(),
        todayKeyForStreak = todayKey,
        youLabel = youLabel,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )
}
