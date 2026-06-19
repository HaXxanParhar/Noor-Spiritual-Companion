package com.parhar.noor.data.repository

import kotlinx.coroutines.flow.Flow

interface FriendsRepository {
    fun observeFriendIds(userUid: String): Flow<List<String>>
    suspend fun getFriendIds(userUid: String): List<String>
    suspend fun addFriend(currentUid: String, friendUid: String)
    suspend fun removeFriend(currentUid: String, friendUid: String)
    suspend fun isFriend(currentUid: String, friendUid: String): Boolean
    suspend fun sendRemind(currentUid: String, friendUid: String, senderName: String, message: String): RemindResult
    suspend fun canSendReminder(currentUid: String, friendUid: String): Boolean
}
