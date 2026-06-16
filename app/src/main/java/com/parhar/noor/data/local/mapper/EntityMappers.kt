package com.parhar.noor.data.local.mapper

import com.parhar.noor.data.local.entity.CategoryEntity
import com.parhar.noor.data.local.entity.DailyTaskEntryEntity
import com.parhar.noor.data.local.entity.FavoriteBannerEntity
import com.parhar.noor.data.local.entity.TaskDefinitionEntity
import com.parhar.noor.data.local.entity.UserEntity
import com.parhar.noor.domain.model.Category
import com.parhar.noor.domain.model.FavoriteBanner
import com.parhar.noor.domain.model.TaskDefinition
import com.parhar.noor.domain.model.TaskItem
import com.parhar.noor.domain.model.UserProfile

object EntityMappers {

    fun UserEntity.toDomain(): UserProfile = UserProfile(
        uid = uid,
        email = email,
        name = name,
        gender = gender,
    )

    fun UserProfile.toEntity(
        updatedAt: Long,
        syncStatus: com.parhar.noor.data.local.SyncStatus = com.parhar.noor.data.local.SyncStatus.SYNCED,
    ): UserEntity = UserEntity(
        uid = uid,
        email = email,
        name = name,
        gender = gender,
        updatedAt = updatedAt,
        syncStatus = syncStatus,
    )

    fun CategoryEntity.toDomain(): Category = Category(
        id = id,
        categoryKey = categoryKey,
        category = categoryName,
        title = title,
    )

    fun TaskDefinitionEntity.toDomain(): TaskDefinition = TaskDefinition(
        id = id,
        category = category,
        name = name,
        points = points,
        sortOrder = sortOrder,
    )

    fun TaskDefinitionEntity.toTaskItem(): TaskItem = TaskItem(
        id = id,
        task = toDomain(),
    )

    fun FavoriteBannerEntity.toDomain(): FavoriteBanner = FavoriteBanner(
        email = email,
        emoji = emoji,
        message = message,
    )

    fun List<DailyTaskEntryEntity>.toTaskPointsMap(): Map<String, Int> {
        return associate { it.taskId to it.points }
    }

    fun List<DailyTaskEntryEntity>.toTaskHistory(): Map<String, Map<String, Int>> {
        return groupBy { it.dateKey }
            .mapValues { (_, entries) -> entries.associate { it.taskId to it.points } }
    }
}
