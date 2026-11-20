package com.example

import com.example.gesturerecognition.CoreGestureType
import com.example.gesturerecognition.GestureClassifier
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class GestureClassifierTest {

    private fun lm(x: Float, y: Float, z: Float = 0f): NormalizedLandmark =
        NormalizedLandmark.create(x, y, z)

    private fun baseHand(
        thumbTip: Pair<Float, Float> = 0.35f to 0.52f,
        thumbIp: Pair<Float, Float> = 0.38f to 0.56f,
        indexTip: Pair<Float, Float> = 0.42f to 0.62f,
        indexPip: Pair<Float, Float> = 0.42f to 0.56f,
        middleTip: Pair<Float, Float> = 0.50f to 0.63f,
        middlePip: Pair<Float, Float> = 0.50f to 0.56f,
        ringTip: Pair<Float, Float> = 0.57f to 0.63f,
        ringPip: Pair<Float, Float> = 0.57f to 0.56f,
        pinkyTip: Pair<Float, Float> = 0.64f to 0.64f,
        pinkyPip: Pair<Float, Float> = 0.64f to 0.58f
    ): List<NormalizedLandmark> {
        return listOf(
            lm(0.50f, 0.80f), // 0 WRIST
            lm(0.42f, 0.72f), // 1 THUMB_CMC
            lm(0.38f, 0.63f), // 2 THUMB_MCP
            lm(thumbIp.first, thumbIp.second), // 3 THUMB_IP
            lm(thumbTip.first, thumbTip.second), // 4 THUMB_TIP
            lm(0.42f, 0.54f), // 5 INDEX_MCP
            lm(indexPip.first, indexPip.second), // 6 INDEX_PIP
            lm(0.42f, 0.58f), // 7 INDEX_DIP
            lm(indexTip.first, indexTip.second), // 8 INDEX_TIP
            lm(0.50f, 0.52f), // 9 MIDDLE_MCP (palmScale = 0.28)
            lm(middlePip.first, middlePip.second), // 10 MIDDLE_PIP
            lm(0.50f, 0.59f), // 11 MIDDLE_DIP
            lm(middleTip.first, middleTip.second), // 12 MIDDLE_TIP
            lm(0.57f, 0.54f), // 13 RING_MCP
            lm(ringPip.first, ringPip.second), // 14 RING_PIP
            lm(0.57f, 0.59f), // 15 RING_DIP
            lm(ringTip.first, ringTip.second), // 16 RING_TIP
            lm(0.64f, 0.57f), // 17 PINKY_MCP
            lm(pinkyPip.first, pinkyPip.second), // 18 PINKY_PIP
            lm(0.64f, 0.60f), // 19 PINKY_DIP
            lm(pinkyTip.first, pinkyTip.second) // 20 PINKY_TIP
        )
    }

    @Test
    fun classifiesOpenPalm() {
        val classifier = GestureClassifier()
        val hand = baseHand(
            thumbTip = 0.24f to 0.48f,
            thumbIp = 0.30f to 0.55f,
            indexTip = 0.42f to 0.22f,
            indexPip = 0.42f to 0.38f,
            middleTip = 0.50f to 0.18f,
            middlePip = 0.50f to 0.36f,
            ringTip = 0.57f to 0.21f,
            ringPip = 0.57f to 0.38f,
            pinkyTip = 0.65f to 0.27f,
            pinkyPip = 0.65f to 0.42f
        )
        val result = classifier.classify(hand, "Right", 75, 1000L)
        assertNotNull(result)
        assertEquals(CoreGestureType.OPEN_PALM, result?.gesture)
    }

    @Test
    fun classifiesThumbsUp() {
        val classifier = GestureClassifier()
        val hand = baseHand(
            thumbTip = 0.26f to 0.28f,
            thumbIp = 0.32f to 0.44f
        )
        val result = classifier.classify(hand, "Right", 75, 1000L)
        assertNotNull(result)
        assertEquals(CoreGestureType.THUMBS_UP, result?.gesture)
    }

    @Test
    fun classifiesPeaceSign() {
        val classifier = GestureClassifier()
        val hand = baseHand(
            indexTip = 0.40f to 0.22f,
            indexPip = 0.41f to 0.38f,
            middleTip = 0.54f to 0.22f,
            middlePip = 0.52f to 0.38f
        )
        val result = classifier.classify(hand, "Right", 75, 1000L)
        assertNotNull(result)
        assertEquals(CoreGestureType.PEACE_SIGN, result?.gesture)
    }

    @Test
    fun classifiesHorns() {
        val classifier = GestureClassifier()
        val hand = baseHand(
            indexTip = 0.40f to 0.22f,
            indexPip = 0.41f to 0.38f,
            pinkyTip = 0.66f to 0.25f,
            pinkyPip = 0.65f to 0.42f
        )
        val result = classifier.classify(hand, "Right", 75, 1000L)
        assertNotNull(result)
        assertEquals(CoreGestureType.HORNS, result?.gesture)
    }
}
