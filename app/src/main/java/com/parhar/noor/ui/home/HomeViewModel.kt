package com.parhar.noor.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parhar.noor.data.repository.UserProfileRepository
import com.parhar.noor.data.repository.UserTaskRepository
import com.parhar.noor.domain.model.DailyTaskState
import com.parhar.noor.domain.model.HomeTaskSection
import com.parhar.noor.domain.model.UserTaskStats
import com.parhar.noor.utils.SessionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    val sections: StateFlow<List<HomeTaskSection>> = userTaskRepository.observeHomeSections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val todayKey: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private var cachedUid: String? = null
    private var cachedPrimaryTaskIds: Set<String> = emptySet()
    private val cachedDailyState = MutableStateFlow(DailyTaskState(emptyMap(), emptySet()))
    private val cachedStats = MutableStateFlow(UserTaskStats(0, 0, 0, 0))

    val taskStats: StateFlow<UserTaskStats> = cachedStats.asStateFlow()
    val todayTaskState: StateFlow<DailyTaskState> = cachedDailyState.asStateFlow()

    init {
        sessionManager.getUserId()?.takeIf { it.isNotBlank() }?.let(::setUserId)
    }

    fun setUserId(uid: String) {
        if (uid == cachedUid) return
        cachedUid = uid
        observeForUser(uid)
    }

    private fun observeForUser(uid: String) {
        viewModelScope.launch {
            userProfileRepository.observeUser(uid).collect { profile ->
                _userName.value = profile?.name?.takeIf { it.isNotBlank() }
            }
        }
        viewModelScope.launch {
            userTaskRepository.observeDailyTaskState(uid, todayKey).collect { state ->
                cachedDailyState.value = state
            }
        }
        viewModelScope.launch {
            userTaskRepository.observeTaskStats(uid, todayKey).collect { stats ->
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
        val uid = cachedUid ?: sessionManager.getUserId() ?: return
        viewModelScope.launch {
            runCatching {
                val todayPointsBeforeToggle = cachedDailyState.value.taskPoints
                    .filterValues { storedPoints -> storedPoints > 0 }
                    .values
                    .sum()

                userTaskRepository.toggleTask(
                    userUid = uid,
                    dateKey = todayKey,
                    taskId = taskId,
                    points = points,
                    checked = !isCurrentlyChecked,
                )
                if (!isCurrentlyChecked) {
                    val addedPoints = points.coerceAtLeast(0)
                    _pointsInfoEvent.emit(todayPointsBeforeToggle + addedPoints)
                }
            }.onFailure { error ->
                _errorMessage.emit(error.message ?: "Unable to update today's tasks.")
            }
        }
    }

    private suspend fun savePrimaryTaskIdsFromSections(uid: String, sectionList: List<HomeTaskSection>) {
        val primarySection = sectionList.firstOrNull { section ->
            section.category.equals(PRIMARY_SECTION_NAME, ignoreCase = true)
        } ?: return

        val primaryIds = primarySection.tasks
            .filter { taskItem ->
                PRIMARY_TASK_NAMES.contains(taskItem.task.name.trim().lowercase(Locale.getDefault()))
            }
            .map { it.id }
            .toSet()

        if (primaryIds.isNotEmpty() && primaryIds != cachedPrimaryTaskIds) {
            cachedPrimaryTaskIds = primaryIds
            userTaskRepository.savePrimaryTaskIds(uid, primaryIds)
        }
    }

    private companion object {
        private const val PRIMARY_SECTION_NAME = "Primary"
        private val PRIMARY_TASK_NAMES = setOf("fajr", "zuhr", "asr", "maghrib", "isha")
    }
}
