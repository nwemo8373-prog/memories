package com.example.gesturerecognition

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.hypot

data class ClassifiedGesture(
    val gesture: CoreGestureType,
    val confidence: Float,
    val handedness: String
)

/**
 * Pure geometric 21-landmark hand gesture classifier.
 * Analyzes real MediaPipe HandLandmarker normalized landmarks (no random or mock output).
 */
class GestureClassifier {

    private data class WristSample(val x: Float, val timestampMs: Long)

    private val wristHistory = ArrayDeque<WristSample>(12)
    private var lastSwipeTimestampMs = 0L

    fun resetMotionHistory() {
        wristHistory.clear()
    }

    /**
     * @param landmarks 21 MediaPipe NormalizedLandmarks for the selected hand
     * @param handedness "Left" or "Right"
     * @param sensitivity 1..100 (higher = slightly looser geometric tolerance)
     * @param timestampMs frame timestamp in ms
     */
    fun classify(
        landmarks: List<NormalizedLandmark>,
        handedness: String,
        sensitivity: Int,
        timestampMs: Long
    ): ClassifiedGesture? {
        if (landmarks.size < 21) return null

        val wrist = landmarks[0]
        val middleMcp = landmarks[9]
        val palmScale = dist(wrist, middleMcp).coerceAtLeast(0.03f)

        // Check dynamic horizontal swipe first
        wristHistory.addLast(WristSample(wrist.x(), timestampMs))
        while (wristHistory.size > 10) {
            wristHistory.removeFirst()
        }
        val swipeResult = detectSwipe(timestampMs, sensitivity, handedness)
        if (swipeResult != null) {
            lastSwipeTimestampMs = timestampMs
            wristHistory.clear()
            return swipeResult
        }

        // Finger extension analysis using tip-to-wrist vs pip-to-wrist ratios normalized by palmScale
        val thumbExtended = isThumbExtended(landmarks, palmScale)
        val indexExtended = isFingerExtended(landmarks, mcpIdx = 5, pipIdx = 6, tipIdx = 8, palmScale = palmScale)
        val middleExtended = isFingerExtended(landmarks, mcpIdx = 9, pipIdx = 10, tipIdx = 12, palmScale = palmScale)
        val ringExtended = isFingerExtended(landmarks, mcpIdx = 13, pipIdx = 14, tipIdx = 16, palmScale = palmScale)
        val pinkyExtended = isFingerExtended(landmarks, mcpIdx = 17, pipIdx = 18, tipIdx = 20, palmScale = palmScale)

        val thumbTip = landmarks[4]
        val thumbMcp = landmarks[2]
        val indexTip = landmarks[8]
        val indexMcp = landmarks[5]

        // Sensitivity tolerance factor: 100 -> 0.80 (easier), 50 -> 1.0, 1 -> 1.25 (stricter)
        val tol = 1.15f - (sensitivity.coerceIn(1, 100) / 100f) * 0.35f

        // 1. OPEN PALM: all 4 main fingers + thumb (or 4 main fingers clearly extended)
        if (indexExtended && middleExtended && ringExtended && pinkyExtended) {
            val spreadBonus = if (thumbExtended) 0.96f else 0.89f
            return ClassifiedGesture(CoreGestureType.OPEN_PALM, spreadBonus, handedness)
        }

        // 2. FIST: all 4 fingers curled and thumb tucked
        if (!indexExtended && !middleExtended && !ringExtended && !pinkyExtended && !thumbExtended) {
            return ClassifiedGesture(CoreGestureType.FIST, 0.93f, handedness)
        }

        // 3. THUMBS UP: thumb clearly extended upward, all other 4 fingers curled
        val thumbVerticalDelta = wrist.y() - thumbTip.y() // positive when thumb is above wrist
        val thumbAboveMcp = thumbMcp.y() - thumbTip.y()
        if (thumbExtended && !indexExtended && !middleExtended && !ringExtended && !pinkyExtended &&
            thumbVerticalDelta > palmScale * 0.45f * tol &&
            thumbAboveMcp > palmScale * 0.2f * tol
        ) {
            val conf = (0.85f + (thumbVerticalDelta / (palmScale * 2f)).coerceIn(0f, 0.13f))
            return ClassifiedGesture(CoreGestureType.THUMBS_UP, conf, handedness)
        }

        // 4. HORNS: index and pinky extended, middle and ring curled
        if (indexExtended && !middleExtended && !ringExtended && pinkyExtended) {
            return ClassifiedGesture(CoreGestureType.HORNS, 0.92f, handedness)
        }

        // 5. PEACE SIGN: index and middle extended, ring and pinky curled
        if (indexExtended && middleExtended && !ringExtended && !pinkyExtended) {
            val fingerGap = dist(landmarks[8], landmarks[12])
            val conf = if (fingerGap > palmScale * 0.25f) 0.95f else 0.88f
            return ClassifiedGesture(CoreGestureType.PEACE_SIGN, conf, handedness)
        }

        // 6. ONE FINGER vs DOWN GESTURE: only index extended
        if (indexExtended && !middleExtended && !ringExtended && !pinkyExtended) {
            val dy = indexTip.y() - indexMcp.y()
            val dx = abs(indexTip.x() - indexMcp.x())
            if (dy < -palmScale * 0.35f * tol && abs(dy) >= dx * 0.65f) {
                return ClassifiedGesture(CoreGestureType.ONE_FINGER, 0.93f, handedness)
            }
            if (dy > palmScale * 0.35f * tol && abs(dy) >= dx * 0.65f) {
                return ClassifiedGesture(CoreGestureType.DOWN_GESTURE, 0.92f, handedness)
            }
        }

        return null
    }

    private fun detectSwipe(
        nowMs: Long,
        sensitivity: Int,
        handedness: String
    ): ClassifiedGesture? {
        if (nowMs - lastSwipeTimestampMs < 900L) return null
        if (wristHistory.size < 4) return null

        val recent = wristHistory.filter { nowMs - it.timestampMs <= 420L }
        if (recent.size < 4) return null

        val first = recent.first()
        val last = recent.last()
        val dt = (last.timestampMs - first.timestampMs).coerceAtLeast(1L)
        val dx = last.x - first.x

        // Threshold adjusted by sensitivity (higher sensitivity = smaller required swipe travel)
        val minTravel = 0.26f - (sensitivity.coerceIn(1, 100) / 100f) * 0.12f
        if (abs(dx) >= minTravel && dt <= 420L) {
            val conf = (0.84f + (abs(dx) - minTravel).coerceIn(0f, 0.14f))
            return if (dx > 0f) {
                ClassifiedGesture(CoreGestureType.SWIPE_RIGHT, conf, handedness)
            } else {
                ClassifiedGesture(CoreGestureType.SWIPE_LEFT, conf, handedness)
            }
        }
        return null
    }

    private fun isThumbExtended(landmarks: List<NormalizedLandmark>, palmScale: Float): Boolean {
        val thumbTip = landmarks[4]
        val thumbIp = landmarks[3]
        val indexMcp = landmarks[5]
        val pinkyMcp = landmarks[17]
        val tipToIndexMcp = dist(thumbTip, indexMcp)
        val tipToPinkyMcp = dist(thumbTip, pinkyMcp)
        val ipToPinkyMcp = dist(thumbIp, pinkyMcp)
        return tipToIndexMcp > palmScale * 0.55f && tipToPinkyMcp > ipToPinkyMcp * 1.06f
    }

    private fun isFingerExtended(
        landmarks: List<NormalizedLandmark>,
        mcpIdx: Int,
        pipIdx: Int,
        tipIdx: Int,
        palmScale: Float
    ): Boolean {
        val wrist = landmarks[0]
        val mcp = landmarks[mcpIdx]
        val pip = landmarks[pipIdx]
        val tip = landmarks[tipIdx]

        val wristToTip = dist(wrist, tip)
        val wristToPip = dist(wrist, pip)
        val mcpToTip = dist(mcp, tip)
        return wristToTip > wristToPip * 1.12f && mcpToTip > palmScale * 0.62f
    }

    private fun dist(a: NormalizedLandmark, b: NormalizedLandmark): Float {
        return hypot(a.x() - b.x(), a.y() - b.y())
    }
}
