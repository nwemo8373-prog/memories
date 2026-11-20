package com.example.firebase

import android.content.Context
import com.example.R
import com.example.gesturerecognition.CoreGestureType
import com.example.gesturerecognition.DefaultGestureSeed
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class NegmFirebaseRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun ensureUserInitialized(firebaseUser: FirebaseUser): Result<Unit> {
        val uid = firebaseUser.uid
        val userDocPath = "users/$uid"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid) { "Signed-in user mismatch." }

            val userRef = db.collection("users").document(uid)
            val snapshot = userRef.get().await()
            if (!snapshot.exists()) {
                val rawName = firebaseUser.displayName?.takeIf { it.isNotBlank() }
                    ?: firebaseUser.email?.substringBefore("@")
                    ?: "Negm User"
                val rawUsername = (firebaseUser.email?.substringBefore("@") ?: "negm_${uid.take(6)}")
                    .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                    .take(30)
                    .ifBlank { "negm_user" }

                val initialProfile = UserProfile(
                    userId = uid,
                    name = rawName.take(80),
                    username = rawUsername,
                    email = firebaseUser.email ?: "",
                    photo = firebaseUser.photoUrl?.toString() ?: "",
                    language = "ar",
                    sensitivity = 75L,
                    holdDurationMs = 500L,
                    soundEnabled = true,
                    vibrationEnabled = true,
                    batterySaver = false,
                    preferredCamera = "front",
                    handPreference = "both",
                    pauseOnLeave = true,
                    syncHistoryEnabled = false,
                    activeProfileId = "default_profile",
                    totalCommandsExecuted = 0L
                )
                userRef.set(initialProfile.toCreateMap()).await()
            }

            // Seed default control profile if missing
            val defaultProfileRef = userRef.collection("profiles").document("default_profile")
            if (!defaultProfileRef.get().await().exists()) {
                val defaultMappings = DefaultGestureSeed.defaultCoreGestures()
                    .associate { it.gestureKey to it.actionKey }
                val defaultProf = ControlProfile(
                    userId = uid,
                    name = "الافتراضي / Default",
                    defaultProfile = true,
                    mappings = defaultMappings
                )
                defaultProfileRef.set(defaultProf.toCreateMap()).await()
            }

            // Seed 9 core gestures if gestures subcollection is empty
            val existingGestures = userRef.collection("gestures").limit(1).get().await()
            if (existingGestures.isEmpty) {
                val batch = db.batch()
                DefaultGestureSeed.defaultCoreGestures().forEach { seed ->
                    val gRef = userRef.collection("gestures").document(seed.gestureKey)
                    val model = GestureConfig(
                        userId = uid,
                        gestureKey = seed.gestureKey,
                        displayNameAr = seed.displayNameAr,
                        displayNameEn = seed.displayNameEn,
                        descriptionAr = seed.descriptionAr,
                        descriptionEn = seed.descriptionEn,
                        actionKey = seed.actionKey,
                        enabled = true,
                        custom = false,
                        sensitivity = 75L,
                        holdDurationMs = 500L
                    )
                    batch.set(gRef, model.toCreateMap())
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userDocPath)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeGestures(userId: String): Flow<List<GestureConfig>> = flow {
        val path = "users/$userId/gestures"
        emitAll(
            db.collection("users").document(userId).collection("gestures")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        GestureConfig::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(compareBy<GestureConfig> { it.custom }.thenBy {
                        CoreGestureType.entries.indexOfFirst { c -> c.name == it.gestureKey }
                            .let { idx -> if (idx < 0) 999 else idx }
                    })
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeProfiles(userId: String): Flow<List<ControlProfile>> = flow {
        val path = "users/$userId/profiles"
        emitAll(
            db.collection("users").document(userId).collection("profiles")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ControlProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.defaultProfile }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun updateProfileInfo(name: String, username: String, photo: String? = null): Result<Unit> {
        var docPath = "users/unknown"
        return try {
            val uid = requireUserId()
            docPath = "users/$uid"
            val cleanName = name.trim().take(80)
            val cleanUsername = username.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(40)
            require(cleanName.isNotEmpty()) { "Name cannot be empty" }
            require(cleanUsername.length >= 2) { "Username must be at least 2 characters" }

            val updates = mutableMapOf<String, Any>(
                "name" to cleanName,
                "username" to cleanUsername,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (photo != null) {
                updates["photo"] = photo.trim().take(500)
            }
            db.document(docPath).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateSettings(
        language: String? = null,
        sensitivity: Int? = null,
        holdDurationMs: Int? = null,
        soundEnabled: Boolean? = null,
        vibrationEnabled: Boolean? = null,
        batterySaver: Boolean? = null,
        preferredCamera: String? = null,
        handPreference: String? = null,
        pauseOnLeave: Boolean? = null,
        syncHistoryEnabled: Boolean? = null,
        activeProfileId: String? = null
    ): Result<Unit> {
        var docPath = "users/unknown"
        return try {
            val uid = requireUserId()
            docPath = "users/$uid"
            val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
            language?.takeIf { it in listOf("ar", "en") }?.let { updates["language"] = it }
            sensitivity?.let { updates["sensitivity"] = it.coerceIn(1, 100).toLong() }
            holdDurationMs?.let { updates["holdDurationMs"] = it.coerceIn(150, 2000).toLong() }
            soundEnabled?.let { updates["soundEnabled"] = it }
            vibrationEnabled?.let { updates["vibrationEnabled"] = it }
            batterySaver?.let { updates["batterySaver"] = it }
            preferredCamera?.takeIf { it in listOf("front", "back") }?.let { updates["preferredCamera"] = it }
            handPreference?.takeIf { it in listOf("both", "right", "left") }?.let { updates["handPreference"] = it }
            pauseOnLeave?.let { updates["pauseOnLeave"] = it }
            syncHistoryEnabled?.let { updates["syncHistoryEnabled"] = it }
            activeProfileId?.takeIf { it.isNotBlank() }?.let { updates["activeProfileId"] = it.take(64) }

            db.document(docPath).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun incrementExecutedCommandCount(): Result<Unit> {
        var docPath = "users/unknown"
        return try {
            val uid = requireUserId()
            docPath = "users/$uid"
            db.document(docPath).update(
                mapOf(
                    "totalCommandsExecuted" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveGestureConfig(config: GestureConfig): Result<Unit> {
        var docPath = "users/unknown/gestures"
        return try {
            val uid = requireUserId()
            val docId = config.id.takeIf { it.matches(Regex("^[a-zA-Z0-9_\\-]+$")) }
                ?: if (config.custom) "custom_${UUID.randomUUID().toString().replace("-", "").take(10)}"
                else config.gestureKey
            val ref = db.collection("users").document(uid).collection("gestures").document(docId)
            docPath = ref.path
            val existingDoc = ref.get().await()
            if (!existingDoc.exists()) {
                ref.set(config.copy(userId = uid).toCreateMap()).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "gestureKey" to config.gestureKey.trim().take(40),
                    "displayNameAr" to config.displayNameAr.trim().take(60).ifBlank { config.gestureKey },
                    "displayNameEn" to config.displayNameEn.trim().take(60).ifBlank { config.gestureKey },
                    "descriptionAr" to config.descriptionAr.trim().take(200),
                    "descriptionEn" to config.descriptionEn.trim().take(200),
                    "actionKey" to config.actionKey.trim().take(40),
                    "enabled" to config.enabled,
                    "sensitivity" to config.sensitivity.coerceIn(1L, 100L),
                    "holdDurationMs" to config.holdDurationMs.coerceIn(150L, 2000L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                config.targetAppPackage?.takeIf { it.isNotBlank() }?.let {
                    updates["targetAppPackage"] = it.trim().take(120)
                }
                config.targetAppName?.takeIf { it.isNotBlank() }?.let {
                    updates["targetAppName"] = it.trim().take(80)
                }
                ref.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomGesture(gestureId: String): Result<Unit> {
        var docPath = "users/unknown/gestures/$gestureId"
        return try {
            val uid = requireUserId()
            require(gestureId.matches(Regex("^[a-zA-Z0-9_\\-]+$"))) { "Invalid gesture ID" }
            val ref = db.collection("users").document(uid).collection("gestures").document(gestureId)
            docPath = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveControlProfile(profile: ControlProfile): Result<String> {
        var docPath = "users/unknown/profiles"
        return try {
            val uid = requireUserId()
            val docId = profile.id.takeIf { it.matches(Regex("^[a-zA-Z0-9_\\-]+$")) }
                ?: "prof_${UUID.randomUUID().toString().replace("-", "").take(10)}"
            val ref = db.collection("users").document(uid).collection("profiles").document(docId)
            docPath = ref.path
            val snapshot = ref.get().await()
            if (!snapshot.exists()) {
                ref.set(profile.copy(userId = uid).toCreateMap()).await()
            } else {
                ref.update(
                    mapOf(
                        "name" to profile.name.trim().take(50).ifBlank { "Profile" },
                        "defaultProfile" to profile.defaultProfile,
                        "mappings" to profile.mappings,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteControlProfile(profileId: String): Result<Unit> {
        var docPath = "users/unknown/profiles/$profileId"
        return try {
            val uid = requireUserId()
            require(profileId != "default_profile") { "Cannot delete default profile" }
            val ref = db.collection("users").document(uid).collection("profiles").document(profileId)
            docPath = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun logActivityToCloud(entry: SyncedActivityLog): Result<Unit> {
        var docPath = "users/unknown/activityLogs"
        return try {
            val uid = requireUserId()
            val docId = "log_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
            val ref = db.collection("users").document(uid).collection("activityLogs").document(docId)
            docPath = ref.path
            ref.set(entry.copy(userId = uid).toCreateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteAllUserData(): Result<Unit> {
        var docPath = "users/unknown"
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            docPath = userRef.path
            listOf("gestures", "profiles", "activityLogs").forEach { sub ->
                val docs = userRef.collection(sub).get().await()
                if (!docs.isEmpty) {
                    val batch = db.batch()
                    docs.documents.forEach { batch.delete(it.reference) }
                    batch.commit().await()
                }
            }
            userRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
