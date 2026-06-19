package com.parhar.noor.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parhar.noor.data.local.dao.DailyTaskEntryDao
import com.parhar.noor.data.local.dao.UserPreferencesDao
import com.parhar.noor.data.local.mapper.EntityMappers.toTaskHistory
import com.parhar.noor.data.repository.FriendsRepository
import com.parhar.noor.data.repository.UserProfileRepository
import com.parhar.noor.data.repository.WeekRepository
import com.parhar.noor.domain.model.UserAvatar
import com.parhar.noor.domain.usecase.TaskStatsUseCase
import com.parhar.noor.utils.MemberSinceFormatter
import com.parhar.noor.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ProfileUiState(
    val isLoading: Boolean = true,
    val isOwnProfile: Boolean = false,
    val name: String = "",
    val memberSince: String = "",
    val avatar: UserAvatar? = null,
    val streak: Int = 0,
    val allTimePoints: Int = 0,
    val friendsCount: Int = 0,
    val weeklyPoints: Int = 0,
    val goldTrophies: Int = 2,
    val silverTrophies: Int = 1,
    val bronzeTrophies: Int = 0,
    val firstPlaceMedals: Int = 0,
    val secondPlaceMedals: Int = 0,
    val thirdPlaceMedals: Int = 0,
    val top5Finishes: Int = 0,
    val top10Finishes: Int = 0,
    val tierCurrentPoints: Int = 62,
    val tierBronzeTarget: Int = 35,
    val tierSilverTarget: Int = 50,
    val tierGoldTarget: Int = 65,
    val tierMaxPoints: Int = 154,
)

class ProfileViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val friendsRepository: FriendsRepository,
    private val weekRepository: WeekRepository,
    private val dailyTaskEntryDao: DailyTaskEntryDao,
    private val userPreferencesDao: UserPreferencesDao,
    private val taskStatsUseCase: TaskStatsUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _isActionInProgress = MutableStateFlow(false)
    val isActionInProgress: StateFlow<Boolean> = _isActionInProgress.asStateFlow()

    private val _friendRemoved = MutableStateFlow(false)
    val friendRemoved: StateFlow<Boolean> = _friendRemoved.asStateFlow()

    private var targetUid: String = ""
    private var currentUid: String = ""

    fun loadProfile(targetUserId: String) {
        targetUid = targetUserId
        currentUid = sessionManager.getUserId().orEmpty()
        viewModelScope.launch {
            combine(
                userProfileRepository.observeUser(targetUid),
                friendsRepository.observeFriendIds(targetUid),
                dailyTaskEntryDao.observeAll(),
                userPreferencesDao.observe(targetUid),
            ) { profile, friendIds, allEntries, preferences ->
                val todayKey = todayDateKey()
                val history = allEntries
                    .filter { it.userUid == targetUid }
                    .toTaskHistory()
                val primaryIds = preferences
                    ?.primaryTaskIds
                    ?.split(",")
                    ?.filter { it.isNotBlank() }
                    ?.toSet()
                    .orEmpty()
                val stats = taskStatsUseCase.calculateStats(
                    taskHistory = history,
                    primaryTaskIds = primaryIds,
                    todayKey = todayKey,
                    todayTaskPoints = history[todayKey].orEmpty(),
                    checkedTaskIds = history[todayKey]?.keys.orEmpty(),
                    taskPointLookup = emptyMap(),
                )
                val earliestTaskDate = history.keys.minOrNull()
                ProfileUiState(
                    isLoading = false,
                    isOwnProfile = targetUid == currentUid,
                    name = profile?.name.orEmpty().ifBlank { "User" },
                    memberSince = MemberSinceFormatter.format(
                        createdAtMillis = profile?.createdAt ?: 0L,
                        earliestTaskDateKey = earliestTaskDate,
                    ),
                    avatar = profile?.avatar,
                    streak = stats.streak,
                    allTimePoints = stats.allTimePoints,
                    friendsCount = friendIds.size,
                    weeklyPoints = stats.weeklyPoints,
                )
            }.collect { state ->
                val current = _uiState.value
                _uiState.value = state.copy(
                    tierCurrentPoints = statsTierPoints(state.weeklyPoints),
                    firstPlaceMedals = current.firstPlaceMedals,
                    secondPlaceMedals = current.secondPlaceMedals,
                    thirdPlaceMedals = current.thirdPlaceMedals,
                    top5Finishes = current.top5Finishes,
                    top10Finishes = current.top10Finishes,
                )
            }
        }
        refreshMedals()
    }

    private fun refreshMedals() {
        if (targetUid.isBlank()) return
        viewModelScope.launch {
            runCatching {
                weekRepository.fetchUserMedals(targetUid)
            }.onSuccess { medals ->
                _uiState.value = _uiState.value.copy(
                    firstPlaceMedals = medals.total1st,
                    secondPlaceMedals = medals.total2nd,
                    thirdPlaceMedals = medals.total3rd,
                    top5Finishes = medals.totalTop5,
                    top10Finishes = medals.totalTop10,
                )
            }
        }
    }

    fun removeFriend() {
        if (currentUid.isBlank() || targetUid.isBlank() || targetUid == currentUid) return
        viewModelScope.launch {
            _isActionInProgress.value = true
            runCatching {
                friendsRepository.removeFriend(currentUid, targetUid)
            }
            _isActionInProgress.value = false
            _friendRemoved.value = true
        }
    }

    fun clearFriendRemoved() {
        _friendRemoved.value = false
    }

    private fun statsTierPoints(weeklyPoints: Int): Int {
        return weeklyPoints.coerceAtMost(154)
    }

    private fun todayDateKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}
