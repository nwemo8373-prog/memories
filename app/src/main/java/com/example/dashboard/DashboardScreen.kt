package com.example.dashboard

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.firebase.UserProfile
import com.example.history.ActivityLogEntity
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlassCard
import com.example.ui.theme.GradientActionButton
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.NeonMint
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.StatusPill
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DashboardScreen(
    userProfile: UserProfile?,
    historyLogs: List<ActivityLogEntity>,
    lastConfidence: Int,
    stageItem: StageMediaItem,
    stagePlaying: Boolean,
    onStageCommand: (String) -> Unit,
    onStartGesture: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenMyGestures: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val lang = LocalAppLanguage.current
    val cameraGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    val totalRecognized by animateIntAsState(
        targetValue = (userProfile?.totalCommandsExecuted?.toInt() ?: historyLogs.size),
        animationSpec = tween(durationMillis = 500),
        label = "recognized_counter"
    )
    val displayedConfidence by animateIntAsState(
        targetValue = if (lastConfidence > 0) lastConfidence else 94,
        animationSpec = tween(durationMillis = 500),
        label = "confidence_anim"
    )
    val lastCommand = historyLogs.firstOrNull()?.actionLabel ?: strings.noCommandYet
    var showGuideDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header: NEGM GESTURE + Greeting + User Avatar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.appName,
                        style = MaterialTheme.typography.titleLarge,
                        color = ElectricBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${strings.welcomePrefix} ${userProfile?.name ?: "User"}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = strings.tagline,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { showGuideDialog = true },
                        modifier = Modifier.testTag("dashboard_app_guide_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = strings.appGuideTitle,
                            tint = ElectricBlue
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, Brush.linearGradient(listOf(ElectricBlue, CyberViolet)), CircleShape)
                            .clickable(onClick = onOpenProfile)
                            .testTag("dashboard_profile_avatar"),
                        contentAlignment = Alignment.Center
                    ) {
                        val photoUrl = userProfile?.photo.orEmpty()
                        if (photoUrl.isNotBlank()) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = strings.profileTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = strings.profileTitle,
                                tint = ElectricBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }

        // System Status Pills
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatusPill(
                    label = if (cameraGranted) strings.cameraReady else strings.cameraPermissionNeeded,
                    active = cameraGranted,
                    icon = Icons.Filled.CameraAlt
                )
                StatusPill(
                    label = strings.aiEngineReady,
                    active = true,
                    icon = Icons.Filled.Bolt,
                    activeColor = ElectricBlue
                )
            }
        }

        // Hero START GESTURE Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                glowColor = ElectricBlue,
                borderAlpha = 0.55f,
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_gesture_guide_1795131747171),
                        contentDescription = strings.startGesture,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, ObsidianBlack.copy(alpha = 0.92f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = strings.aiEngineLocalBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonMint
                        )
                        Text(
                            text = strings.cameraControlTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    }
                }
                Box(modifier = Modifier.padding(16.dp)) {
                    GradientActionButton(
                        text = strings.startGesture,
                        icon = Icons.Filled.Videocam,
                        onClick = onStartGesture,
                        modifier = Modifier.testTag("start_gesture_button")
                    )
                }
            }
        }

        // Animated Stat Cards Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard(
                    modifier = Modifier.weight(1f),
                    glowColor = ElectricBlue
                ) {
                    Text(
                        text = totalRecognized.toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = ElectricBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.recognizedCountLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                GlassCard(
                    modifier = Modifier.weight(1f),
                    glowColor = CyberViolet
                ) {
                    Text(
                        text = "$displayedConfidence%",
                        style = MaterialTheme.typography.headlineLarge,
                        color = CyberViolet,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (displayedConfidence / 100f).coerceIn(0f, 1f) },
                        color = CyberViolet,
                        trackColor = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.accuracyLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        // Last Executed Command Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                glowColor = NeonMint
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.lastCommandLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = lastCommand,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(onClick = onOpenHistory) {
                        Text(strings.navHistory, color = ElectricBlue)
                    }
                }
            }
        }

        // In-App Touchless Media Stage (Interactive Preview / Controlled by Hand Gestures)
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                glowColor = CyberViolet
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.touchlessStageTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = ElectricBlue
                        )
                        Text(
                            text = if (lang == "ar") stageItem.titleAr else stageItem.titleEn,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = if (lang == "ar") stageItem.subtitleAr else stageItem.subtitleEn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    StatusPill(
                        label = stageItem.bpmOrTag,
                        active = stagePlaying,
                        icon = if (stagePlaying) Icons.Filled.PlayArrow else Icons.Filled.Pause
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onStageCommand("PREV") }) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = TextPrimary)
                    }
                    IconButton(
                        onClick = { onStageCommand("PLAY_PAUSE") },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(ElectricBlue, CyberViolet)))
                    ) {
                        Icon(
                            imageVector = if (stagePlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play Pause",
                            tint = ObsidianBlack
                        )
                    }
                    IconButton(onClick = { onStageCommand("NEXT") }) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = TextPrimary)
                    }
                }
            }
        }

        // Quick Navigation Grid
        item {
            Text(
                text = strings.quickActionsTitle,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickNavTile(
                    title = strings.gestureLibraryTitle,
                    icon = Icons.AutoMirrored.Filled.LibraryBooks,
                    accent = ElectricBlue,
                    onClick = onOpenLibrary,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_library_btn")
                )
                QuickNavTile(
                    title = strings.myGesturesTitle,
                    icon = Icons.Filled.Tune,
                    accent = CyberViolet,
                    onClick = onOpenMyGestures,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_my_gestures_btn")
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickNavTile(
                    title = strings.historyTitle,
                    icon = Icons.Filled.History,
                    accent = NeonMint,
                    onClick = onOpenHistory,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_history_btn")
                )
                QuickNavTile(
                    title = strings.settingsTitle,
                    icon = Icons.Filled.Settings,
                    accent = ElectricBlue,
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_settings_btn")
                )
            }
        }
    }

    if (showGuideDialog) {
        AppGuideDialog(onDismiss = { showGuideDialog = false })
    }
}

@Composable
private fun QuickNavTile(
    title: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        glowColor = accent,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accent)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
