package com.parhar.noor.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parhar.noor.data.local.mapper.DailyTaskKeys
import com.parhar.noor.data.repository.UserProfileRepository
import com.parhar.noor.data.repository.UserTaskRepository
import com.parhar.noor.domain.model.DailyTaskState
import com.parhar.noor.domain.model.HomeTaskSection
import com.parhar.noor.domain.model.TaskItem
import com.parhar.noor.domain.model.UserTaskStats
import com.parhar.noor.utils.DateKeyUtils
import com.parhar.noor.utils.SessionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val userTaskRepository: UserTaskRepository,
    private val userProfileRepository: UserProfileRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _pointsInfoEvent = MutableSharedFlow<Int>()
    val pointsInfoEvent: SharedFlow<Int> = _pointsInfoEvent.asSharedFlow()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    private val _userName = MutableStateFlow<String?>(null)
    val userName: StateFlow<String?> = _userName.asStateFlow()

    private val _userProfile = MutableStateFlow<com.parhar.noor.domain.model.UserProfile?>(null)
    val userProfile: StateFlow<com.parhar.noor.domain.model.UserProfile?> = _userProfile.asStateFlow()

    private val _showCannotOfferOption = MutableStateFlow(false)
    val showCannotOfferOption: StateFlow<Boolean> = _showCannotOfferOption.asStateFlow()

    private val _cannotOffer = MutableStateFlow(false)
    val cannotOffer: StateFlow<Boolean> = _cannotOffer.asStateFlow()

    val isPrimaryPrayerLocked: StateFlow<Boolean> = _cannotOffer.asStateFlow()

    val sections: StateFlow<List<HomeTaskSection>> = userTaskRepository.observeHomeSections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedDateKey = MutableStateFlow(DateKeyUtils.todayKey())
    val selectedDateKey: StateFlow<String> = _selectedDateKey.asStateFlow()

    val canCheckSelectedDate: StateFlow<Boolean> = _selectedDateKey
        .map { DateKeyUtils.canCheckTasksForDate(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _isTasksLoading = MutableStateFlow(false)
    val isTasksLoading: StateFlow<Boolean> = _isTasksLoading.asStateFlow()

    private var cachedUid: String? = null
    private var cachedPrimaryTaskIds: Set<String> = emptySet()
    private val cachedDailyState = MutableStateFlow(DailyTaskState(emptyMap(), emptySet()))
    private val cachedStats = MutableStateFlow(UserTaskStats(0, 0, 0, 0))

    val taskStats: StateFlow<UserTaskStats> = cachedStats.asStateFlow()
    val todayTaskState: StateFlow<DailyTaskState> = cachedDailyState.asStateFlow()

    fun setUserId(uid: String) {
        if (uid == cachedUid) return
        cachedUid = uid
        observeForUser(uid)
    }

    fun goToPreviousDate() {
        _selectedDateKey.value = DateKeyUtils.offsetDateKey(_selectedDateKey.value, -1)
    }

    fun goToNextDate() {
        _selectedDateKey.value = DateKeyUtils.offsetDateKey(_selectedDateKey.value, 1)
    }

    private fun observeForUser(uid: String) {
        viewModelScope.launch {
            cachedPrimaryTaskIds = userTaskRepository.getPrimaryTaskIds(uid)
        }
        viewModelScope.launch {
            userProfileRepository.observeUser(uid).collect { profile ->
                _userProfile.value = profile
                _userName.value = profile?.name?.takeIf { it.isNotBlank() }
                _showCannotOfferOption.value = profile?.gender.equals("female", ignoreCase = true)
            }
        }
        viewModelScope.launch {
            _selectedDateKey.collectLatest { dateKey ->
                _isTasksLoading.value = true
                runCatching {
                    userTaskRepository.refreshDailyTasksFromRemote(uid, dateKey)
                }.onFailure { error ->
                    _errorMessage.emit(error.message ?: "Unable to load tasks for this date.")
                }
                _isTasksLoading.value = false

                userTaskRepository.observeDailyTaskState(uid, dateKey).collect { state ->
                    cachedDailyState.value = state
                    _cannotOffer.value = (state.taskPoints[DailyTaskKeys.CANNOT_OFFER_TASK_ID] ?: 0) > 0
                }
            }
        }
        viewModelScope.launch {
            userTaskRepository.observeTaskStats(uid, DateKeyUtils.todayKey()).collect { stats ->
                cachedStats.value = stats
            }
        }
        viewModelScope.launch {
            sections.collect { sectionList ->
                savePrimaryTaskIdsFromSections(uid, sectionList)
            }
        }
    }

    fun toggleTask(taskId: String, points: Int, isCurrentlyChecked: Boolean) {
        if (_cannotOffer.value && cachedPrimaryTaskIds.contains(taskId)) return
        val uid = cachedUid ?: sessionManager.getUserId() ?: return
        val dateKey = _selectedDateKey.value
        if (!DateKeyUtils.canCheckTasksForDate(dateKey)) return
        viewModelScope.launch {
            runCatching {
                val todayPointsBeforeToggle = cachedDailyState.value.taskPoints
                    .filter { (taskIdKey, storedPoints) ->
                        storedPoints > 0 && !DailyTaskKeys.isCannotOfferKey(taskIdKey)
                    }
                    .values
                    .sum()

                userTaskRepository.toggleTask(
                    userUid = uid,
                    dateKey = dateKey,
                    taskId = taskId,
                    points = points,
                    checked = !isCurrentlyChecked,
                )
                if (!isCurrentlyChecked && DateKeyUtils.isToday(dateKey)) {
                    val addedPoints = points.coerceAtLeast(0)
                    _pointsInfoEvent.emit(todayPointsBeforeToggle + addedPoints)
                }
            }.onFailure { error ->
                _errorMessage.emit(error.message ?: "Unable to update today's tasks.")
            }
        }
    }

    fun toggleCannotOffer(checked: Boolean) {
        val uid = cachedUid ?: sessionManager.getUserId() ?: return
        val dateKey = _selectedDateKey.value
        if (!DateKeyUtils.canCheckTasksForDate(dateKey)) return
        viewModelScope.launch {
            runCatching {
                val primaryTasks = primaryTasksFromSections(sections.value)
                userTaskRepository.toggleCannotOfferForDate(
                    userUid = uid,
                    dateKey = dateKey,
                    checked = checked,
                    primaryTasks = primaryTasks,
                )
            }.onFailure { error ->
                _errorMessage.emit(error.message ?: "Unable to update prayer preference.")
            }
        }
    }

    private suspend fun savePrimaryTaskIdsFromSections(uid: String, sectionList: List<HomeTaskSection>) {
        val primaryIds = primaryTaskIdsFromSections(sectionList)
        if (primaryIds.isNotEmpty() && primaryIds != cachedPrimaryTaskIds) {
            cachedPrimaryTaskIds = primaryIds
            userTaskRepository.savePrimaryTaskIds(uid, primaryIds)
        }
    }

    private fun primaryTasksFromSections(sectionList: List<HomeTaskSection>): List<TaskItem> {
        val primarySection = sectionList.firstOrNull { section ->
            section.category.equals(PRIMARY_SECTION_NAME, ignoreCase = true)
        } ?: return emptyList()

        val primaryIds = cachedPrimaryTaskIds.ifEmpty { primaryTaskIdsFromSections(sectionList) }
        if (primaryIds.isEmpty()) return primarySection.tasks
        return primarySection.tasks.filter { taskItem -> taskItem.id in primaryIds }
    }

    private fun primaryTaskIdsFromSections(sectionList: List<HomeTaskSection>): Set<String> {
        val primarySection = sectionList.firstOrNull { section ->
            section.category.equals(PRIMARY_SECTION_NAME, ignoreCase = true)
        } ?: return emptySet()

        return primarySection.tasks.map { it.id }.toSet()
    }

    private companion object {
        private const val PRIMARY_SECTION_NAME = "Primary"
    }
}
