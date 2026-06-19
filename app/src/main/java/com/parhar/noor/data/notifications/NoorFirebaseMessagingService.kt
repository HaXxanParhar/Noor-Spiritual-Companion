package com.parhar.noor.data.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.parhar.noor.NoorApplication
import com.parhar.noor.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NoorFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val appContainer = (application as? NoorApplication)?.appContainer ?: return
        val uid = appContainer.sessionManager.getUserId().orEmpty()
        if (uid.isBlank() || token.isBlank()) return
        serviceScope.launch {
            runCatching {
                appContainer.userProfileRepository.saveFcmToken(uid, token)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(R.string.notification_remind_title)
        val body = message.notification?.body
            ?: message.data["body"]
            ?: getString(R.string.notification_remind_body_default)
        NotificationHelper.showFriendRemindNotification(this, title, body)
    }
}
