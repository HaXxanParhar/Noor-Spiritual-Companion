package com.parhar.noor.ui.usertasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.parhar.noor.di.AppContainer

class UserTasksViewModelFactory(
    private val targetUserId: String,
    private val appContainer: AppContainer,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserTasksViewModel::class.java)) {
            return UserTasksViewModel(
                targetUserId = targetUserId,
                userProfileRepository = appContainer.userProfileRepository,
                userTaskRepository = appContainer.userTaskRepository,
                friendsRepository = appContainer.friendsRepository,
                sessionManager = appContainer.sessionManager,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
