package com.parhar.noor.ui.usertasks

import com.parhar.noor.domain.model.DailyTaskState
import com.parhar.noor.domain.model.HomeTaskSection
import com.parhar.noor.domain.model.UserAvatar

data class UserTasksUiState(
    val name: String = "",
    val avatar: UserAvatar? = null,
    val streak: Int = 0,
    val weeklyPoints: Int = 0,
    val allTimePoints: Int = 0,
    val selectedDateKey: String = "",
    val showDateNavigator: Boolean = false,
    val canGoPrevious: Boolean = false,
    val canGoNext: Boolean = false,
    val showTasks: Boolean = false,
    val isPrivate: Boolean = false,
    val isLoading: Boolean = true,
    val sections: List<HomeTaskSection> = emptyList(),
    val taskState: DailyTaskState = DailyTaskState(emptyMap(), emptySet()),
)
