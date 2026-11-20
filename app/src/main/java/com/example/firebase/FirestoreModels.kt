package com.example.firebase

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "Negm User",
    val username: String = "negm_user",
    val email: String = "",
    val photo: String = "",
    val language: String = "ar",
    val sensitivity: Long = 75L,
    val holdDurationMs: Long = 500L,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val batterySaver: Boolean = false,
    val preferredCamera: String = "front",
    val handPreference: String = "both",
    val pauseOnLeave: Boolean = true,
    val syncHistoryEnabled: Boolean = false,
    val activeProfileId: String = "default_profile",
    val totalCommandsExecuted: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "name" to name.trim().take(80).ifBlank { "Negm User" },
        "username" to username.trim().take(40).ifBlank { "negm_user" },
        "email" to email.trim().take(120),
        "photo" to photo.trim().take(500),
        "language" to if (language in listOf("ar", "en")) language else "ar",
        "sensitivity" to sensitivity.coerceIn(1L, 100L),
        "holdDurationMs" to holdDurationMs.coerceIn(150L, 2000L),
        "soundEnabled" to soundEnabled,
        "vibrationEnabled" to vibrationEnabled,
        "batterySaver" to batterySaver,
        "preferredCamera" to if (preferredCamera in listOf("front", "back")) preferredCamera else "front",
        "handPreference" to if (handPreference in listOf("both", "right", "left")) handPreference else "both",
        "pauseOnLeave" to pauseOnLeave,
        "syncHistoryEnabled" to syncHistoryEnabled,
        "activeProfileId" to activeProfileId.trim().take(64).ifBlank { "default_profile" },
        "totalCommandsExecuted" to totalCommandsExecuted.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class ControlProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "Default",
    val defaultProfile: Boolean = false,
    val mappings: Map<String, String> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "name" to name.trim().take(50).ifBlank { "Profile" },
        "defaultProfile" to defaultProfile,
        "mappings" to mappings,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class GestureConfig(
    @DocumentId val id: String = "",
    val userId: String = "",
    val gestureKey: String = "THUMBS_UP",
    val displayNameAr: String = "",
    val displayNameEn: String = "",
    val descriptionAr: String = "",
    val descriptionEn: String = "",
    val actionKey: String = "NONE",
    val targetAppPackage: String? = null,
    val targetAppName: String? = null,
    val enabled: Boolean = true,
    val custom: Boolean = false,
    val sensitivity: Long = 75L,
    val holdDurationMs: Long = 500L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("gestureKey", gestureKey.trim().take(40))
        put("displayNameAr", displayNameAr.trim().take(60).ifBlank { gestureKey })
        put("displayNameEn", displayNameEn.trim().take(60).ifBlank { gestureKey })
        put("descriptionAr", descriptionAr.trim().take(200))
        put("descriptionEn", descriptionEn.trim().take(200))
        put("actionKey", actionKey.trim().take(40).ifBlank { "NONE" })
        targetAppPackage?.takeIf { it.isNotBlank() }?.let { put("targetAppPackage", it.trim().take(120)) }
        targetAppName?.takeIf { it.isNotBlank() }?.let { put("targetAppName", it.trim().take(80)) }
        put("enabled", enabled)
        put("custom", custom)
        put("sensitivity", sensitivity.coerceIn(1L, 100L))
        put("holdDurationMs", holdDurationMs.coerceIn(150L, 2000L))
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@Keep
@IgnoreExtraProperties
data class SyncedActivityLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val gestureKey: String = "",
    val gestureLabel: String = "",
    val actionKey: String = "",
    val actionLabel: String = "",
    val succeeded: Boolean = true,
    val detailMessage: String = "",
    val confidence: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "gestureKey" to gestureKey.trim().take(40).ifBlank { "UNKNOWN" },
        "gestureLabel" to gestureLabel.trim().take(60).ifBlank { "Gesture" },
        "actionKey" to actionKey.trim().take(40).ifBlank { "NONE" },
        "actionLabel" to actionLabel.trim().take(80).ifBlank { "Action" },
        "succeeded" to succeeded,
        "detailMessage" to detailMessage.trim().take(200),
        "confidence" to confidence.coerceIn(0L, 100L),
        "createdAt" to FieldValue.serverTimestamp()
    )
}
