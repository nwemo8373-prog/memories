package com.example.handtracking

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

data class TrackedHand(
    val landmarks: List<NormalizedLandmark>,
    val handedness: String,
    val score: Float
)

data class HandTrackingFrameResult(
    val hands: List<TrackedHand>,
    val inferenceTimeMs: Long,
    val imageWidth: Int,
    val imageHeight: Int
)

/**
 * Local on-device Google MediaPipe Hand Landmarker wrapper.
 * Processes CameraX frames entirely in-memory with zero network uploads.
 */
class MediaPipeHandTracker(
    context: Context,
    private val onResult: (HandTrackingFrameResult) -> Unit,
    private val onError: (String) -> Unit
) {
    private val appContext = context.applicationContext
    private var handLandmarker: HandLandmarker? = null

    @Volatile
    private var isClosed = false

    init {
        setupLandmarker()
    }

    private fun setupLandmarker() {
        try {
            val baseOptions = BaseOptions.builder()
                .setDelegate(Delegate.CPU)
                .setModelAssetPath("hand_landmarker.task")
                .build()

            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinHandDetectionConfidence(0.55f)
                .setMinTrackingConfidence(0.55f)
                .setMinHandPresenceConfidence(0.55f)
                .setNumHands(2)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener { result, input ->
                    handleLandmarkerResult(result, input.width, input.height)
                }
                .setErrorListener { error ->
                    Log.e(TAG, "MediaPipe error: ${error.message}", error)
                    onError(error.message ?: "MediaPipe hand tracking error")
                }
                .build()

            handLandmarker = HandLandmarker.createFromOptions(appContext, options)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaPipe HandLandmarker", e)
            onError(e.localizedMessage ?: "Failed to initialize HandLandmarker")
        }
    }

    fun detectLiveStream(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        if (isClosed) {
            imageProxy.close()
            return
        }
        val landmarker = handLandmarker
        if (landmarker == null) {
            imageProxy.close()
            return
        }

        try {
            val frameTime = SystemClock.uptimeMillis()
            val rawBitmap = imageProxy.toBitmap()
            val rotation = imageProxy.imageInfo.rotationDegrees

            val matrix = Matrix().apply {
                if (rotation != 0) postRotate(rotation.toFloat())
                if (isFrontCamera) {
                    postScale(-1f, 1f, rawBitmap.width / 2f, rawBitmap.height / 2f)
                }
            }

            val rotatedBitmap = if (rotation != 0 || isFrontCamera) {
                Bitmap.createBitmap(
                    rawBitmap,
                    0,
                    0,
                    rawBitmap.width,
                    rawBitmap.height,
                    matrix,
                    true
                )
            } else {
                rawBitmap
            }

            if (rotatedBitmap !== rawBitmap) {
                rawBitmap.recycle()
            }

            val mpImage = BitmapImageBuilder(rotatedBitmap).build()
            landmarker.detectAsync(mpImage, frameTime)
        } catch (e: Exception) {
            Log.e(TAG, "Frame inference failed", e)
        } finally {
            imageProxy.close()
        }
    }

    private fun handleLandmarkerResult(
        result: HandLandmarkerResult,
        imageWidth: Int,
        imageHeight: Int
    ) {
        if (isClosed) return
        val inferenceMs = SystemClock.uptimeMillis() - result.timestampMs()
        val trackedHands = result.landmarks().mapIndexed { idx, landmarks ->
            val category = result.handednesses().getOrNull(idx)?.firstOrNull()
            val label = category?.categoryName() ?: "Right"
            val score = category?.score() ?: 0.85f
            TrackedHand(landmarks = landmarks, handedness = label, score = score)
        }
        onResult(
            HandTrackingFrameResult(
                hands = trackedHands,
                inferenceTimeMs = inferenceMs.coerceAtLeast(1L),
                imageWidth = imageWidth,
                imageHeight = imageHeight
            )
        )
    }

    fun close() {
        isClosed = true
        try {
            handLandmarker?.close()
        } catch (_: Exception) {
        } finally {
            handLandmarker = null
        }
    }

    private companion object {
        const val TAG = "MediaPipeHandTracker"
    }
}
