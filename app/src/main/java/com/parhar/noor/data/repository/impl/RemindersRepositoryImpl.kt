package com.parhar.noor.data.repository.impl

import android.os.Handler
import android.os.Looper
import com.parhar.noor.data.reminders.ReminderObserverDebug
import com.parhar.noor.data.remote.UserRemoteDataSource
import com.parhar.noor.data.remote.dto.RemoteReminder
import com.parhar.noor.data.repository.RemindResult
import com.parhar.noor.data.repository.RemindersRepository
import com.parhar.noor.domain.model.FriendReminder
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class RemindersRepositoryImpl(
    private val userRemote: UserRemoteDataSource,
) : RemindersRepository {

    override fun observeReminders(userUid: String): Flow<List<FriendReminder>> = callbackFlow {
        if (userUid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        var listener: AutoCloseable? = null
        var isClosed = false
        val reconnectHandler = Handler(Looper.getMainLooper())
        var reconnectRunnable: Runnable? = null

        fun attachListener() {
            listener?.close()
            listener = userRemote.observeReminders(
                uid = userUid,
                onChanged = { reminders ->
                    val senderSummary = reminders.joinToString { it.senderId }
                    ReminderObserverDebug.notify(
                        "Reminders mapped: ${reminders.size} item(s) [$senderSummary]",
                    )
                    trySend(reminders.map { it.toDomain() })
                },
                onError = { error ->
                    ReminderObserverDebug.warn("Reminders listener error: $error")
                    listener?.close()
                    listener = null
                    if (!isClosed) {
                        reconnectRunnable?.let { reconnectHandler.removeCallbacks(it) }
                        reconnectRunnable = Runnable {
                            if (!isClosed) {
                                ReminderObserverDebug.notify("Reconnecting reminders listener…")
                                attachListener()
                            }
                        }
                        reconnectHandler.postDelayed(reconnectRunnable!!, RECONNECT_DELAY_MS)
                    }
                },
            )
            ReminderObserverDebug.notify("Reminders listener attached for uid=$userUid")
        }

        attachListener()

        awaitClose {
            isClosed = true
            reconnectRunnable?.let { reconnectHandler.removeCallbacks(it) }
            listener?.close()
            ReminderObserverDebug.notify("Reminders listener closed for uid=$userUid")
        }
    }

    override fun observeCanSendReminderTo(targetUid: String, senderUid: String): Flow<Boolean> =
        callbackFlow {
            if (targetUid.isBlank() || senderUid.isBlank()) {
                trySend(false)
                close()
                return@callbackFlow
            }
            val listener = userRemote.observeReminderFromSender(
                targetUid = targetUid,
                senderUid = senderUid,
                onChanged = { reminder ->
                    trySend(reminder == null)
                },
                onError = {},
            )
            awaitClose { listener.close() }
        }.distinctUntilChanged()

    override suspend fun deleteReminder(userUid: String, senderUid: String) {
        userRemote.deleteReminder(userUid, senderUid)
    }

    override suspend fun canSendReminderTo(targetUid: String, senderUid: String): Boolean {
        if (targetUid.isBlank() || senderUid.isBlank()) return false
        return !userRemote.hasReminderFromSender(targetUid, senderUid)
    }

    override suspend fun sendReminder(
        targetUid: String,
        senderUid: String,
        senderName: String,
        message: String,
        isOnline: Boolean,
    ): RemindResult {
        if (!isOnline) return RemindResult.Offline
        if (targetUid.isBlank() || senderUid.isBlank()) {
            return RemindResult.Failed("Unable to send reminder.")
        }

        val trimmedMessage = message.trim().take(REMINDER_MESSAGE_MAX_LENGTH)
        if (trimmedMessage.isBlank()) {
            return RemindResult.Failed("Reminder message cannot be empty.")
        }

        if (userRemote.hasReminderFromSender(targetUid, senderUid)) {
            return RemindResult.PendingUnseen
        }

        return runCatching {
            userRemote.pushReminder(
                targetUid = targetUid,
                reminder = RemoteReminder(
                    senderId = senderUid,
                    sender = senderName,
                    message = trimmedMessage,
                    createdAt = System.currentTimeMillis(),
                ),
            )
            RemindResult.Sent
        }.getOrElse { error ->
            RemindResult.Failed(error.message ?: "Unable to send reminder.")
        }
    }

    private fun RemoteReminder.toDomain(): FriendReminder = FriendReminder(
        senderId = senderId,
        sender = sender,
        message = message,
        createdAt = createdAt,
    )

    private companion object {
        private const val RECONNECT_DELAY_MS = 2_000L
        private const val REMINDER_MESSAGE_MAX_LENGTH = 200
    }
}
