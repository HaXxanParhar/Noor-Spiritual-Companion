package com.parhar.noor.data.notifications

import com.google.firebase.messaging.FirebaseMessaging
import com.parhar.noor.data.repository.UserProfileRepository
import kotlinx.coroutines.tasks.await

object FcmTokenManager {

    suspend fun registerToken(userProfileRepository: UserProfileRepository, uid: String) {
        if (uid.isBlank()) return
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            if (token.isNotBlank()) {
                userProfileRepository.saveFcmToken(uid, token)
            }
        }
    }
}
