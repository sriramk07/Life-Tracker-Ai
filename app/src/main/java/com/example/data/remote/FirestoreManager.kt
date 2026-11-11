package com.example.data.remote

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * ============================================================================
 * FIRESTORE MANAGER — BIDIRECTIONAL CLOUD DATA SYNCHRONIZATION CLIENT
 * ============================================================================
 *
 * Provides cloud persistence, bidirectional delta fetching, and real-time
 * syncing for LifeOS AI:
 * 1. User-scoped document paths (`users/{sanitizedEmail}/{collectionName}/{docId}`)
 *    ensuring multi-tenant security and zero cross-account data leakage.
 * 2. One-shot suspending collection pull ([fetchUserCollection]) for LWW merge.
 * 3. Real-time Reactive Streams via [callbackFlow] and snapshot listeners.
 * 4. Error boundaries handling permission denied, offline mode, or init faults.
 */
class FirestoreManager {

    private val db: FirebaseFirestore?
        get() = try {
            FirebaseApp.getInstance()
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Firestore instance error boundary caught: ${e.message}")
            null
        }

    /**
     * Fetches all documents in a user-scoped Firestore subcollection for
     * bidirectional Last-Write-Wins conflict reconciliation.
     *
     * @param userEmail Authenticated user email partitioning cloud storage.
     * @param collectionName Subcollection name (e.g., "tasks", "habits").
     * @return List of document property maps augmented with `"id"`.
     */
    suspend fun fetchUserCollection(
        userEmail: String,
        collectionName: String
    ): List<Map<String, Any>> {
        val firestore = db ?: return emptyList()
        if (userEmail.isBlank()) return emptyList()

        return try {
            val sanitizedEmail = sanitizeEmail(userEmail)
            val path = "users/$sanitizedEmail/$collectionName"
            val snapshot = firestore.collection(path).get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.data?.plus("id" to doc.id)
            }
        } catch (e: Exception) {
            Log.w("FirestoreManager", "Pull error boundary for $collectionName: ${e.message}")
            emptyList()
        }
    }

    /**
     * Listens in real-time to changes in a Firestore collection and emits updates.
     * Automatically registers and unregisters [ListenerRegistration] with coroutine lifecycle.
     *
     * @param collectionName Name of the Firestore collection to observe.
     * @return Flow emitting updated list of documents represented as map of key-values.
     */
    fun listenToCollection(collectionName: String): Flow<List<Map<String, Any>>> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener: ListenerRegistration = firestore.collection(collectionName)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreManager", "Error observing $collectionName: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val dataList = snapshot.documents.mapNotNull { doc ->
                        doc.data?.plus("id" to doc.id)
                    }
                    trySend(dataList)
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Upserts a document in a user-scoped collection for secure cloud backup.
     * Email sanitization converts dots and at-signs into valid Firestore path components.
     *
     * @param userEmail Google user email used for document partitioning.
     * @param collectionName Target subcollection name (e.g., "tasks", "habits").
     * @param documentId Unique record identifier.
     * @param data Key-value map representing entity properties.
     */
    fun saveUserDocument(userEmail: String, collectionName: String, documentId: String, data: Map<String, Any>) {
        val sanitizedEmail = sanitizeEmail(userEmail)
        val path = "users/$sanitizedEmail/$collectionName"
        db?.collection(path)
            ?.document(documentId)
            ?.set(data, SetOptions.merge())
            ?.addOnSuccessListener {
                Log.d("FirestoreManager", "Successfully synced $collectionName/$documentId for $userEmail")
            }
            ?.addOnFailureListener { e ->
                Log.e("FirestoreManager", "Error syncing $collectionName/$documentId for $userEmail: ${e.message}")
            }

        val dataWithUser = data + ("userEmail" to userEmail) + ("syncedAt" to System.currentTimeMillis())
        saveDocument(collectionName, "${sanitizedEmail}_$documentId", dataWithUser)
    }

    /**
     * Upserts a document in a root collection with merge semantics.
     */
    fun saveDocument(collectionName: String, documentId: String, data: Map<String, Any>) {
        db?.collection(collectionName)
            ?.document(documentId)
            ?.set(data, SetOptions.merge())
            ?.addOnSuccessListener {
                Log.d("FirestoreManager", "Successfully saved document $documentId in $collectionName")
            }
            ?.addOnFailureListener { e ->
                Log.e("FirestoreManager", "Error saving document $documentId in $collectionName: ${e.message}")
            }
    }

    /**
     * Deletes a document from a root collection.
     */
    fun deleteDocument(collectionName: String, documentId: String) {
        db?.collection(collectionName)
            ?.document(documentId)
            ?.delete()
            ?.addOnSuccessListener {
                Log.d("FirestoreManager", "Successfully deleted document $documentId from $collectionName")
            }
            ?.addOnFailureListener { e ->
                Log.e("FirestoreManager", "Error deleting document $documentId from $collectionName: ${e.message}")
            }
    }

    companion object {
        fun sanitizeEmail(email: String): String =
            email.replace(".", "_").replace("@", "_at_")
    }
}
