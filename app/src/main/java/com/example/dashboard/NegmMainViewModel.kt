package com.example.dashboard

import android.util.Log
import com.example.firebase.ControlProfile
import com.example.firebase.GestureConfig
import com.example.firebase.NegmFirebaseRepository
import com.example.firebase.SyncedActivityLog
import com.example.firebase.UiState
import com.example.firebase.UserProfile
import com.example.gesturerecognition.CoreGestureType
import com.example.gesturerecognition.GestureActionType
import com.example.history.ActivityLogEntity
import com.example.history.HistoryLocalRepository
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StageMediaItem(
    val index: Int,
    val titleAr: String,
    val titleEn: String,
    val subtitleAr: String,
    val subtitleEn: String,
    val bpmOrTag: String
)

class NegmMainViewModel(
    private val firebaseRepository: NegmFirebaseRepository,
    private val historyRepository: HistoryLocalRepository,
    private val currentUserId: String,
    firebaseUser: FirebaseUser?
) : ViewModel() {

    private val _initStatus = MutableStateFlow<String?>(null)
    val initStatus: StateFlow<String?> = _initStatus.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _lastConfidence = MutableStateFlow(0)
    val lastConfidence: StateFlow<Int> = _lastConfidence.asStateFlow()

    private val _stageIndex = MutableStateFlow(0)
    val stageIndex: StateFlow<Int> = _stageIndex.asStateFlow()

    private val _stagePlaying = MutableStateFlow(false)
    val stagePlaying: StateFlow<Boolean> = _stagePlaying.asStateFlow()

    val stageItems = listOf(
        StageMediaItem(0, "موجة النيون السيبرانية", "Cyber Neon Wave", "تحكم بالمسار عبر علامة النصر", "Toggle playback with Peace Sign", "128 BPM"),
        StageMediaItem(1, "أفق الذكاء الاصطناعي", "AI Horizon Pulse", "اسحب يميناً أو يساراً للتبديل", "Swipe Right / Left to switch", "96 BPM"),
        StageMediaItem(2, "تردد الأوبسيديان", "Obsidian Frequency", "ارفع السبابة لرفع الصوت", "One Finger up to boost volume", "114 BPM"),
        StageMediaItem(3, "كوكبة المفاصل الـ 21", "21-Node Constellation", "إشارة لأسفل لخفض الصوت", "Point down to lower volume", "105 BPM")
    )

    init {
        if (firebaseUser != null && firebaseUser.uid == currentUserId) {
            viewModelScope.launch {
                val res = firebaseRepository.ensureUserInitialized(firebaseUser)
                if (res.isFailure) {
                    _initStatus.value = res.exceptionOrNull()?.localizedMessage
                }
            }
        }
    }

    val profileState: StateFlow<UiState<UserProfile?>> = firebaseRepository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing profile", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val gesturesState: StateFlow<UiState<List<GestureConfig>>> = firebaseRepository.observeGestures(currentUserId)
        .map<List<GestureConfig>, UiState<List<GestureConfig>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing gestures", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load gestures"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val profilesState: StateFlow<UiState<List<ControlProfile>>> = firebaseRepository.observeProfiles(currentUserId)
        .map<List<ControlProfile>, UiState<List<ControlProfile>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing control profiles", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load profiles"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val historyLogs: StateFlow<List<ActivityLogEntity>> = historyRepository.observeUserHistory(currentUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showMessage(message: String) {
        _toastMessage.value = message
    }

    fun handleStageCommand(command: String) {
        when (command) {
            "PLAY_PAUSE" -> _stagePlaying.value = !_stagePlaying.value
            "NEXT" -> _stageIndex.value = (_stageIndex.value + 1) % stageItems.size
            "PREV" -> _stageIndex.value = (_stageIndex.value - 1 + stageItems.size) % stageItems.size
        }
    }

    fun recordGestureExecution(
        gestureKey: String,
        gestureLabel: String,
        actionKey: String,
        actionLabel: String,
        succeeded: Boolean,
        detailMessage: String,
        confidencePercent: Int,
        syncToCloud: Boolean
    ) {
        _lastConfidence.value = confidencePercent
        viewModelScope.launch {
            historyRepository.recordLog(
                ActivityLogEntity(
                    userId = currentUserId,
                    gestureKey = gestureKey,
                    gestureLabel = gestureLabel,
                    actionKey = actionKey,
                    actionLabel = actionLabel,
                    succeeded = succeeded,
                    detailMessage = detailMessage,
                    confidencePercent = confidencePercent
                )
            )
            if (succeeded) {
                firebaseRepository.incrementExecutedCommandCount()
            }
            if (syncToCloud) {
                firebaseRepository.logActivityToCloud(
                    SyncedActivityLog(
                        userId = currentUserId,
                        gestureKey = gestureKey,
                        gestureLabel = gestureLabel,
                        actionKey = actionKey,
                        actionLabel = actionLabel,
                        succeeded = succeeded,
                        detailMessage = detailMessage,
                        confidence = confidencePercent.toLong()
                    )
                )
            }
        }
    }

    fun clearLocalHistory() {
        viewModelScope.launch {
            historyRepository.clearUserHistory(currentUserId)
        }
    }

    fun updateSettings(
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
    ) {
        viewModelScope.launch {
            val res = firebaseRepository.updateSettings(
                language = language,
                sensitivity = sensitivity,
                holdDurationMs = holdDurationMs,
                soundEnabled = soundEnabled,
                vibrationEnabled = vibrationEnabled,
                batterySaver = batterySaver,
                preferredCamera = preferredCamera,
                handPreference = handPreference,
                pauseOnLeave = pauseOnLeave,
                syncHistoryEnabled = syncHistoryEnabled,
                activeProfileId = activeProfileId
            )
            if (res.isFailure) {
                _toastMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to update settings"
            }
        }
    }

    fun updateProfileInfo(name: String, username: String, photo: String? = null) {
        viewModelScope.launch {
            val res = firebaseRepository.updateProfileInfo(name, username, photo)
            _toastMessage.value = if (res.isSuccess) {
                "✓"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to update profile"
            }
        }
    }

    fun saveGesture(config: GestureConfig) {
        viewModelScope.launch {
            val res = firebaseRepository.saveGestureConfig(config)
            if (res.isFailure) {
                _toastMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save gesture"
            }
        }
    }

    fun deleteCustomGesture(gestureId: String) {
        viewModelScope.launch {
            val res = firebaseRepository.deleteCustomGesture(gestureId)
            if (res.isFailure) {
                _toastMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete gesture"
            }
        }
    }

    fun createControlProfile(name: String, baseMappings: Map<String, String>) {
        viewModelScope.launch {
            val mappings = if (baseMappings.isNotEmpty()) baseMappings else {
                CoreGestureType.entries.associate { it.name to it.defaultAction.name }
            }
            val res = firebaseRepository.saveControlProfile(
                ControlProfile(
                    userId = currentUserId,
                    name = name,
                    defaultProfile = false,
                    mappings = mappings
                )
            )
            res.onSuccess { newId ->
                firebaseRepository.updateSettings(activeProfileId = newId)
            }.onFailure { err ->
                _toastMessage.value = err.localizedMessage ?: "Failed to create profile"
            }
        }
    }

    fun updateControlProfileMapping(profile: ControlProfile, gestureKey: String, action: GestureActionType) {
        viewModelScope.launch {
            val updatedMappings = profile.mappings.toMutableMap().apply {
                put(gestureKey, action.name)
            }
            val res = firebaseRepository.saveControlProfile(profile.copy(mappings = updatedMappings))
            if (res.isFailure) {
                _toastMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to update mapping"
            }
        }
    }

    fun deleteControlProfile(profileId: String) {
        viewModelScope.launch {
            val res = firebaseRepository.deleteControlProfile(profileId)
            if (res.isSuccess) {
                firebaseRepository.updateSettings(activeProfileId = "default_profile")
            } else {
                _toastMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Cannot delete profile"
            }
        }
    }

    suspend fun deleteAccountData(): Result<Unit> {
        historyRepository.clearUserHistory(currentUserId)
        return firebaseRepository.deleteAllUserData()
    }

    private companion object {
        const val TAG = "NegmMainVM"
    }
}
