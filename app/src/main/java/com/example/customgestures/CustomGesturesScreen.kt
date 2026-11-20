package com.example.customgestures

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.firebase.ControlProfile
import com.example.firebase.GestureConfig
import com.example.gestureactions.DeviceActionExecutor
import com.example.gesturerecognition.CoreGestureType
import com.example.gesturerecognition.GestureActionType
import com.example.ui.theme.CrimsonAlert
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
fun CustomGesturesScreen(
    gestures: List<GestureConfig>,
    profiles: List<ControlProfile>,
    activeProfileId: String,
    onSelectProfile: (String) -> Unit,
    onCreateProfile: (String, Map<String, String>) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onUpdateProfileMapping: (ControlProfile, String, GestureActionType) -> Unit,
    onSaveGesture: (GestureConfig) -> Unit,
    onDeleteCustomGesture: (String) -> Unit,
    onTestGesture: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val lang = LocalAppLanguage.current
    val actionExecutor = remember(context) { DeviceActionExecutor(context) }

    val activeProfile = profiles.firstOrNull { it.id == activeProfileId } ?: profiles.firstOrNull()

    var showNewProfileDialog by remember { mutableStateOf(false) }
    var showCreateGestureDialog by remember { mutableStateOf(false) }
    var actionPickerForGesture by remember { mutableStateOf<GestureConfig?>(null) }
    var appPickerForGesture by remember { mutableStateOf<GestureConfig?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.myGesturesTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.profilesTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Button(
                    onClick = { showCreateGestureDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricBlue,
                        contentColor = ObsidianBlack
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("create_custom_gesture_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.createCustomGesture, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Control Profiles Strip
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), glowColor = CyberViolet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.profilesTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    TextButton(
                        onClick = { showNewProfileDialog = true },
                        modifier = Modifier.testTag("new_profile_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = ElectricBlue)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.newProfile, color = ElectricBlue)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(profiles, key = { it.id }) { prof ->
                        val selected = prof.id == (activeProfile?.id ?: "default_profile")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectProfile(prof.id) },
                                label = { Text(prof.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricBlue.copy(alpha = 0.25f),
                                    selectedLabelColor = ElectricBlue
                                )
                            )
                            if (!prof.defaultProfile && prof.id != "default_profile") {
                                IconButton(onClick = { onDeleteProfile(prof.id) }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = strings.delete,
                                        tint = CrimsonAlert
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Gesture -> Action Mappings
        items(gestures, key = { it.id.ifBlank { it.gestureKey } }) { item ->
            val mappedActionKey = activeProfile?.mappings?.get(item.gestureKey) ?: item.actionKey
            val action = GestureActionType.fromKey(mappedActionKey)
            val core = CoreGestureType.fromKey(item.gestureKey)

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                glowColor = if (item.custom) CyberViolet else ElectricBlue
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${core?.badgeSymbol ?: "✨"} " +
                                if (lang == "ar") item.displayNameAr else item.displayNameEn,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (lang == "ar") action.labelAr else action.labelEn,
                            style = MaterialTheme.typography.labelLarge,
                            color = NeonMint
                        )
                        if (action == GestureActionType.OPEN_APP) {
                            Text(
                                text = item.targetAppName ?: item.targetAppPackage
                                    ?: if (lang == "ar") "لم يتم اختيار تطبيق" else "No app selected",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricBlue
                            )
                        }
                    }

                    if (item.custom) {
                        IconButton(onClick = { onDeleteCustomGesture(item.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = CrimsonAlert)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { actionPickerForGesture = item },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("remap_action_${item.gestureKey}")
                    ) {
                        Icon(Icons.Filled.Tune, contentDescription = null, tint = ElectricBlue)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.selectActionTitle, color = TextPrimary)
                    }

                    if (action == GestureActionType.OPEN_APP) {
                        OutlinedButton(
                            onClick = { appPickerForGesture = item },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("pick_app_${item.gestureKey}")
                        ) {
                            Icon(Icons.Filled.Apps, contentDescription = null, tint = CyberViolet)
                        }
                    }

                    OutlinedButton(
                        onClick = { onTestGesture(item.gestureKey) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = strings.testGesture, tint = NeonMint)
                    }
                }
            }
        }
    }

    // Action Picker Dialog
    actionPickerForGesture?.let { targetGesture ->
        AlertDialog(
            onDismissRequest = { actionPickerForGesture = null },
            title = { Text(strings.selectActionTitle, color = ElectricBlue) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GestureActionType.entries.forEach { act ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (activeProfile != null) {
                                    onUpdateProfileMapping(activeProfile, targetGesture.gestureKey, act)
                                }
                                onSaveGesture(targetGesture.copy(actionKey = act.name))
                                actionPickerForGesture = null
                                if (act == GestureActionType.OPEN_APP) {
                                    appPickerForGesture = targetGesture.copy(actionKey = act.name)
                                }
                            }
                        ) {
                            Text(
                                text = if (lang == "ar") act.labelAr else act.labelEn,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = if (lang == "ar") act.categoryAr else act.categoryEn,
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricBlue
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { actionPickerForGesture = null }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            }
        )
    }

    // Launchable App Picker Dialog
    appPickerForGesture?.let { targetGesture ->
        val apps = remember { actionExecutor.queryLaunchableApps() }
        AlertDialog(
            onDismissRequest = { appPickerForGesture = null },
            title = { Text(strings.selectAppTitle, color = ElectricBlue) },
            text = {
                if (apps.isEmpty()) {
                    Text(
                        text = if (lang == "ar") "لا توجد تطبيقات خارجية متاحة" else "No launchable apps found",
                        color = TextSecondary
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 340.dp)) {
                        items(apps, key = { it.packageName }) { app ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSaveGesture(
                                            targetGesture.copy(
                                                actionKey = GestureActionType.OPEN_APP.name,
                                                targetAppPackage = app.packageName,
                                                targetAppName = app.label
                                            )
                                        )
                                        appPickerForGesture = null
                                    }
                                    .padding(vertical = 10.dp)
                            ) {
                                Text(app.label, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { appPickerForGesture = null }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            }
        )
    }

    // Create Control Profile Dialog
    if (showNewProfileDialog) {
        var profileName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewProfileDialog = false },
            title = { Text(strings.newProfile, color = ElectricBlue) },
            text = {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text(if (lang == "ar") "اسم الملف" else "Profile Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (profileName.isNotBlank()) {
                            onCreateProfile(profileName.trim(), activeProfile?.mappings ?: emptyMap())
                            showNewProfileDialog = false
                        }
                    }
                ) {
                    Text(strings.save, color = ElectricBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewProfileDialog = false }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            }
        )
    }

    // Create Custom Gesture Dialog
    if (showCreateGestureDialog) {
        var customName by remember { mutableStateOf("") }
        var selectedShape by remember { mutableStateOf(CoreGestureType.PEACE_SIGN) }
        var selectedAction by remember { mutableStateOf(GestureActionType.MEDIA_PLAY_PAUSE) }
        var sens by remember { mutableFloatStateOf(75f) }

        AlertDialog(
            onDismissRequest = { showCreateGestureDialog = false },
            title = { Text(strings.createCustomGesture, color = ElectricBlue) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text(if (lang == "ar") "اسم الحركة المخصصة" else "Custom Gesture Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(if (lang == "ar") "شكل اليد المدعوم:" else "Supported Hand Shape:", color = TextSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(CoreGestureType.entries) { shape ->
                            FilterChip(
                                selected = selectedShape == shape,
                                onClick = { selectedShape = shape },
                                label = { Text("${shape.badgeSymbol} ${if (lang == "ar") shape.titleAr else shape.titleEn}") }
                            )
                        }
                    }

                    Text(strings.selectActionTitle, color = TextSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(GestureActionType.entries.filter { it != GestureActionType.NONE }) { act ->
                            FilterChip(
                                selected = selectedAction == act,
                                onClick = { selectedAction = act },
                                label = { Text(if (lang == "ar") act.labelAr else act.labelEn) }
                            )
                        }
                    }

                    Text("${strings.sensitivityLabel}: ${sens.toInt()}%", color = TextPrimary)
                    Slider(
                        value = sens,
                        onValueChange = { sens = it },
                        valueRange = 1f..100f,
                        colors = SliderDefaults.colors(thumbColor = ElectricBlue, activeTrackColor = ElectricBlue)
                    )
                }
            },
            confirmButton = {
                Row {
                    TextButton(
                        onClick = {
                            showCreateGestureDialog = false
                            onTestGesture(selectedShape.name)
                        }
                    ) {
                        Text(strings.testGesture, color = NeonMint)
                    }
                    TextButton(
                        onClick = {
                            val label = customName.trim().ifBlank {
                                if (lang == "ar") selectedShape.titleAr else selectedShape.titleEn
                            }
                            onSaveGesture(
                                GestureConfig(
                                    gestureKey = selectedShape.name,
                                    displayNameAr = label,
                                    displayNameEn = label,
                                    descriptionAr = selectedShape.descAr,
                                    descriptionEn = selectedShape.descEn,
                                    actionKey = selectedAction.name,
                                    enabled = true,
                                    custom = true,
                                    sensitivity = sens.toLong().coerceIn(1L, 100L),
                                    holdDurationMs = 500L
                                )
                            )
                            showCreateGestureDialog = false
                        }
                    ) {
                        Text(strings.save, color = ElectricBlue, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGestureDialog = false }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            }
        )
    }
}
