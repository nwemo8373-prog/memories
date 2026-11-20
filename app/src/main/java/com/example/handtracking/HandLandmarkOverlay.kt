package com.example.handtracking

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonMint
import kotlin.math.max
import kotlin.math.min

private val HAND_CONNECTIONS = listOf(
    0 to 1, 1 to 2, 2 to 3, 3 to 4,
    0 to 5, 5 to 6, 6 to 7, 7 to 8,
    5 to 9, 9 to 10, 10 to 11, 11 to 12,
    9 to 13, 13 to 14, 14 to 15, 15 to 16,
    13 to 17, 0 to 17, 17 to 18, 18 to 19, 19 to 20
)

private val FINGERTIP_INDICES = setOf(4, 8, 12, 16, 20)

@Composable
fun HandLandmarkOverlay(
    hands: List<TrackedHand>,
    holdProgress: Float,
    gestureLocked: Boolean,
    showLandmarks: Boolean,
    modifier: Modifier = Modifier
) {
    val glowStrength by animateFloatAsState(
        targetValue = if (hands.isNotEmpty()) (if (gestureLocked) 1f else 0.65f) else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "hand_aura"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        if (hands.isEmpty()) return@Canvas

        hands.forEach { hand ->
            val landmarks = hand.landmarks
            if (landmarks.size < 21) return@forEach

            var minX = 1f
            var minY = 1f
            var maxX = 0f
            var maxY = 0f
            val points = landmarks.map { lm ->
                minX = min(minX, lm.x())
                minY = min(minY, lm.y())
                maxX = max(maxX, lm.x())
                maxY = max(maxY, lm.y())
                Offset(lm.x() * size.width, lm.y() * size.height)
            }

            // Neon bounding aura box around detected hand
            val pad = 28f
            val left = (minX * size.width - pad).coerceAtLeast(8f)
            val top = (minY * size.height - pad).coerceAtLeast(8f)
            val right = (maxX * size.width + pad).coerceAtMost(size.width - 8f)
            val bottom = (maxY * size.height + pad).coerceAtMost(size.height - 8f)
            val boxW = (right - left).coerceAtLeast(24f)
            val boxH = (bottom - top).coerceAtLeast(24f)

            val auraColor = if (gestureLocked) NeonMint else ElectricBlue
            drawRoundRect(
                color = auraColor.copy(alpha = 0.14f * glowStrength),
                topLeft = Offset(left, top),
                size = Size(boxW, boxH),
                cornerRadius = CornerRadius(28f, 28f)
            )
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(
                        auraColor.copy(alpha = 0.9f),
                        CyberViolet.copy(alpha = 0.85f)
                    )
                ),
                topLeft = Offset(left, top),
                size = Size(boxW, boxH),
                cornerRadius = CornerRadius(28f, 28f),
                style = Stroke(width = if (gestureLocked) 5f else 3f)
            )

            // Hold lock progress ring around wrist/palm center
            if (holdProgress > 0.02f) {
                val wrist = points[0]
                drawArc(
                    color = if (gestureLocked) NeonMint else ElectricBlue,
                    startAngle = -90f,
                    sweepAngle = 360f * holdProgress.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = Offset(wrist.x - 36f, wrist.y - 36f),
                    size = Size(72f, 72f),
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )
            }

            if (showLandmarks) {
                // Draw bone connections
                HAND_CONNECTIONS.forEach { (startIdx, endIdx) ->
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(ElectricBlue, CyberViolet),
                            start = points[startIdx],
                            end = points[endIdx]
                        ),
                        start = points[startIdx],
                        end = points[endIdx],
                        strokeWidth = 5f,
                        cap = StrokeCap.Round
                    )
                }

                // Draw 21 landmark nodes
                points.forEachIndexed { idx, pt ->
                    val isTip = idx in FINGERTIP_INDICES
                    val nodeColor = when {
                        gestureLocked && isTip -> NeonMint
                        isTip -> ElectricBlue
                        else -> CyberViolet
                    }
                    if (isTip) {
                        drawCircle(
                            color = nodeColor.copy(alpha = 0.35f),
                            radius = 18f,
                            center = pt
                        )
                    }
                    drawCircle(
                        color = nodeColor,
                        radius = if (isTip) 9f else 6f,
                        center = pt
                    )
                    drawCircle(
                        color = Color.White,
                        radius = if (isTip) 3.5f else 2f,
                        center = pt
                    )
                }
            }
        }
    }
}
