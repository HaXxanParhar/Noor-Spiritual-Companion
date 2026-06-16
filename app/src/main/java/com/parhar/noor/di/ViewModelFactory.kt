package com.parhar.noor.di

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.parhar.noor.ui.admin.AdminViewModel
import com.parhar.noor.ui.auth.AuthViewModel
import com.parhar.noor.ui.board.BoardViewModel
import com.parhar.noor.ui.board.InviteFriendsViewModel
import com.parhar.noor.ui.home.HomeViewModel
import com.parhar.noor.ui.main.MainViewModel

class ViewModelFactory(
    private val appContainer: AppContainer,
    private val application: Application,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(
                userTaskRepository = appContainer.userTaskRepository,
                userProfileRepository = appContainer.userProfileRepository,
                sessionManager = appContainer.sessionManager,
            ) as T
            modelClass.isAssignableFrom(BoardViewModel::class.java) -> BoardViewModel(
                application = application,
                leaderboardRepository = appContainer.leaderboardRepository,
                sessionManager = appContainer.sessionManager,
            ) as T
            modelClass.isAssignableFrom(MainViewModel::class.java) -> MainViewModel(
                favoriteRepository = appContainer.favoriteRepository,
                sessionManager = appContainer.sessionManager,
            ) as T
            modelClass.isAssignableFrom(InviteFriendsViewModel::class.java) -> InviteFriendsViewModel(
                friendsRepository = appContainer.friendsRepository,
                userProfileRepository = appContainer.userProfileRepository,
                sessionManager = appContainer.sessionManager,
                connectivityMonitor = appContainer.connectivityMonitor,
            ) as T
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> AuthViewModel(
                userProfileRepository = appContainer.userProfileRepository,
                sessionManager = appContainer.sessionManager,
            ) as T
            modelClass.isAssignableFrom(AdminViewModel::class.java) -> AdminViewModel(
                catalogRepository = appContainer.catalogRepository,
            ) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
