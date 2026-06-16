package com.parhar.noor.di

import android.app.Application
import androidx.room.Room
import com.google.gson.Gson
import com.parhar.noor.data.local.LocalDataStore
import com.parhar.noor.data.local.NoorDatabase
import com.parhar.noor.data.remote.firebase.FirebaseCatalogRemoteDataSource
import com.parhar.noor.data.remote.firebase.FirebaseUserRemoteDataSource
import com.parhar.noor.data.repository.CatalogRepository
import com.parhar.noor.data.repository.FavoriteRepository
import com.parhar.noor.data.repository.FriendsRepository
import com.parhar.noor.data.repository.LeaderboardRepository
import com.parhar.noor.data.repository.UserProfileRepository
import com.parhar.noor.data.repository.UserTaskRepository
import com.parhar.noor.data.repository.impl.CatalogRepositoryImpl
import com.parhar.noor.data.repository.impl.FavoriteRepositoryImpl
import com.parhar.noor.data.repository.impl.FriendsRepositoryImpl
import com.parhar.noor.data.repository.impl.LeaderboardRepositoryImpl
import com.parhar.noor.data.repository.impl.UserProfileRepositoryImpl
import com.parhar.noor.data.repository.impl.UserTaskRepositoryImpl
import com.parhar.noor.data.sync.ConnectivityMonitor
import com.parhar.noor.data.sync.SyncCoordinator
import com.parhar.noor.domain.usecase.LeaderboardUseCase
import com.parhar.noor.domain.usecase.StreakSyncUseCase
import com.parhar.noor.domain.usecase.TaskStatsUseCase
import com.parhar.noor.utils.SessionManager

class AppContainer(private val application: Application) {

    private val appContext = application.applicationContext
    private val gson = Gson()

    val sessionManager: SessionManager = SessionManager(appContext)

    private val database: NoorDatabase = Room.databaseBuilder(
        appContext,
        NoorDatabase::class.java,
        "noor.db",
    ).build()

    val connectivityMonitor: ConnectivityMonitor = ConnectivityMonitor(appContext)

    private val catalogRemote = FirebaseCatalogRemoteDataSource()
    private val userRemote = FirebaseUserRemoteDataSource()

    private val localDataStore = LocalDataStore(
        userDao = database.userDao(),
        friendDao = database.friendDao(),
        categoryDao = database.categoryDao(),
        taskDefinitionDao = database.taskDefinitionDao(),
        dailyTaskEntryDao = database.dailyTaskEntryDao(),
        favoriteBannerDao = database.favoriteBannerDao(),
        syncMetadataDao = database.syncMetadataDao(),
    )

    val syncCoordinator: SyncCoordinator = SyncCoordinator(
        catalogRemote = catalogRemote,
        userRemote = userRemote,
        localDataStore = localDataStore,
        userDao = database.userDao(),
        friendDao = database.friendDao(),
        taskDefinitionDao = database.taskDefinitionDao(),
        dailyTaskEntryDao = database.dailyTaskEntryDao(),
        syncOutboxDao = database.syncOutboxDao(),
        connectivityMonitor = connectivityMonitor,
        gson = gson,
    )

    private val taskStatsUseCase = TaskStatsUseCase()
    private val leaderboardUseCase = LeaderboardUseCase()
    private val streakSyncUseCase = StreakSyncUseCase(
        dailyTaskEntryDao = database.dailyTaskEntryDao(),
        userPreferencesDao = database.userPreferencesDao(),
        userRemote = userRemote,
    )

    val catalogRepository: CatalogRepository = CatalogRepositoryImpl(
        categoryDao = database.categoryDao(),
        taskDefinitionDao = database.taskDefinitionDao(),
        syncCoordinator = syncCoordinator,
    )

    val userTaskRepository: UserTaskRepository = UserTaskRepositoryImpl(
        categoryDao = database.categoryDao(),
        taskDefinitionDao = database.taskDefinitionDao(),
        dailyTaskEntryDao = database.dailyTaskEntryDao(),
        userPreferencesDao = database.userPreferencesDao(),
        syncCoordinator = syncCoordinator,
        taskStatsUseCase = taskStatsUseCase,
    )

    val userProfileRepository: UserProfileRepository = UserProfileRepositoryImpl(
        userDao = database.userDao(),
        userRemote = userRemote,
        syncCoordinator = syncCoordinator,
        connectivityMonitor = connectivityMonitor,
    )

    val friendsRepository: FriendsRepository = FriendsRepositoryImpl(
        friendDao = database.friendDao(),
        userDao = database.userDao(),
        userRemote = userRemote,
        syncCoordinator = syncCoordinator,
        connectivityMonitor = connectivityMonitor,
    )

    val favoriteRepository: FavoriteRepository = FavoriteRepositoryImpl(
        favoriteBannerDao = database.favoriteBannerDao(),
    )

    val leaderboardRepository: LeaderboardRepository = LeaderboardRepositoryImpl(
        friendDao = database.friendDao(),
        userDao = database.userDao(),
        dailyTaskEntryDao = database.dailyTaskEntryDao(),
        userPreferencesDao = database.userPreferencesDao(),
        connectivityMonitor = connectivityMonitor,
        leaderboardUseCase = leaderboardUseCase,
    )

    val viewModelFactory: ViewModelFactory = ViewModelFactory(this, application)

    suspend fun onUserLoggedIn(uid: String) {
        migratePrimaryTaskIds(uid)
        syncCoordinator.startLeaderboardSync(uid)
    }

    suspend fun bootstrapOnSplash(uid: String?): Boolean {
        if (!uid.isNullOrBlank()) {
            migratePrimaryTaskIds(uid)
        }
        return syncCoordinator.bootstrapFromRemote(uid).isSuccess
    }

    suspend fun syncSteakOnSplash(uid: String) {
        if (!connectivityMonitor.checkOnline()) return
        streakSyncUseCase.syncSteakForUser(
            userUid = uid,
            todayKey = todayDateKey(),
        )
    }

    private fun todayDateKey(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
    }

    fun onUserLoggedOut() {
        syncCoordinator.stopSync()
    }

    private suspend fun migratePrimaryTaskIds(uid: String) {
        val legacyIds = sessionManager.getPrimaryTaskIds()
        if (legacyIds.isEmpty()) return
        val existing = database.userPreferencesDao().get(uid)?.primaryTaskIds.orEmpty()
        if (existing.isBlank()) {
            database.userPreferencesDao().upsert(
                com.parhar.noor.data.local.entity.UserPreferencesEntity(
                    userUid = uid,
                    primaryTaskIds = legacyIds.joinToString(","),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }
}
