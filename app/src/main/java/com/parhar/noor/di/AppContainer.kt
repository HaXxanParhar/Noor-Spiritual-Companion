package com.parhar.noor.di

import android.app.Application
import androidx.room.Room
import com.google.gson.Gson
import com.parhar.noor.data.admin.AdminRepository
import com.parhar.noor.data.local.LocalDataStore
import com.parhar.noor.data.local.NoorDatabase
import com.parhar.noor.data.local.NoorDatabaseMigrations.MIGRATION_1_2
import com.parhar.noor.data.local.NoorDatabaseMigrations.MIGRATION_2_3
import com.parhar.noor.data.local.NoorDatabaseMigrations.MIGRATION_3_4
import com.parhar.noor.data.local.NoorDatabaseMigrations.MIGRATION_4_5
import com.parhar.noor.data.local.NoorDatabaseMigrations.MIGRATION_5_6
import com.parhar.noor.data.local.NoorDatabaseMigrations.MIGRATION_6_7
import com.parhar.noor.data.local.NoorDatabaseMigrations.MIGRATION_7_8
import com.parhar.noor.data.local.NoorDatabaseMigrations.MIGRATION_8_9
import com.parhar.noor.data.local.NoorDatabaseMigrations.MIGRATION_9_10
import com.parhar.noor.data.remote.firebase.FirebaseAyatsRemoteDataSource
import com.parhar.noor.data.remote.firebase.FirebaseCatalogRemoteDataSource
import com.parhar.noor.data.remote.firebase.FirebaseTrophiesRemoteDataSource
import com.parhar.noor.data.remote.firebase.FirebaseUserRemoteDataSource
import com.parhar.noor.data.repository.AyatsRepository
import com.parhar.noor.data.repository.CatalogRepository
import com.parhar.noor.data.repository.FavoriteRepository
import com.parhar.noor.data.repository.RemindersRepository
import com.parhar.noor.data.repository.FriendsRepository
import com.parhar.noor.data.repository.LeaderboardRepository
import com.parhar.noor.data.repository.TrophiesRepository
import com.parhar.noor.data.repository.UserProfileRepository
import com.parhar.noor.data.repository.UserTaskRepository
import com.parhar.noor.data.repository.WeekRepository
import com.parhar.noor.data.repository.impl.AyatsRepositoryImpl
import com.parhar.noor.data.repository.impl.CatalogRepositoryImpl
import com.parhar.noor.data.repository.impl.FavoriteRepositoryImpl
import com.parhar.noor.data.repository.impl.RemindersRepositoryImpl
import com.parhar.noor.data.repository.impl.FriendsRepositoryImpl
import com.parhar.noor.data.repository.impl.LeaderboardRepositoryImpl
import com.parhar.noor.data.repository.impl.TrophiesRepositoryImpl
import com.parhar.noor.data.repository.impl.UserProfileRepositoryImpl
import com.parhar.noor.data.repository.impl.UserTaskRepositoryImpl
import com.parhar.noor.data.repository.impl.WeekRepositoryImpl
import com.parhar.noor.data.sync.ConnectivityMonitor
import com.parhar.noor.data.sync.SyncCoordinator
import com.parhar.noor.domain.usecase.LeaderboardUseCase
import com.parhar.noor.domain.usecase.StreakSyncUseCase
import com.parhar.noor.domain.usecase.TaskStatsUseCase
import com.parhar.noor.domain.usecase.WeekCycleUseCase
import com.parhar.noor.utils.SessionManager

class AppContainer(private val application: Application) {

    private val appContext = application.applicationContext
    private val gson = Gson()

    val sessionManager: SessionManager = SessionManager(appContext)

    val adminRepository: AdminRepository = AdminRepository()

    private val database: NoorDatabase = Room.databaseBuilder(
        appContext,
        NoorDatabase::class.java,
        "noor.db",
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10).build()

    val databaseAccessor: NoorDatabase
        get() = database

    val connectivityMonitor: ConnectivityMonitor = ConnectivityMonitor(appContext)

    private val catalogRemote = FirebaseCatalogRemoteDataSource()
    private val trophiesRemote = FirebaseTrophiesRemoteDataSource()
    private val ayatsRemote = FirebaseAyatsRemoteDataSource()
    private val userRemote = FirebaseUserRemoteDataSource()

    private val localDataStore = LocalDataStore(
        userDao = database.userDao(),
        friendDao = database.friendDao(),
        categoryDao = database.categoryDao(),
        taskDefinitionDao = database.taskDefinitionDao(),
        dailyTaskEntryDao = database.dailyTaskEntryDao(),
        favoriteBannerDao = database.favoriteBannerDao(),
        syncMetadataDao = database.syncMetadataDao(),
        userPreferencesDao = database.userPreferencesDao(),
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
    val taskStatsUseCaseAccessor: TaskStatsUseCase
        get() = taskStatsUseCase
    private val leaderboardUseCase = LeaderboardUseCase()
    private val streakSyncUseCase = StreakSyncUseCase(
        dailyTaskEntryDao = database.dailyTaskEntryDao(),
        userPreferencesDao = database.userPreferencesDao(),
        userRemote = userRemote,
    )

    val catalogRepository: CatalogRepository = CatalogRepositoryImpl(
        categoryDao = database.categoryDao(),
        taskDefinitionDao = database.taskDefinitionDao(),
        dailyTaskEntryDao = database.dailyTaskEntryDao(),
        syncCoordinator = syncCoordinator,
    )

    val trophiesRepository: TrophiesRepository = TrophiesRepositoryImpl(
        trophiesRemote = trophiesRemote,
    )

    val ayatsRepository: AyatsRepository = AyatsRepositoryImpl(
        ayatsRemote = ayatsRemote,
        ayatDao = database.ayatDao(),
    )

    val weekRepository: WeekRepository = WeekRepositoryImpl(
        userRemote = userRemote,
    )

    private val weekCycleUseCase = WeekCycleUseCase(
        weekRepository = weekRepository,
        leaderboardUseCase = leaderboardUseCase,
    )

    val weekCycleUseCaseAccessor: WeekCycleUseCase
        get() = weekCycleUseCase

    val userTaskRepository: UserTaskRepository = UserTaskRepositoryImpl(
        categoryDao = database.categoryDao(),
        taskDefinitionDao = database.taskDefinitionDao(),
        dailyTaskEntryDao = database.dailyTaskEntryDao(),
        userPreferencesDao = database.userPreferencesDao(),
        syncCoordinator = syncCoordinator,
        taskStatsUseCase = taskStatsUseCase,
        userRemote = userRemote,
        localDataStore = localDataStore,
    )

    val userProfileRepository: UserProfileRepository = UserProfileRepositoryImpl(
        userDao = database.userDao(),
        userRemote = userRemote,
        syncCoordinator = syncCoordinator,
        connectivityMonitor = connectivityMonitor,
    )

    val remindersRepository: RemindersRepository = RemindersRepositoryImpl(
        userRemote = userRemote,
    )

    val friendsRepository: FriendsRepository = FriendsRepositoryImpl(
        friendDao = database.friendDao(),
        userDao = database.userDao(),
        userRemote = userRemote,
        syncCoordinator = syncCoordinator,
        connectivityMonitor = connectivityMonitor,
        sessionManager = sessionManager,
        remindersRepository = remindersRepository,
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
        cacheFriendCount(uid)
        startLeaderboardSyncIfNeeded(uid)
    }

    suspend fun cacheFriendCount(uid: String): Int {
        val count = friendsRepository.getFriendIds(uid).size
        sessionManager.saveFriendCount(count)
        return count
    }

    fun startLeaderboardSyncIfNeeded(uid: String) {
        if (sessionManager.getFriendCount() > 0) {
            syncCoordinator.startLeaderboardSync(uid)
        }
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
