package com.parhar.noor.data.repository

import com.parhar.noor.domain.model.FriendReminder
import kotlinx.coroutines.flow.Flow

interface RemindersRepository {
    fun observeReminders(userUid: String): Flow<List<FriendReminder>>
    fun observeCanSendReminderTo(targetUid: String, senderUid: String): Flow<Boolean>
    suspend fun deleteReminder(userUid: String, senderUid: String)
    suspend fun canSendReminderTo(targetUid: String, senderUid: String): Boolean
    suspend fun sendReminder(
        targetUid: String,
        senderUid: String,
        senderName: String,
        message: String,
        isOnline: Boolean,
    ): RemindResult
}
