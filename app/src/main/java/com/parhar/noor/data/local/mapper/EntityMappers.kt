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
import com.parhar.noor.domain.model.UserAvatar
import com.parhar.noor.domain.model.UserPrivacy
import com.parhar.noor.domain.model.UserProfile
import com.parhar.noor.data.remote.dto.RemoteUserAvatar
import com.parhar.noor.data.remote.dto.RemoteUserPrivacy
import com.parhar.noor.data.remote.dto.RemoteUserProfile

object EntityMappers {

    fun UserEntity.toDomain(): UserProfile {
        val avatar = if (
            avatarText.isNotBlank() ||
            avatarBg.isNotBlank() ||
            avatarBorder.isNotBlank() ||
            avatarStyle.isNotBlank()
        ) {
            UserAvatar(
                text = avatarText,
                bg = avatarBg,
                border = avatarBorder,
                style = avatarStyle,
            )
        } else {
            null
        }
        return UserProfile(
            uid = uid,
            email = email,
            name = name,
            gender = gender,
            avatar = avatar,
            createdAt = createdAt,
            privacy = UserPrivacy(
                tasksToday = privacyTasksToday,
                tasksHistory = privacyTasksHistory,
            ),
        )
    }

    fun UserProfile.toEntity(
        updatedAt: Long,
        syncStatus: com.parhar.noor.data.local.SyncStatus = com.parhar.noor.data.local.SyncStatus.SYNCED,
    ): UserEntity = UserEntity(
        uid = uid,
        email = email,
        name = name,
        gender = gender,
        avatarText = avatar?.text.orEmpty(),
        avatarBg = avatar?.bg.orEmpty(),
        avatarBorder = avatar?.border.orEmpty(),
        avatarStyle = avatar?.style.orEmpty(),
        createdAt = createdAt,
        privacyTasksToday = privacy.tasksToday,
        privacyTasksHistory = privacy.tasksHistory,
        updatedAt = updatedAt,
        syncStatus = syncStatus,
    )

    fun UserAvatar.toRemote(): RemoteUserAvatar = RemoteUserAvatar(
        text = text,
        bg = bg,
        border = border,
        style = style,
    )

    fun RemoteUserAvatar.toDomain(): UserAvatar = UserAvatar(
        text = text,
        bg = bg,
        border = border,
        style = style,
    )

    fun RemoteUserPrivacy.toDomain(): UserPrivacy = UserPrivacy(
        tasksToday = tasksToday,
        tasksHistory = tasksHistory,
    )

    fun UserPrivacy.toRemote(): RemoteUserPrivacy = RemoteUserPrivacy(
        tasksToday = tasksToday,
        tasksHistory = tasksHistory,
    )

    fun RemoteUserProfile.toDomain(): UserProfile = UserProfile(
        uid = uid,
        email = email,
        name = name,
        gender = gender,
        avatar = avatar?.toDomain(),
        createdAt = createdAt,
        privacy = privacy?.toDomain() ?: UserPrivacy(),
    )

    fun UserProfile.toRemote(): RemoteUserProfile = RemoteUserProfile(
        uid = uid,
        email = email,
        name = name,
        gender = gender,
        avatar = avatar?.toRemote(),
        createdAt = createdAt,
        privacy = privacy.toRemote(),
    )

    fun CategoryEntity.toDomain(): Category = Category(
        id = id,
        categoryKey = categoryKey,
        category = categoryName,
        title = title,
        description = description,
        position = position,
    )

    fun TaskDefinitionEntity.toDomain(): TaskDefinition = TaskDefinition(
        id = id,
        category = category,
        name = name,
        points = points,
        position = position,
        emoji = emoji,
        shortDescription = shortDescription,
        detailedDescription = detailedDescription,
        arabic = arabic,
        visible = visible,
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
