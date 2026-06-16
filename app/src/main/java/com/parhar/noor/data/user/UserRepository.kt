package com.parhar.noor.data.user

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance(),
) {

    suspend fun saveUserProfile(userProfile: UserProfile) {
        database.reference
            .child(USERS_NODE)
            .child(userProfile.uid)
            .setValue(userProfile)
            .await()
    }

    suspend fun getUserProfile(uid: String): UserProfile? {
        return database.reference
            .child(USERS_NODE)
            .child(uid)
            .get()
            .await()
            .getValue(UserProfile::class.java)
    }

    suspend fun getUserProfileByEmail(email: String): UserProfile? {
        return database.reference
            .child(USERS_NODE)
            .orderByChild(EMAIL_FIELD)
            .equalTo(email)
            .get()
            .await()
            .children
            .firstOrNull()
            ?.getValue(UserProfile::class.java)
    }

    fun observeDailyTaskPoints(
        uid: String,
        dateKey: String,
        onChanged: (Map<String, Int>) -> Unit,
        onError: (String) -> Unit,
    ): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onChanged(snapshot.getValue(String::class.java).toDailyTaskPoints())
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
        return listener
    }

    fun removeDailyTaskPointsObserver(uid: String, dateKey: String, listener: ValueEventListener) {
        database.reference
            .child(USERS_NODE)
            .child(uid)
            .child(TASKS_NODE)
            .child(dateKey)
            .removeEventListener(listener)
    }

    suspend fun saveDailyTaskPoints(uid: String, dateKey: String, taskPoints: Map<String, Int>) {
        database.reference
            .child(USERS_NODE)
            .child(uid)
            .child(TASKS_NODE)
            .child(dateKey)
            .setValue(taskPoints.toDailyTaskPointsString())
            .await()
    }

    suspend fun getFavoriteInfo(): FavoriteInfo? {
        return database.reference
            .child(FAV_NODE)
            .get()
            .await()
            .getValue(FavoriteInfo::class.java)
    }

    fun observeFavoriteInfo(
        onChanged: (FavoriteInfo?) -> Unit,
        onError: (String) -> Unit,
    ): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onChanged(snapshot.getValue(FavoriteInfo::class.java))
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        }

        database.reference.child(FAV_NODE).addValueEventListener(listener)
        return listener
    }

    fun removeFavoriteInfoObserver(listener: ValueEventListener) {
        database.reference.child(FAV_NODE).removeEventListener(listener)
    }

    fun observeUserTaskHistory(
        uid: String,
        onChanged: (Map<String, Map<String, Int>>) -> Unit,
        onError: (String) -> Unit,
    ): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val history = linkedMapOf<String, Map<String, Int>>()
                snapshot.children.forEach { dateSnapshot ->
                    val dateKey = dateSnapshot.key.orEmpty()
                    if (dateKey.isNotBlank()) {
                        history[dateKey] = dateSnapshot.getValue(String::class.java).toDailyTaskPoints()
                    }
                }
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
        return listener
    }

    fun removeUserTaskHistoryObserver(uid: String, listener: ValueEventListener) {
        database.reference
            .child(USERS_NODE)
            .child(uid)
            .child(TASKS_NODE)
            .removeEventListener(listener)
    }

    suspend fun getUserTaskHistory(uid: String): Map<String, Map<String, Int>> {
        val snapshot = database.reference
            .child(USERS_NODE)
            .child(uid)
            .child(TASKS_NODE)
            .get()
            .await()

        val history = linkedMapOf<String, Map<String, Int>>()
        snapshot.children.forEach { dateSnapshot ->
            val dateKey = dateSnapshot.key.orEmpty()
            if (dateKey.isNotBlank()) {
                history[dateKey] = dateSnapshot.getValue(String::class.java).toDailyTaskPoints()
            }
        }
        return history
    }

    suspend fun getFriends(uid: String): List<String> {
        return database.reference
            .child(USERS_NODE)
            .child(uid)
            .child(FRIENDS_NODE)
            .get()
            .await()
            .children
            .mapNotNull { child -> child.key?.takeIf { it.isNotBlank() } }
    }

    suspend fun addFriend(currentUid: String, friendUid: String) {
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

    suspend fun userExists(uid: String): Boolean {
        return database.reference
            .child(USERS_NODE)
            .child(uid)
            .get()
            .await()
            .exists()
    }

    fun observeFriends(
        uid: String,
        onChanged: (List<String>) -> Unit,
        onError: (String) -> Unit,
    ): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val friends = snapshot.children.mapNotNull { child ->
                    child.key?.takeIf { it.isNotBlank() }
                }
                onChanged(friends)
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
        return listener
    }

    fun removeFriendsObserver(uid: String, listener: ValueEventListener) {
        database.reference
            .child(USERS_NODE)
            .child(uid)
            .child(FRIENDS_NODE)
            .removeEventListener(listener)
    }

    private fun String?.toDailyTaskPoints(): Map<String, Int> {
        if (isNullOrBlank()) return emptyMap()

        return split(",")
            .mapNotNull { pair ->
                val parts = pair.split("=", ":", limit = 2)
                val taskId = parts.getOrNull(0)?.trim().orEmpty()
                val points = parts.getOrNull(1)?.trim()?.toIntOrNull()
                if (taskId.isBlank() || points == null) {
                    null
                } else {
                    taskId to points
                }
            }
            .toMap()
    }

    private fun Map<String, Int>.toDailyTaskPointsString(): String {
        return entries.joinToString(separator = ",") { (taskId, points) ->
            "$taskId=$points"
        }
    }

    private companion object {
        private const val USERS_NODE = "users"
        private const val TASKS_NODE = "tasks"
        private const val FRIENDS_NODE = "friends"
        private const val FAV_NODE = "fav"
        private const val EMAIL_FIELD = "email"
    }
}

data class FavoriteInfo(
    val email: String = "",
    val emoji: String = "",
    val top: String = "",
)
