package com.parhar.noor.ui.board

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parhar.noor.R
import com.parhar.noor.data.local.dao.DailyTaskEntryDao
import com.parhar.noor.data.local.dao.FriendDao
import com.parhar.noor.data.local.dao.UserDao
import com.parhar.noor.data.local.dao.UserPreferencesDao
import com.parhar.noor.data.local.mapper.EntityMappers.toDomain
import com.parhar.noor.data.local.mapper.EntityMappers.toTaskHistory
import com.parhar.noor.data.repository.LeaderboardRepository
import com.parhar.noor.data.repository.WeekRepository
import com.parhar.noor.data.sync.ConnectivityMonitor
import com.parhar.noor.domain.model.BoardState
import com.parhar.noor.domain.model.WeekResultSummary
import com.parhar.noor.domain.usecase.WeekCycleUseCase
import com.parhar.noor.utils.SessionManager
import com.parhar.noor.utils.WeekCycleUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BoardViewModel(
    application: Application,
    private val leaderboardRepository: LeaderboardRepository,
    private val weekRepository: WeekRepository,
    private val weekCycleUseCase: WeekCycleUseCase,
    private val friendDao: FriendDao,
    private val userDao: UserDao,
    private val dailyTaskEntryDao: DailyTaskEntryDao,
    private val userPreferencesDao: UserPreferencesDao,
    private val connectivityMonitor: ConnectivityMonitor,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val youLabel = application.getString(R.string.leaderboard_you)
    private val preparingWeekLabel = application.getString(R.string.board_preparing_week)

    private val todayKey: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private val _weekTitle = MutableStateFlow("")
    private val _countdownText = MutableStateFlow<String?>(null)
    private val _weekEndAtMillis = MutableStateFlow(0L)
    private val _isWeekPreparing = MutableStateFlow(false)
    private val _weekResultEvent = MutableSharedFlow<WeekResultSummary>(extraBufferCapacity = 1)

    val isWeekPreparing: StateFlow<Boolean> = _isWeekPreparing.asStateFlow()
    val weekResultEvent: SharedFlow<WeekResultSummary> = _weekResultEvent.asSharedFlow()
    val preparingWeekMessage: String = preparingWeekLabel

    private var countdownJob: Job? = null
    private var refreshJob: Job? = null

    val boardState: StateFlow<BoardState> = combine(
        leaderboardRepository.observeBoardState(
            currentUid = sessionManager.getUserId().orEmpty(),
            todayKey = todayKey,
            youLabel = youLabel,
        ),
        _weekTitle,
        _countdownText,
        _weekEndAtMillis,
    ) { baseState, weekTitle, countdownText, weekEndAt ->
        baseState.copy(
            weekTitle = weekTitle.ifBlank { baseState.dateRangeLabel.removePrefix("🗓 ") },
            countdownText = countdownText,
            weekEndAtMillis = weekEndAt,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BoardState(
            dateRangeLabel = "",
            entries = emptyList(),
            hasFriends = false,
        ),
    )

    fun refreshWeekState() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val uid = sessionManager.getUserId().orEmpty()
            if (uid.isBlank()) return@launch

            val friendIds = friendDao.observeFriendIds(uid).first()
            if (friendIds.isEmpty()) {
                applyFallbackWeekUi()
                return@launch
            }

            _isWeekPreparing.value = true
            var pendingResult: WeekResultSummary? = null
            try {
                val participantIds = (friendIds + uid).distinct()
                val allUsers = userDao.observeAll().first()
                val allEntries = dailyTaskEntryDao.observeAll().first()
                val preferences = userPreferencesDao.observe(uid).first()
                val isOnline = connectivityMonitor.isOnline.first()

                val profiles = allUsers
                    .filter { it.uid in participantIds }
                    .associate { user -> user.uid to user.name.ifBlank { user.uid } }
                val avatars = allUsers
                    .filter { it.uid in participantIds }
                    .associate { user -> user.uid to user.toDomain().avatar }
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

                val preparation = weekCycleUseCase.prepareBoardWeekState(
                    uid = uid,
                    todayKey = todayKey,
                    participantIds = participantIds,
                    participantProfiles = profiles,
                    participantAvatars = avatars,
                    histories = histories,
                    primaryTaskIds = primaryIds,
                    youLabel = youLabel,
                    isOnline = isOnline,
                )

                preparation.activeWeek?.let { activeWeek ->
                    applyActiveWeekUi(activeWeek.title, activeWeek.countdownText, activeWeek.endAtMillis)
                } ?: applyFallbackWeekUi()

                pendingResult = preparation.pendingWeekResult
            } finally {
                _isWeekPreparing.value = false
            }

            pendingResult?.let { result ->
                _weekResultEvent.emit(result)
            }
        }
    }

    private fun applyActiveWeekUi(title: String, countdownText: String?, endAtMillis: Long) {
        _weekTitle.value = title
        _countdownText.value = countdownText
        _weekEndAtMillis.value = endAtMillis
        startCountdownTicker(endAtMillis)
    }

    private fun applyFallbackWeekUi() {
        val fallback = WeekCycleUtils.buildWeekCycleDefaults(todayKey)
        applyActiveWeekUi(
            title = fallback.title,
            countdownText = WeekCycleUtils.formatCountdown(fallback.endAt),
            endAtMillis = fallback.endAt,
        )
    }

    private fun startCountdownTicker(endAtMillis: Long) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (isActive) {
                _countdownText.update {
                    WeekCycleUtils.formatCountdown(endAtMillis)
                }
                delay(60_000L)
            }
        }
    }
}
