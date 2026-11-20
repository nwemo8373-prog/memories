package com.example.gesturelibrary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firebase.GestureConfig
import com.example.gesturerecognition.CoreGestureType
import com.example.gesturerecognition.GestureActionType
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlassCard
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.NeonMint
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun GestureLibraryScreen(
    gestures: List<GestureConfig>,
    onToggleEnabled: (GestureConfig, Boolean) -> Unit,
    onSaveGesture: (GestureConfig) -> Unit,
    onTestGesture: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val lang = LocalAppLanguage.current
    var editingGesture by remember { mutableStateOf<GestureConfig?>(null) }

    // Ensure all 9 core gestures appear even while initial seeding runs
    val displayList = remember(gestures) {
        if (gestures.isNotEmpty()) gestures else {
            CoreGestureType.entries.map {
                GestureConfig(
                    id = it.name,
                    gestureKey = it.name,
                    displayNameAr = it.titleAr,
                    displayNameEn = it.titleEn,
                    descriptionAr = it.descAr,
                    descriptionEn = it.descEn,
                    actionKey = it.defaultAction.name,
                    enabled = true,
                    custom = false,
                    sensitivity = 75L,
                    holdDurationMs = 500L
                )
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = strings.gestureLibraryTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (lang == "ar") {
                    "9 إشارات ذكية مدعومة بالذكاء الاصطناعي المحلي عبر 21 نقطة مفصلية"
                } else {
                    "9 AI-powered hand poses analyzed locally via 21 hand landmarks"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        items(displayList, key = { it.id.ifBlank { it.gestureKey } }) { item ->
            val core = CoreGestureType.fromKey(item.gestureKey)
            val action = GestureActionType.fromKey(item.actionKey)

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                glowColor = if (item.enabled) ElectricBlue else TextSecondary
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(ElectricBlue.copy(alpha = 0.22f), CyberViolet.copy(alpha = 0.22f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = core?.badgeSymbol ?: "✋", fontSize = 26.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (lang == "ar") item.displayNameAr else item.displayNameEn,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = if (lang == "ar") action.labelAr else action.labelEn,
                            style = MaterialTheme.typography.labelLarge,
                            color = NeonMint
                        )
                    }

                    Switch(
                        checked = item.enabled,
                        onCheckedChange = { onToggleEnabled(item, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBlack,
                            checkedTrackColor = ElectricBlue
                        ),
                        modifier = Modifier.testTag("toggle_gesture_${item.gestureKey}")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (lang == "ar") item.descriptionAr else item.descriptionEn,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${strings.sensitivityLabel}: ${item.sensitivity}%  •  ${strings.holdDurationLabel}: ${item.holdDurationMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricBlue
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onTestGesture(item.gestureKey) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricBlue,
                            contentColor = ObsidianBlack
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_gesture_${item.gestureKey}")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.testGesture, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { editingGesture = item },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_gesture_${item.gestureKey}")
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, tint = CyberViolet, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.editGesture, color = TextPrimary)
                    }
                }
            }
        }
    }

    editingGesture?.let { target ->
        var sens by remember(target.id) { mutableFloatStateOf(target.sensitivity.toFloat()) }
        var hold by remember(target.id) { mutableFloatStateOf(target.holdDurationMs.toFloat()) }

        AlertDialog(
            onDismissRequest = { editingGesture = null },
            title = {
                Text(
                    text = if (lang == "ar") target.displayNameAr else target.displayNameEn,
                    color = ElectricBlue
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${strings.sensitivityLabel}: ${sens.toInt()}%", color = TextPrimary)
                    Slider(
                        value = sens,
                        onValueChange = { sens = it },
                        valueRange = 1f..100f,
                        colors = SliderDefaults.colors(thumbColor = ElectricBlue, activeTrackColor = ElectricBlue)
                    )
                    Text("${strings.holdDurationLabel}: ${hold.toInt()} ms", color = TextPrimary)
                    Slider(
                        value = hold,
                        onValueChange = { hold = it },
                        valueRange = 150f..2000f,
                        colors = SliderDefaults.colors(thumbColor = CyberViolet, activeTrackColor = CyberViolet)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSaveGesture(
                            target.copy(
                                sensitivity = sens.toLong().coerceIn(1L, 100L),
                                holdDurationMs = hold.toLong().coerceIn(150L, 2000L)
                            )
                        )
                        editingGesture = null
                    }
                ) {
                    Text(strings.save, color = ElectricBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingGesture = null }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            }
        )
    }
}
