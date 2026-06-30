package com.parhar.noor.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.parhar.noor.data.local.dao.AyatDao
import com.parhar.noor.data.local.dao.CategoryDao
import com.parhar.noor.data.local.dao.DailyTaskEntryDao
import com.parhar.noor.data.local.dao.FavoriteBannerDao
import com.parhar.noor.data.local.dao.FriendDao
import com.parhar.noor.data.local.dao.SyncMetadataDao
import com.parhar.noor.data.local.dao.SyncOutboxDao
import com.parhar.noor.data.local.dao.TaskDefinitionDao
import com.parhar.noor.data.local.dao.UserDao
import com.parhar.noor.data.local.dao.UserPreferencesDao
import com.parhar.noor.data.local.entity.AyatEntity
import com.parhar.noor.data.local.entity.CategoryEntity
import com.parhar.noor.data.local.entity.DailyTaskEntryEntity
import com.parhar.noor.data.local.entity.FavoriteBannerEntity
import com.parhar.noor.data.local.entity.FriendEntity
import com.parhar.noor.data.local.entity.SyncMetadataEntity
import com.parhar.noor.data.local.entity.SyncOutboxEntity
import com.parhar.noor.data.local.entity.TaskDefinitionEntity
import com.parhar.noor.data.local.entity.UserEntity
import com.parhar.noor.data.local.entity.UserPreferencesEntity

@Database(
    entities = [
        UserEntity::class,
        FriendEntity::class,
        CategoryEntity::class,
        TaskDefinitionEntity::class,
        DailyTaskEntryEntity::class,
        FavoriteBannerEntity::class,
        UserPreferencesEntity::class,
        SyncOutboxEntity::class,
        SyncMetadataEntity::class,
        AyatEntity::class,
    ],
    version = 10,
    exportSchema = false,
)
@TypeConverters(NoorTypeConverters::class)
abstract class NoorDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun friendDao(): FriendDao
    abstract fun categoryDao(): CategoryDao
    abstract fun taskDefinitionDao(): TaskDefinitionDao
    abstract fun dailyTaskEntryDao(): DailyTaskEntryDao
    abstract fun favoriteBannerDao(): FavoriteBannerDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun syncOutboxDao(): SyncOutboxDao
    abstract fun syncMetadataDao(): SyncMetadataDao
    abstract fun ayatDao(): AyatDao
}
