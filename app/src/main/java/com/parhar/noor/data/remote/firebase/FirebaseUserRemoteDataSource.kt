package com.parhar.noor.data.remote.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.parhar.noor.data.reminders.ReminderObserverDebug
import com.parhar.noor.data.remote.UserRemoteDataSource
import com.parhar.noor.data.remote.dto.RemoteFavoriteInfo
import com.parhar.noor.data.remote.dto.RemoteReminder
import com.parhar.noor.data.remote.dto.RemoteSteakSnapshot
import com.parhar.noor.data.remote.dto.RemoteUserMedals
import com.parhar.noor.data.remote.dto.RemoteUserProfile
import com.parhar.noor.data.remote.dto.RemoteWeekCycle
import kotlinx.coroutines.tasks.await
import java.io.Closeable

class FirebaseUserRemoteDataSource(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance(),
) : UserRemoteDataSource {

    override suspend fun fetchUserProfile(uid: String): RemoteUserProfile? {
        val snapshot = database.reference
            .child(USERS_NODE)
            .child(uid)
            .get()
            .await()
        if (!snapshot.exists()) return null
        return snapshot.toRemoteUserProfile(uid)
    }

    override suspend fun pushUserProfile(profile: RemoteUserProfile) {
        val updates = linkedMapOf<String, Any>(
            "uid" to profile.uid,
            "email" to profile.email,
            "name" to profile.name,
            "gender" to profile.gender,
        )
        if (profile.createdAt > 0L) {
            updates["createdAt"] = profile.createdAt
        }
        profile.avatar?.let { avatar ->
            updates["avatar"] = mapOf(
                "text" to avatar.text,
                "bg" to avatar.bg,
                "border" to avatar.border,
                "style" to avatar.style,
            )
        }
        profile.privacy?.let { privacy ->
            updates["privacy"] = mapOf(
                "tasks_today" to privacy.tasksToday,
                "tasks_history" to privacy.tasksHistory,
            )
        }
        database.reference
            .child(USERS_NODE)
            .child(profile.uid)
            .updateChildren(updates)
            .await()
    }

    override suspend fun userExists(uid: String): Boolean {
        return database.reference.child(USERS_NODE).child(uid).get().await().exists()
    }

    override suspend fun saveFcmToken(uid: String, token: String) {
        database.reference
            .child(USERS_NODE)
            .child(uid)
            .child(FCM_TOKEN_FIELD)
            .setValue(token)
            .await()
    }

    override suspend fun fetchFcmToken(uid: String): String? {
        return database.reference
            .child(USERS_NODE)
            .child(uid)
            .child(FCM_TOKEN_FIELD)
            .get()
            .await()
            .getValue(String::class.java)
            ?.takeIf { it.isNotBlank() }
    }

    override suspend fun fetchDailyTaskString(uid: String, dateKey: String): String? {
        return database.reference
            .child(USERS_NODE)
            .child(uid)
            .child(TASKS_NODE)
            .child(dateKey)
            .get()
            .await()
            .getValue(String::class.java)
    }

    override suspend fun pushDailyTaskString(uid: String, dateKey: String, value: String) {
        database.reference
            .child(USERS_NODE)
            .child(uid)
            .child(TASKS_NODE)
            .child(dateKey)
            .setValue(value)
            .await()
    }

    override suspend fun fetchUserTaskHistory(uid: String): Map<String, String> {
        val snapshot = database.reference
            .child(USERS_NODE)
            .child(uid)
            .child(TASKS_NODE)
            .get()
            .await()
        return snapshot.children.associate { child ->
            child.key.orEmpty() to child.getValue(String::class.java).orEmpty()
        }.filterKeys { it.isNotBlank() }
    }

    override suspend fun fetchFriendIds(uid: String): List<String> {
        return database.reference
            .child(USERS_NODE)
            .child(uid)
            .child(FRIENDS_NODE)
            .get()
            .await()
            .children
            .mapNotNull { child -> child.key?.takeIf { it.isNotBlank() } }
    }

    override suspend fun pushFriendship(currentUid: String, friendUid: String) {
        database.reference
            .child(USERS_NODE)
            .child(currentUid)
            .child(FRIENDS_NODE)
            .child(friendUid)
            .setValue(true)
            .await()
        database.reference
            .child(USERS_NODE)
            .child(friendUid)
            .child(FRIENDS_NODE)
            .child(currentUid)
            .setValue(true)
            .await()
    }

    override suspend fun removeFriendship(currentUid: String, friendUid: String) {
        database.reference
            .child(USERS_NODE)
            .child(currentUid)
            .child(FRIENDS_NODE)
            .child(friendUid)
            .removeValue()
            .await()
        database.reference
            .child(USERS_NODE)
            .child(friendUid)
            .child(FRIENDS_NODE)
            .child(currentUid)
            .removeValue()
            .await()
    }

    override suspend fun fetchFavoriteInfo(): RemoteFavoriteInfo? {
        return database.reference.child(FAV_NODE).get().await()
            .getValue(RemoteFavoriteInfo::class.java)
    }

    override suspend fun fetchSteak(uid: String): RemoteSteakSnapshot? {
        val snapshot = database.reference
            .child(USERS_NODE)
            .child(uid)
            .child(STEAK_NODE)
            .get()
            .await()
        if (!snapshot.exists()) return null
        return snapshot.getValue(RemoteSteakSnapshot::class.java)
    }

    override suspend fun pushSteak(uid: String, steak: RemoteSteakSnapshot) {
        database.reference
            .child(USERS_NODE)
            .child(uid)
            .child(STEAK_NODE)
            .setValue(steak)
            .await()
    }

    override fun observeDailyTasks(
        uid: String,
        dateKey: String,
        onChanged: (String?) -> Unit,
        onError: (String) -> Unit,
    ): AutoCloseable {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onChanged(snapshot.getValue(String::class.java))
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        }
        database.reference
            .child(USERS_NODE)
            .child(uid)
            .child(TASKS_NODE)
            .child(dateKey)
            .addValueEventListener(listener)
        return CloseableListener {
            database.reference
                .child(USERS_NODE)
                .child(uid)
                .child(TASKS_NODE)
                .child(dateKey)
                .removeEventListener(listener)
        }
    }

    override fun observeUserTaskHistory(
        uid: String,
        onChanged: (Map<String, String>) -> Unit,
        onError: (String) -> Unit,
    ): AutoCloseable {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val history = snapshot.children.associate { child ->
                    child.key.orEmpty() to child.getValue(String::class.java).orEmpty()
                }.filterKeys { it.isNotBlank() }
                onChanged(history)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        }
        database.reference
            .child(USERS_NODE)
            .child(uid)
            .child(TASKS_NODE)
            .addValueEventListener(listener)
        return CloseableListener {
            database.reference
                .child(USERS_NODE)
                .child(uid)
                .child(TASKS_NODE)
                .removeEventListener(listener)
        }
    }

    override fun observeFriends(
        uid: String,
        onChanged: (List<String>) -> Unit,
        onError: (String) -> Unit,
    ): AutoCloseable {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onChanged(
                    snapshot.children.mapNotNull { child ->
                        child.key?.takeIf { it.isNotBlank() }
                    },
                )
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        }
        database.reference
            .child(USERS_NODE)
            .child(uid)
            .child(FRIENDS_NODE)
            .addValueEventListener(listener)
        return CloseableListener {
            database.reference
                .child(USERS_NODE)
                .child(uid)
                .child(FRIENDS_NODE)
                .removeEventListener(listener)
        }
    }

    override fun observeFavoriteInfo(
        onChanged: (RemoteFavoriteInfo?) -> Unit,
        onError: (String) -> Unit,
    ): AutoCloseable {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onChanged(snapshot.getValue(RemoteFavoriteInfo::class.java))
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        }
        database.reference.child(FAV_NODE).addValueEventListener(listener)
        return CloseableListener {
            database.reference.child(FAV_NODE).removeEventListener(listener)
        }
    }

    override suspend fun fetchWeeks(uid: String): Map<String, RemoteWeekCycle> {
        val snapshot = database.reference
            .child(USERS_NODE)
            .child(uid)
            .child(WEEKS_NODE)
            .get()
            .await()
        return snapshot.children.mapNotNull { child ->
            child.toRemoteWeekCycle()?.let { week -> week.weekKey to week }
        }.toMap()
    }

    override suspend fun fetchWeek(uid: String, weekKey: String): RemoteWeekCycle? {
        val snapshot = database.reference
            .child(USERS_NODE)
            .child(uid)
            .child(WEEKS_NODE)
            .child(weekKey)
            .get()
            .await()
        if (!snapshot.exists()) return null
        return snapshot.toRemoteWeekCycle(weekKey)
    }

    override suspend fun createWeekIfMissing(uid: String, week: RemoteWeekCycle) {
        val weekRef = database.reference
            .child(USERS_NODE)
            .child(uid)
            .child(WEEKS_NODE)
            .child(week.weekKey)
        val existing = weekRef.get().await()
        if (existing.exists()) return
        weekRef.setValue(week.toFirebaseMap()).await()
    }

    override suspend fun updateWeek(uid: String, weekKey: String, fields: Map<String, Any>) {
        database.reference
            .child(USERS_NODE)
            .child(uid)
            .child(WEEKS_NODE)
            .child(weekKey)
            .updateChildren(fields)
            .await()
    }

    override suspend fun fetchUserMedals(uid: String): RemoteUserMedals {
        val snapshot = database.reference
            .child(USERS_NODE)
            .child(uid)
            .child(MEDALS_NODE)
            .get()
            .await()
        return snapshot.toRemoteUserMedals()
    }

    override suspend fun hasReminderFromSender(targetUid: String, senderUid: String): Boolean {
        if (targetUid.isBlank() || senderUid.isBlank()) return false
        return database.reference
            .child(USERS_NODE)
            .child(targetUid)
            .child(REMINDERS_NODE)
            .child(senderUid)
            .get()
            .await()
            .exists()
    }

    override suspend fun pushReminder(targetUid: String, reminder: RemoteReminder) {
        if (targetUid.isBlank() || reminder.senderId.isBlank()) return
        database.reference
            .child(USERS_NODE)
            .child(targetUid)
            .child(REMINDERS_NODE)
            .child(reminder.senderId)
            .setValue(
                mapOf(
                    SENDER_FIELD to reminder.sender,
                    MESSAGE_FIELD to reminder.message,
                    CREATED_AT_FIELD to reminder.createdAt,
                ),
            )
            .await()
    }

    override suspend fun deleteReminder(targetUid: String, senderUid: String) {
        if (targetUid.isBlank() || senderUid.isBlank()) return
        database.reference
            .child(USERS_NODE)
            .child(targetUid)
            .child(REMINDERS_NODE)
            .child(senderUid)
            .removeValue()
            .await()
    }

    override fun observeReminderFromSender(
        targetUid: String,
        senderUid: String,
        onChanged: (RemoteReminder?) -> Unit,
        onError: (String) -> Unit,
    ): AutoCloseable {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onChanged(
                    if (snapshot.exists()) {
                        snapshot.toRemoteReminder(senderUid)
                    } else {
                        null
                    },
                )
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        }
        val ref = database.reference
            .child(USERS_NODE)
            .child(targetUid)
            .child(REMINDERS_NODE)
            .child(senderUid)
        ref.addValueEventListener(listener)
        return CloseableListener { ref.removeEventListener(listener) }
    }

    override fun observeReminders(
        uid: String,
        onChanged: (List<RemoteReminder>) -> Unit,
        onError: (String) -> Unit,
    ): AutoCloseable {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val exists = snapshot.exists()
                val childCount = snapshot.childrenCount
                ReminderObserverDebug.notify(
                    "RTDB onDataChange uid=$uid exists=$exists children=$childCount",
                )
                val reminders = snapshot.children.mapNotNull { child ->
                    val senderId = child.key.orEmpty()
                    if (senderId.isBlank()) null else child.toRemoteReminder(senderId)
                }
                onChanged(reminders)
            }

            override fun onCancelled(error: DatabaseError) {
                val message = "${error.code}: ${error.message}"
                ReminderObserverDebug.warn("RTDB onCancelled uid=$uid $message")
                onError(message)
            }
        }
        val ref = database.reference
            .child(USERS_NODE)
            .child(uid)
            .child(REMINDERS_NODE)
        ref.addValueEventListener(listener)
        return CloseableListener { ref.removeEventListener(listener) }
    }

    override suspend fun incrementMedalTier(uid: String, position: Int) {
        val field = medalFieldForPosition(position) ?: return
        val current = fetchUserMedals(uid)
        val updated = when (field) {
            TOTAL_1ST_FIELD -> current.copy(total1st = current.total1st + 1)
            TOTAL_2ND_FIELD -> current.copy(total2nd = current.total2nd + 1)
            TOTAL_3RD_FIELD -> current.copy(total3rd = current.total3rd + 1)
            TOTAL_TOP5_FIELD -> current.copy(totalTop5 = current.totalTop5 + 1)
            TOTAL_TOP10_FIELD -> current.copy(totalTop10 = current.totalTop10 + 1)
            else -> return
        }
        database.reference
            .child(USERS_NODE)
            .child(uid)
            .child(MEDALS_NODE)
            .updateChildren(updated.toFirebaseMap())
            .await()
    }

    private fun medalFieldForPosition(position: Int): String? = when (position) {
        1 -> TOTAL_1ST_FIELD
        2 -> TOTAL_2ND_FIELD
        3 -> TOTAL_3RD_FIELD
        in 4..5 -> TOTAL_TOP5_FIELD
        in 6..10 -> TOTAL_TOP10_FIELD
        else -> null
    }

    private fun RemoteWeekCycle.toFirebaseMap(): Map<String, Any> = mapOf(
        JOINED_AT_FIELD to joinedAt,
        TITLE_FIELD to title,
        START_FIELD to start,
        END_FIELD to end,
        END_AT_FIELD to endAt,
        MY_POSITION_FIELD to myPosition,
        POINTS_FIELD to points,
        DONE_FIELD to done,
    )

    private fun RemoteUserMedals.toFirebaseMap(): Map<String, Any> = mapOf(
        TOTAL_1ST_FIELD to total1st,
        TOTAL_2ND_FIELD to total2nd,
        TOTAL_3RD_FIELD to total3rd,
        TOTAL_TOP5_FIELD to totalTop5,
        TOTAL_TOP10_FIELD to totalTop10,
    )

    private fun DataSnapshot.toRemoteWeekCycle(fallbackKey: String? = null): RemoteWeekCycle? {
        val weekKey = fallbackKey ?: key?.takeIf { it.isNotBlank() } ?: return null
        return RemoteWeekCycle(
            weekKey = weekKey,
            joinedAt = child(JOINED_AT_FIELD).getValue(Long::class.java) ?: 0L,
            title = child(TITLE_FIELD).getValue(String::class.java).orEmpty(),
            start = child(START_FIELD).getValue(String::class.java).orEmpty(),
            end = child(END_FIELD).getValue(String::class.java).orEmpty(),
            endAt = child(END_AT_FIELD).getValue(Long::class.java) ?: 0L,
            myPosition = child(MY_POSITION_FIELD).getValue(Int::class.java)
                ?: child(MY_POSITION_FIELD).getValue(Long::class.java)?.toInt()
                ?: -1,
            points = child(POINTS_FIELD).getValue(Int::class.java)
                ?: child(POINTS_FIELD).getValue(Long::class.java)?.toInt()
                ?: 0,
            done = child(DONE_FIELD).getValue(Boolean::class.java) ?: false,
        )
    }

    private fun DataSnapshot.toRemoteReminder(senderId: String): RemoteReminder {
        return RemoteReminder(
            senderId = senderId,
            sender = child(SENDER_FIELD).getValue(String::class.java).orEmpty(),
            message = child(MESSAGE_FIELD).getValue(String::class.java).orEmpty(),
            createdAt = child(CREATED_AT_FIELD).getValue(Long::class.java) ?: 0L,
        )
    }

    private fun DataSnapshot.toRemoteUserMedals(): RemoteUserMedals {
        return RemoteUserMedals(
            total1st = child(TOTAL_1ST_FIELD).getValue(Int::class.java)
                ?: child(TOTAL_1ST_FIELD).getValue(Long::class.java)?.toInt()
                ?: 0,
            total2nd = child(TOTAL_2ND_FIELD).getValue(Int::class.java)
                ?: child(TOTAL_2ND_FIELD).getValue(Long::class.java)?.toInt()
                ?: 0,
            total3rd = child(TOTAL_3RD_FIELD).getValue(Int::class.java)
                ?: child(TOTAL_3RD_FIELD).getValue(Long::class.java)?.toInt()
                ?: 0,
            totalTop5 = child(TOTAL_TOP5_FIELD).getValue(Int::class.java)
                ?: child(TOTAL_TOP5_FIELD).getValue(Long::class.java)?.toInt()
                ?: 0,
            totalTop10 = child(TOTAL_TOP10_FIELD).getValue(Int::class.java)
                ?: child(TOTAL_TOP10_FIELD).getValue(Long::class.java)?.toInt()
                ?: 0,
        )
    }

    override fun observeUserProfile(
        uid: String,
        onChanged: (RemoteUserProfile?) -> Unit,
        onError: (String) -> Unit,
    ): AutoCloseable {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onChanged(if (snapshot.exists()) snapshot.toRemoteUserProfile(uid) else null)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        }
        database.reference.child(USERS_NODE).child(uid).addValueEventListener(listener)
        return CloseableListener {
            database.reference.child(USERS_NODE).child(uid).removeEventListener(listener)
        }
    }

    private fun DataSnapshot.toRemoteUserProfile(fallbackUid: String): RemoteUserProfile {
        val avatarSnapshot = child(AVATAR_NODE)
        val avatar = if (avatarSnapshot.exists()) {
            com.parhar.noor.data.remote.dto.RemoteUserAvatar(
                text = avatarSnapshot.child(TEXT_FIELD).getValue(String::class.java).orEmpty(),
                bg = avatarSnapshot.child(BG_FIELD).getValue(String::class.java).orEmpty(),
                border = avatarSnapshot.child(BORDER_FIELD).getValue(String::class.java).orEmpty(),
                style = avatarSnapshot.child(STYLE_FIELD).getValue(String::class.java).orEmpty(),
            )
        } else {
            null
        }
        val privacySnapshot = child(PRIVACY_NODE)
        val privacy = if (privacySnapshot.exists()) {
            com.parhar.noor.data.remote.dto.RemoteUserPrivacy(
                tasksToday = privacySnapshot.child(TASKS_TODAY_FIELD)
                    .getValue(String::class.java)
                    .orEmpty()
                    .ifBlank { com.parhar.noor.domain.model.PrivacyVisibility.PRIVATE },
                tasksHistory = privacySnapshot.child(TASKS_HISTORY_FIELD)
                    .getValue(String::class.java)
                    .orEmpty()
                    .ifBlank { com.parhar.noor.domain.model.PrivacyVisibility.PRIVATE },
            )
        } else {
            null
        }
        return RemoteUserProfile(
            uid = child(UID_FIELD).getValue(String::class.java).orEmpty().ifBlank { fallbackUid },
            email = child(EMAIL_FIELD).getValue(String::class.java).orEmpty(),
            name = child(NAME_FIELD).getValue(String::class.java).orEmpty(),
            gender = child(GENDER_FIELD).getValue(String::class.java).orEmpty(),
            avatar = avatar,
            createdAt = child(CREATED_AT_FIELD).getValue(Long::class.java) ?: 0L,
            privacy = privacy,
        )
    }

    private class CloseableListener(private val onClose: () -> Unit) : Closeable {
        override fun close() = onClose()
    }

    private companion object {
        private const val USERS_NODE = "users"
        private const val TASKS_NODE = "tasks"
        private const val FRIENDS_NODE = "friends"
        private const val FAV_NODE = "fav"
        private const val STEAK_NODE = "steak"
        private const val WEEKS_NODE = "weeks"
        private const val MEDALS_NODE = "medals"
        private const val REMINDERS_NODE = "reminders"
        private const val SENDER_FIELD = "sender"
        private const val MESSAGE_FIELD = "message"
        private const val AVATAR_NODE = "avatar"
        private const val PRIVACY_NODE = "privacy"
        private const val UID_FIELD = "uid"
        private const val EMAIL_FIELD = "email"
        private const val NAME_FIELD = "name"
        private const val GENDER_FIELD = "gender"
        private const val CREATED_AT_FIELD = "createdAt"
        private const val FCM_TOKEN_FIELD = "fcmToken"
        private const val TEXT_FIELD = "text"
        private const val BG_FIELD = "bg"
        private const val BORDER_FIELD = "border"
        private const val STYLE_FIELD = "style"
        private const val TASKS_TODAY_FIELD = "tasks_today"
        private const val TASKS_HISTORY_FIELD = "tasks_history"
        private const val JOINED_AT_FIELD = "joinedAt"
        private const val TITLE_FIELD = "title"
        private const val START_FIELD = "start"
        private const val END_FIELD = "end"
        private const val END_AT_FIELD = "endAt"
        private const val MY_POSITION_FIELD = "myPosition"
        private const val POINTS_FIELD = "points"
        private const val DONE_FIELD = "done"
        private const val TOTAL_1ST_FIELD = "total_1st"
        private const val TOTAL_2ND_FIELD = "total_2nd"
        private const val TOTAL_3RD_FIELD = "total_3rd"
        private const val TOTAL_TOP5_FIELD = "total_top5"
        private const val TOTAL_TOP10_FIELD = "total_top10"
    }
}
