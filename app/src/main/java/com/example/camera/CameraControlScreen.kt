package com.example.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.firebase.ControlProfile
import com.example.firebase.GestureConfig
import com.example.firebase.UserProfile
import com.example.gestureactions.DeviceActionExecutor
import com.example.gesturerecognition.CoreGestureType
import com.example.gesturerecognition.GestureActionType
import com.example.gesturerecognition.GestureClassifier
import com.example.handtracking.HandLandmarkOverlay
import com.example.handtracking.MediaPipeHandTracker
import com.example.handtracking.TrackedHand
import com.example.notifications.FeedbackManager
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlassCard
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.NeonMint
import com.example.ui.theme.ObsidianBackdrop
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.StatusPill
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.concurrent.Executors

@Composable
fun CameraControlScreen(
    userProfile: UserProfile?,
    gestures: List<GestureConfig>,
    activeProfile: ControlProfile?,
    testTargetGestureKey: String?,
    onClose: () -> Unit,
    onInAppNavigate: (String) -> Unit,
    onStageCommand: (String) -> Unit,
    onRecordExecution: (
        gestureKey: String,
        gestureLabel: String,
        actionKey: String,
        actionLabel: String,
        succeeded: Boolean,
        detail: String,
        confidence: Int,
        syncToCloud: Boolean
    ) -> Unit
) {
    BackHandler { onClose() }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val strings = LocalAppStrings.current
    val lang = LocalAppLanguage.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val initialFront = (userProfile?.preferredCamera ?: "front") == "front"
    var isFrontCamera by remember(userProfile?.preferredCamera) { mutableStateOf(initialFront) }
    var isPaused by remember { mutableStateOf(false) }
    var showLandmarks by remember { mutableStateOf(true) }

    var trackedHands by remember { mutableStateOf<List<TrackedHand>>(emptyList()) }
    var detectedGesture by remember { mutableStateOf<CoreGestureType?>(null) }
    var detectedConfidence by remember { mutableIntStateOf(0) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var gestureLocked by remember { mutableStateOf(false) }
    var lastStatusText by remember { mutableStateOf(strings.statusScanning) }
    var lastActionFeedback by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var inferenceMs by remember { mutableLongStateOf(0L) }
    var trackerError by remember { mutableStateOf<String?>(null) }

    // Hold-duration & anti-repeat cooldown state
    var candidateGesture by remember { mutableStateOf<CoreGestureType?>(null) }
    var candidateStartMs by remember { mutableLongStateOf(0L) }
    var lastExecutedGesture by remember { mutableStateOf<CoreGestureType?>(null) }
    var lastExecutedTimestampMs by remember { mutableLongStateOf(0L) }
    var lastFrameProcessedMs by remember { mutableLongStateOf(0L) }

    val actionExecutor = remember(context) { DeviceActionExecutor(context) }
    val feedbackManager = remember(context) { FeedbackManager(context) }
    val classifier = remember { GestureClassifier() }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    // Automatically pause or stop when leaving foreground if enabled
    DisposableEffect(lifecycleOwner, userProfile?.pauseOnLeave) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && (userProfile?.pauseOnLeave != false)) {
                isPaused = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            analysisExecutor.shutdown()
        }
    }

    if (!hasCameraPermission) {
        ObsidianBackdrop {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = ElectricBlue,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = strings.cameraPermissionNeeded,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = strings.cameraPermissionRationale,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricBlue,
                                contentColor = ObsidianBlack
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("grant_camera_permission_button")
                        ) {
                            Text(strings.grantCameraPermission, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onClose,
                            modifier = Modifier.weight(0.6f)
                        ) {
                            Text(strings.cancel, color = TextPrimary)
                        }
                    }
                }
            }
        }
        return
    }

    val sensitivity = (userProfile?.sensitivity ?: 75L).toInt().coerceIn(1, 100)
    val defaultHoldMs = (userProfile?.holdDurationMs ?: 500L).coerceIn(150L, 2000L)
    val handPref = userProfile?.handPreference ?: "both"
    val batterySaver = userProfile?.batterySaver ?: false

    val handTracker = remember(context) {
        MediaPipeHandTracker(
            context = context,
            onResult = { frame ->
                inferenceMs = frame.inferenceTimeMs
                if (isPaused) {
                    trackedHands = emptyList()
                    holdProgress = 0f
                    gestureLocked = false
                    return@MediaPipeHandTracker
                }

                // Filter handedness preference
                val filteredHands = when (handPref) {
                    "right" -> frame.hands.filter { it.handedness.equals("Right", ignoreCase = true) }
                    "left" -> frame.hands.filter { it.handedness.equals("Left", ignoreCase = true) }
                    else -> frame.hands
                }
                trackedHands = filteredHands

                val primaryHand = filteredHands.firstOrNull()
                if (primaryHand == null) {
                    detectedGesture = null
                    detectedConfidence = 0
                    candidateGesture = null
                    holdProgress = 0f
                    gestureLocked = false
                    lastStatusText = strings.statusScanning
                    classifier.resetMotionHistory()
                    return@MediaPipeHandTracker
                }

                val nowMs = SystemClock.uptimeMillis()
                val classified = classifier.classify(
                    landmarks = primaryHand.landmarks,
                    handedness = primaryHand.handedness,
                    sensitivity = sensitivity,
                    timestampMs = nowMs
                )

                if (classified == null) {
                    detectedGesture = null
                    detectedConfidence = 0
                    candidateGesture = null
                    holdProgress = 0f
                    gestureLocked = false
                    lastStatusText = strings.statusScanning
                    return@MediaPipeHandTracker
                }

                val gType = classified.gesture
                val confPct = (classified.confidence * 100f).toInt().coerceIn(50, 99)
                detectedGesture = gType
                detectedConfidence = confPct

                // Check gesture configuration
                val config = gestures.firstOrNull { it.gestureKey == gType.name }
                if (config != null && !config.enabled && testTargetGestureKey == null) {
                    lastStatusText = if (lang == "ar") "الإشارة معطّلة" else "Gesture Disabled"
                    holdProgress = 0f
                    gestureLocked = false
                    return@MediaPipeHandTracker
                }

                // Dynamic swipes fire immediately on detection (still respecting cooldown)
                val isDynamicSwipe = gType == CoreGestureType.SWIPE_LEFT || gType == CoreGestureType.SWIPE_RIGHT
                val requiredHoldMs = if (isDynamicSwipe) 0L else (config?.holdDurationMs ?: defaultHoldMs)

                if (candidateGesture != gType) {
                    candidateGesture = gType
                    candidateStartMs = nowMs
                }

                val elapsed = nowMs - candidateStartMs
                val progress = if (requiredHoldMs <= 0L) 1f else (elapsed.toFloat() / requiredHoldMs.toFloat()).coerceIn(0f, 1f)
                holdProgress = progress

                val cooldownMs = 1700L
                val inCooldown = (lastExecutedGesture == gType) && (nowMs - lastExecutedTimestampMs < cooldownMs)

                if (progress >= 1f && !inCooldown) {
                    gestureLocked = true
                    lastExecutedGesture = gType
                    lastExecutedTimestampMs = nowMs

                    val gLabel = if (lang == "ar") (config?.displayNameAr ?: gType.titleAr) else (config?.displayNameEn ?: gType.titleEn)

                    // In Test Mode, highlight success without triggering destructive system actions if testing a different pose
                    if (testTargetGestureKey != null) {
                        val matched = gType.name == testTargetGestureKey
                        val msg = if (matched) {
                            if (lang == "ar") "تم اختبار $gLabel بنجاح!" else "Verified $gLabel successfully!"
                        } else {
                            if (lang == "ar") "تم رصد $gLabel ($confPct%)" else "Detected $gLabel ($confPct%)"
                        }
                        lastStatusText = strings.statusExecuted
                        lastActionFeedback = Pair(matched, msg)
                        feedbackManager.triggerFeedback(
                            vibrationEnabled = userProfile?.vibrationEnabled ?: true,
                            soundEnabled = userProfile?.soundEnabled ?: true,
                            success = matched
                        )
                    } else {
                        // Resolve mapped action from active ControlProfile or GestureConfig
                        val mappedActionKey = activeProfile?.mappings?.get(gType.name)
                            ?: config?.actionKey
                            ?: gType.defaultAction.name
                        val actionType = GestureActionType.fromKey(mappedActionKey)
                        val actionLabel = if (lang == "ar") actionType.labelAr else actionType.labelEn

                        val execResult = actionExecutor.execute(
                            action = actionType,
                            targetAppPackage = config?.targetAppPackage,
                            activeCameraTorchSetter = { enable ->
                                val cam = boundCamera
                                if (cam != null && !isFrontCamera && cam.cameraInfo.hasFlashUnit()) {
                                    cam.cameraControl.enableTorch(enable)
                                    true
                                } else {
                                    false
                                }
                            },
                            onInAppNavigate = onInAppNavigate,
                            onPauseRequested = { isPaused = true },
                            onStageControl = onStageCommand
                        )

                        val detailMsg = if (lang == "ar") execResult.detailAr else execResult.detailEn
                        lastStatusText = if (execResult.succeeded) strings.statusExecuted else strings.statusDetected
                        lastActionFeedback = Pair(execResult.succeeded, detailMsg)

                        feedbackManager.triggerFeedback(
                            vibrationEnabled = userProfile?.vibrationEnabled ?: true,
                            soundEnabled = userProfile?.soundEnabled ?: true,
                            success = execResult.succeeded
                        )

                        onRecordExecution(
                            gType.name,
                            gLabel,
                            actionType.name,
                            actionLabel,
                            execResult.succeeded,
                            detailMsg,
                            confPct,
                            userProfile?.syncHistoryEnabled ?: false
                        )
                    }
                } else {
                    gestureLocked = false
                    lastStatusText = strings.statusDetected
                }
            },
            onError = { err ->
                trackerError = err
            }
        )
    }

    DisposableEffect(handTracker) {
        onDispose {
            handTracker.close()
        }
    }

    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(isFrontCamera, batterySaver, lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()

                imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                    val now = SystemClock.uptimeMillis()
                    val minIntervalMs = if (batterySaver) 125L else 45L
                    if (now - lastFrameProcessedMs < minIntervalMs) {
                        imageProxy.close()
                    } else {
                        lastFrameProcessedMs = now
                        handTracker.detectLiveStream(imageProxy, isFrontCamera)
                    }
                }

                val selector = if (isFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                cameraProvider.unbindAll()
                boundCamera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                trackerError = e.localizedMessage ?: "Camera unavailable"
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        // Live CameraX Preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Glowing 21-Point Landmark + Aura Overlay
        HandLandmarkOverlay(
            hands = trackedHands,
            holdProgress = holdProgress,
            gestureLocked = gestureLocked,
            showLandmarks = showLandmarks,
            modifier = Modifier.fillMaxSize()
        )

        // Top Control HUD
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(
                    label = if (isPaused) strings.statusPaused else "$lastStatusText (${inferenceMs}ms)",
                    active = !isPaused && trackedHands.isNotEmpty(),
                    icon = Icons.Filled.CameraAlt,
                    activeColor = if (gestureLocked) NeonMint else ElectricBlue,
                    inactiveColor = if (isPaused) AmberGlow else CyberViolet
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { showLandmarks = !showLandmarks },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ObsidianBlack.copy(alpha = 0.72f))
                            .border(1.dp, ElectricBlue.copy(alpha = 0.4f), CircleShape)
                            .testTag("toggle_landmarks_button")
                    ) {
                        Icon(
                            imageVector = if (showLandmarks) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = "Toggle Landmarks",
                            tint = ElectricBlue
                        )
                    }
                    IconButton(
                        onClick = { isFrontCamera = !isFrontCamera },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ObsidianBlack.copy(alpha = 0.72f))
                            .border(1.dp, CyberViolet.copy(alpha = 0.4f), CircleShape)
                            .testTag("flip_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FlipCameraAndroid,
                            contentDescription = strings.switchCamera,
                            tint = TextPrimary
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(CrimsonAlert.copy(alpha = 0.25f))
                            .border(1.dp, CrimsonAlert.copy(alpha = 0.6f), CircleShape)
                            .testTag("end_camera_session_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = strings.endSession,
                            tint = TextPrimary
                        )
                    }
                }
            }

            if (testTargetGestureKey != null) {
                val target = CoreGestureType.fromKey(testTargetGestureKey)
                GlassCard(glowColor = CyberViolet) {
                    Text(
                        text = if (lang == "ar") {
                            "وضع الاختبار: جرّب إشارة ${target?.titleAr ?: testTargetGestureKey}"
                        } else {
                            "Test Mode: Perform ${target?.titleEn ?: testTargetGestureKey}"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = ElectricBlue
                    )
                }
            }

            if (trackerError != null) {
                GlassCard(glowColor = CrimsonAlert) {
                    Text(
                        text = trackerError!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CrimsonAlert
                    )
                }
            }
        }

        // Bottom Detection & Action Feedback HUD
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnimatedVisibility(
                visible = lastActionFeedback != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                lastActionFeedback?.let { (ok, msg) ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        glowColor = if (ok) NeonMint else AmberGlow
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = if (ok) NeonMint else AmberGlow
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                glowColor = if (gestureLocked) NeonMint else ElectricBlue
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = detectedGesture?.let {
                                if (lang == "ar") "${it.badgeSymbol} ${it.titleAr}" else "${it.badgeSymbol} ${it.titleEn}"
                            } ?: strings.statusScanning,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (gestureLocked) NeonMint else ElectricBlue
                        )
                        Text(
                            text = "${strings.confidenceLabel}: $detectedConfidence%  •  ${if (isFrontCamera) strings.frontCamera else strings.backCamera}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    Button(
                        onClick = { isPaused = !isPaused },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPaused) NeonMint else CyberViolet,
                            contentColor = ObsidianBlack
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("pause_resume_camera_button")
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPaused) strings.resumeSession else strings.pauseSession,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = strings.holdProgressLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { holdProgress },
                    color = if (gestureLocked) NeonMint else ElectricBlue,
                    trackColor = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(CircleShape)
                )
            }
        }
    }
}
