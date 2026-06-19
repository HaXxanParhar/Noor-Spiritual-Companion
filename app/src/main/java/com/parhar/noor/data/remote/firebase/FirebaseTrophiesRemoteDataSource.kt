package com.parhar.noor.data.remote.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.parhar.noor.data.remote.TrophiesRemoteDataSource
import com.parhar.noor.data.remote.dto.RemoteTrophy
import kotlinx.coroutines.tasks.await
import java.io.Closeable

class FirebaseTrophiesRemoteDataSource(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance(),
) : TrophiesRemoteDataSource {

    override suspend fun fetchTrophies(): List<RemoteTrophy> {
        val snapshot = database.reference.child(TROPHIES_NODE).get().await()
        return snapshot.children.mapNotNull { child -> child.toRemoteTrophy() }
    }

    override suspend fun pushTrophy(trophy: RemoteTrophy) {
        database.reference
            .child(TROPHIES_NODE)
            .child(trophy.id)
            .setValue(
                mapOf(
                    NAME_FIELD to trophy.name,
                    ICON_FIELD to trophy.icon,
                    REQUIREMENT_FIELD to trophy.requirement,
                ),
            )
            .await()
    }

    override suspend fun deleteTrophy(trophyId: String) {
        database.reference
            .child(TROPHIES_NODE)
            .child(trophyId)
            .removeValue()
            .await()
    }

    override fun observeTrophies(
        onChanged: (List<RemoteTrophy>) -> Unit,
        onError: (String) -> Unit,
    ): AutoCloseable {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onChanged(snapshot.children.mapNotNull { child -> child.toRemoteTrophy() })
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        }
        database.reference.child(TROPHIES_NODE).addValueEventListener(listener)
        return CloseableListener {
            database.reference.child(TROPHIES_NODE).removeEventListener(listener)
        }
    }

    private fun DataSnapshot.toRemoteTrophy(): RemoteTrophy? {
        val trophyId = key?.takeIf { it.isNotBlank() } ?: return null
        return RemoteTrophy(
            id = trophyId,
            name = child(NAME_FIELD).getValue(String::class.java).orEmpty(),
            icon = child(ICON_FIELD).getValue(String::class.java).orEmpty(),
            requirement = child(REQUIREMENT_FIELD).getValue(Int::class.java)
                ?: child(REQUIREMENT_FIELD).getValue(Long::class.java)?.toInt()
                ?: 0,
        )
    }

    private class CloseableListener(private val onClose: () -> Unit) : Closeable {
        override fun close() = onClose()
    }

    private companion object {
        private const val TROPHIES_NODE = "trophies"
        private const val NAME_FIELD = "name"
        private const val ICON_FIELD = "icon"
        private const val REQUIREMENT_FIELD = "requirement"
    }
}
