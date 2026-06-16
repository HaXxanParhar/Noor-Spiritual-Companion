package com.parhar.noor.data.remote.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.parhar.noor.data.remote.UserRemoteDataSource
import com.parhar.noor.data.remote.dto.RemoteFavoriteInfo
import com.parhar.noor.data.remote.dto.RemoteSteakSnapshot
import com.parhar.noor.data.remote.dto.RemoteUserProfile
import kotlinx.coroutines.tasks.await
import java.io.Closeable

class FirebaseUserRemoteDataSource(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance(),
) : UserRemoteDataSource {

    override suspend fun fetchUserProfile(uid: String): RemoteUserProfile? {
        return database.reference
            .child(USERS_NODE)
            .child(uid)
            .get()
            .await()
            .getValue(RemoteUserProfile::class.java)
    }

    override suspend fun pushUserProfile(profile: RemoteUserProfile) {
        database.reference
            .child(USERS_NODE)
            .child(profile.uid)
            .updateChildren(
                mapOf(
                    "uid" to profile.uid,
                    "email" to profile.email,
                    "name" to profile.name,
                    "gender" to profile.gender,
                ),
            )
            .await()
    }

    override suspend fun userExists(uid: String): Boolean {
        return database.reference.child(USERS_NODE).child(uid).get().await().exists()
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

    override fun observeUserProfile(
        uid: String,
        onChanged: (RemoteUserProfile?) -> Unit,
        onError: (String) -> Unit,
    ): AutoCloseable {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onChanged(snapshot.getValue(RemoteUserProfile::class.java))
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

    private class CloseableListener(private val onClose: () -> Unit) : Closeable {
        override fun close() = onClose()
    }

    private companion object {
        private const val USERS_NODE = "users"
        private const val TASKS_NODE = "tasks"
        private const val FRIENDS_NODE = "friends"
        private const val FAV_NODE = "fav"
        private const val STEAK_NODE = "steak"
    }
}
