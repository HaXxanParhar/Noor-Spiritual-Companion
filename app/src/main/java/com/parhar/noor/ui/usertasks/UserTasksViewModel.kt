package com.parhar.noor.ui.usertasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parhar.noor.data.repository.FriendsRepository
import com.parhar.noor.data.repository.UserProfileRepository
import com.parhar.noor.data.repository.UserTaskRepository
import com.parhar.noor.domain.model.DailyTaskState
import com.parhar.noor.domain.model.UserPrivacy
import com.parhar.noor.utils.DateKeyUtils
import com.parhar.noor.utils.PrivacyAccessHelper
import com.parhar.noor.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserTasksViewModel(
    private val targetUserId: String,
    private val userProfileRepository: UserProfileRepository,
    private val userTaskRepository: UserTaskRepository,
    private val friendsRepository: FriendsRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        UserTasksUiState(selectedDateKey = DateKeyUtils.todayKey()),
    )
    val uiState: StateFlow<UserTasksUiState> = _uiState.asStateFlow()

    private val _selectedDateKey = MutableStateFlow(DateKeyUtils.todayKey())
    private val _showTasks = MutableStateFlow(false)

    private var privacy: UserPrivacy = UserPrivacy()
    private var isFriend: Boolean = false
    private var viewerUid: String = ""

    init {
        viewerUid = sessionManager.getUserId().orEmpty()
        viewModelScope.launch {
            isFriend = friendsRepository.isFriend(viewerUid, targetUserId)
            val profile = userProfileRepository.getUser(targetUserId)
            privacy = profile?.privacy ?: UserPrivacy()
            _uiState.update {
                it.copy(
                    name = profile?.name.orEmpty().ifBlank { "User" },
                    avatar = profile?.avatar,
                )
            }
            applyAccessState(_selectedDateKey.value)
        }
        viewModelScope.launch {
            userTaskRepository.observeTaskStats(targetUserId, DateKeyUtils.todayKey()).collect { stats ->
                _uiState.update {
                    it.copy(
                        streak = stats.streak,
                        weeklyPoints = stats.weeklyPoints,
                        allTimePoints = stats.allTimePoints,
                    )
                }
            }
        }
        viewModelScope.launch {
            userTaskRepository.observeHomeSections().collect { sections ->
                _uiState.update { it.copy(sections = sections) }
            }
        }
        viewModelScope.launch {
            combine(_selectedDateKey, _showTasks) { dateKey, showTasks -> dateKey to showTasks }
                .collectLatest { (dateKey, showTasks) ->
                    if (!showTasks) {
                        _uiState.update {
                            it.copy(
                                taskState = DailyTaskState(emptyMap(), emptySet()),
                                isLoading = false,
                            )
                        }
                        return@collectLatest
                    }
                    _uiState.update { it.copy(isLoading = true) }
                    userTaskRepository.refreshDailyTasksFromRemote(targetUserId, dateKey)
                    userTaskRepository.observeDailyTaskState(targetUserId, dateKey).collectLatest { taskState ->
                        _uiState.update { it.copy(taskState = taskState, isLoading = false) }
                    }
                }
        }
    }

    private fun applyAccessState(dateKey: String) {
        val effectiveDateKey = resolveDateKey(dateKey)
        if (effectiveDateKey != _selectedDateKey.value) {
            _selectedDateKey.value = effectiveDateKey
        }

        val canViewHistory = PrivacyAccessHelper.canBrowseTaskHistory(
            viewerUid = viewerUid,
            targetUid = targetUserId,
            privacy = privacy,
            isFriend = isFriend,
        )
        val canViewTasks = PrivacyAccessHelper.canViewTasksForDate(
            viewerUid = viewerUid,
            targetUid = targetUserId,
            privacy = privacy,
            isFriend = isFriend,
            dateKey = effectiveDateKey,
        )

        _showTasks.value = canViewTasks
        _uiState.update {
            it.copy(
                selectedDateKey = effectiveDateKey,
                showTasks = canViewTasks,
                isPrivate = !canViewTasks,
                showDateNavigator = canViewHistory,
                canGoPrevious = canViewHistory,
                canGoNext = canViewHistory && !DateKeyUtils.isToday(effectiveDateKey),
            )
        }
    }

    private fun resolveDateKey(dateKey: String): String {
        val daysFromToday = DateKeyUtils.daysFromToday(dateKey)
        if (daysFromToday != null && daysFromToday > 0) {
            return DateKeyUtils.todayKey()
        }

        val canViewHistory = PrivacyAccessHelper.canBrowseTaskHistory(
            viewerUid = viewerUid,
            targetUid = targetUserId,
            privacy = privacy,
            isFriend = isFriend,
        )
        if (canViewHistory) {
            return dateKey
        }

        if (viewerUid != targetUserId) {
            return DateKeyUtils.todayKey()
        }

        return DateKeyUtils.clampToAllowedHomeDate(dateKey)
    }

    fun goToPreviousDate() {
        if (!_uiState.value.canGoPrevious) return
        val previous = DateKeyUtils.offsetDateKey(_selectedDateKey.value, -1)
        _selectedDateKey.value = previous
        applyAccessState(previous)
    }

    fun goToNextDate() {
        if (!_uiState.value.canGoNext) return
        val next = DateKeyUtils.offsetDateKey(_selectedDateKey.value, 1)
        _selectedDateKey.value = next
        applyAccessState(next)
    }
}
