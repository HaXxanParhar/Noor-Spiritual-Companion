package com.parhar.noor.di

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.parhar.noor.ui.admin.AdminViewModel
import com.parhar.noor.ui.auth.AuthViewModel
import com.parhar.noor.ui.board.BoardViewModel
import com.parhar.noor.ui.board.InviteFriendsViewModel
import com.parhar.noor.ui.friends.FriendsViewModel
import com.parhar.noor.ui.home.HomeViewModel
import com.parhar.noor.ui.main.MainViewModel
import com.parhar.noor.ui.profile.CreateAvatarViewModel
import com.parhar.noor.ui.profile.EditProfileViewModel
import com.parhar.noor.ui.profile.ProfileViewModel
import com.parhar.noor.ui.privacy.PrivacyViewModel

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
                weekRepository = appContainer.weekRepository,
                weekCycleUseCase = appContainer.weekCycleUseCaseAccessor,
                friendDao = appContainer.databaseAccessor.friendDao(),
                userDao = appContainer.databaseAccessor.userDao(),
                dailyTaskEntryDao = appContainer.databaseAccessor.dailyTaskEntryDao(),
                userPreferencesDao = appContainer.databaseAccessor.userPreferencesDao(),
                connectivityMonitor = appContainer.connectivityMonitor,
                sessionManager = appContainer.sessionManager,
            ) as T
            modelClass.isAssignableFrom(MainViewModel::class.java) -> MainViewModel(
                favoriteRepository = appContainer.favoriteRepository,
                remindersRepository = appContainer.remindersRepository,
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
                trophiesRepository = appContainer.trophiesRepository,
                ayatsRepository = appContainer.ayatsRepository,
            ) as T
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> ProfileViewModel(
                userProfileRepository = appContainer.userProfileRepository,
                friendsRepository = appContainer.friendsRepository,
                weekRepository = appContainer.weekRepository,
                dailyTaskEntryDao = appContainer.databaseAccessor.dailyTaskEntryDao(),
                userPreferencesDao = appContainer.databaseAccessor.userPreferencesDao(),
                taskStatsUseCase = appContainer.taskStatsUseCaseAccessor,
                sessionManager = appContainer.sessionManager,
            ) as T
            modelClass.isAssignableFrom(EditProfileViewModel::class.java) -> EditProfileViewModel(
                userProfileRepository = appContainer.userProfileRepository,
                sessionManager = appContainer.sessionManager,
            ) as T
            modelClass.isAssignableFrom(CreateAvatarViewModel::class.java) -> CreateAvatarViewModel(
                userProfileRepository = appContainer.userProfileRepository,
                sessionManager = appContainer.sessionManager,
            ) as T
            modelClass.isAssignableFrom(FriendsViewModel::class.java) -> FriendsViewModel(
                friendsRepository = appContainer.friendsRepository,
                remindersRepository = appContainer.remindersRepository,
                userProfileRepository = appContainer.userProfileRepository,
                sessionManager = appContainer.sessionManager,
            ) as T
            modelClass.isAssignableFrom(PrivacyViewModel::class.java) -> PrivacyViewModel(
                userProfileRepository = appContainer.userProfileRepository,
                sessionManager = appContainer.sessionManager,
            ) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
