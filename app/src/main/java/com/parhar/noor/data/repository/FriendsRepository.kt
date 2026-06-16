package com.parhar.noor.data.repository

import kotlinx.coroutines.flow.Flow

interface FriendsRepository {
    fun observeFriendIds(userUid: String): Flow<List<String>>
    suspend fun getFriendIds(userUid: String): List<String>
    suspend fun addFriend(currentUid: String, friendUid: String)
}
