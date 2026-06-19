package com.parhar.noor.data.repository

import com.parhar.noor.domain.model.UserAvatar
import com.parhar.noor.domain.model.UserPrivacy
import com.parhar.noor.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    fun observeUser(uid: String): Flow<UserProfile?>
    suspend fun getUser(uid: String): UserProfile?
    suspend fun saveProfile(profile: UserProfile)
    suspend fun saveAvatar(uid: String, avatar: UserAvatar)
    suspend fun savePrivacy(uid: String, privacy: UserPrivacy)
    suspend fun saveFcmToken(uid: String, token: String)
    suspend fun userExists(uid: String): Boolean
}
