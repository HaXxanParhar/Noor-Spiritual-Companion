package com.parhar.noor.data.repository

import com.parhar.noor.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    fun observeUser(uid: String): Flow<UserProfile?>
    suspend fun getUser(uid: String): UserProfile?
    suspend fun saveProfile(profile: UserProfile)
    suspend fun userExists(uid: String): Boolean
}
