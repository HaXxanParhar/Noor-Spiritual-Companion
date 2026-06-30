package com.parhar.noor.data.remote.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.parhar.noor.data.remote.AyatsRemoteDataSource
import com.parhar.noor.data.remote.dto.RemoteAyat
import kotlinx.coroutines.tasks.await
import java.io.Closeable

class FirebaseAyatsRemoteDataSource(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance(),
) : AyatsRemoteDataSource {

    override suspend fun fetchAyats(): List<RemoteAyat> {
        val snapshot = database.reference.child(AYATS_NODE).get().await()
        return snapshot.children.mapNotNull { child -> child.toRemoteAyat() }
    }

    override suspend fun pushAyat(ayat: RemoteAyat) {
        database.reference
            .child(AYATS_NODE)
            .child(ayat.id)
            .setValue(
                mapOf(
                    AYAT_FIELD to ayat.ayat,
                    ENGLISH_FIELD to ayat.english,
                    URDU_FIELD to ayat.urdu,
                    REFERENCE_FIELD to ayat.reference,
                ),
            )
            .await()
    }

    override suspend fun deleteAyat(ayatId: String) {
        database.reference
            .child(AYATS_NODE)
            .child(ayatId)
            .removeValue()
            .await()
    }

    override fun observeAyats(
        onChanged: (List<RemoteAyat>) -> Unit,
        onError: (String) -> Unit,
    ): AutoCloseable {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onChanged(snapshot.children.mapNotNull { child -> child.toRemoteAyat() })
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        }
        database.reference.child(AYATS_NODE).addValueEventListener(listener)
        return CloseableListener {
            database.reference.child(AYATS_NODE).removeEventListener(listener)
        }
    }

    private fun DataSnapshot.toRemoteAyat(): RemoteAyat? {
        val ayatId = key?.takeIf { it.isNotBlank() } ?: return null
        return RemoteAyat(
            id = ayatId,
            ayat = child(AYAT_FIELD).getValue(String::class.java).orEmpty(),
            english = child(ENGLISH_FIELD).getValue(String::class.java).orEmpty(),
            urdu = child(URDU_FIELD).getValue(String::class.java).orEmpty(),
            reference = child(REFERENCE_FIELD).getValue(String::class.java).orEmpty(),
        )
    }

    private class CloseableListener(private val onClose: () -> Unit) : Closeable {
        override fun close() = onClose()
    }

    private companion object {
        private const val AYATS_NODE = "ayats"
        private const val AYAT_FIELD = "ayat"
        private const val ENGLISH_FIELD = "english"
        private const val URDU_FIELD = "urdu"
        private const val REFERENCE_FIELD = "reference"
    }
}
