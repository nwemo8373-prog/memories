package com.example.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dashboard.AppGuideDialog
import com.example.firebase.UserProfile
import com.example.gestureactions.NegmAccessibilityService
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlassCard
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.NeonMint
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.StatusPill
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    userProfile: UserProfile?,
    onUpdateSettings: (
        language: String?,
        sensitivity: Int?,
        holdDurationMs: Int?,
        soundEnabled: Boolean?,
        vibrationEnabled: Boolean?,
        batterySaver: Boolean?,
        preferredCamera: String?,
        handPreference: String?,
        pauseOnLeave: Boolean?,
        syncHistoryEnabled: Boolean?
    ) -> Unit,
    onOpenProfile: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val lang = LocalAppLanguage.current

    var sensitivitySlider by remember(userProfile?.sensitivity) {
        mutableFloatStateOf((userProfile?.sensitivity ?: 75L).toFloat())
    }
    var holdSlider by remember(userProfile?.holdDurationMs) {
        mutableFloatStateOf((userProfile?.holdDurationMs ?: 500L).toFloat())
    }
    var showPolicyDialog by remember { mutableStateOf(false) }
    var showCloudInfoDialog by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val accessibilityActive = remember(context) {
        NegmAccessibilityService.isServiceEnabled(context)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = strings.settingsTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        // Recognition Sensitivity & Hold Duration
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), glowColor = ElectricBlue) {
                Text(
                    text = "${strings.sensitivityLabel}: ${sensitivitySlider.toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Slider(
                    value = sensitivitySlider,
                    onValueChange = { sensitivitySlider = it },
                    onValueChangeFinished = {
                        onUpdateSettings(null, sensitivitySlider.toInt(), null, null, null, null, null, null, null, null)
                    },
                    valueRange = 1f..100f,
                    colors = SliderDefaults.colors(thumbColor = ElectricBlue, activeTrackColor = ElectricBlue),
                    modifier = Modifier.testTag("settings_sensitivity_slider")
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${strings.holdDurationLabel}: ${holdSlider.toInt()} ms",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Slider(
                    value = holdSlider,
                    onValueChange = { holdSlider = it },
                    onValueChangeFinished = {
                        onUpdateSettings(null, null, holdSlider.toInt(), null, null, null, null, null, null, null)
                    },
                    valueRange = 150f..2000f,
                    colors = SliderDefaults.colors(thumbColor = CyberViolet, activeTrackColor = CyberViolet),
                    modifier = Modifier.testTag("settings_hold_slider")
                )
            }
        }

        // Camera, Hand & Language Preferences
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), glowColor = CyberViolet) {
                Text(strings.languageLabel, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = (userProfile?.language ?: lang) == "ar",
                        onClick = { onUpdateSettings("ar", null, null, null, null, null, null, null, null, null) },
                        label = { Text("العربية (RTL)") },
                        modifier = Modifier.testTag("lang_ar_chip")
                    )
                    FilterChip(
                        selected = (userProfile?.language ?: lang) == "en",
                        onClick = { onUpdateSettings("en", null, null, null, null, null, null, null, null, null) },
                        label = { Text("English (LTR)") },
                        modifier = Modifier.testTag("lang_en_chip")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(strings.switchCamera, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = (userProfile?.preferredCamera ?: "front") == "front",
                        onClick = { onUpdateSettings(null, null, null, null, null, null, "front", null, null, null) },
                        label = { Text(strings.frontCamera) }
                    )
                    FilterChip(
                        selected = (userProfile?.preferredCamera ?: "front") == "back",
                        onClick = { onUpdateSettings(null, null, null, null, null, null, "back", null, null, null) },
                        label = { Text(strings.backCamera) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(strings.handPreferenceLabel, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "both" to strings.bothHands,
                        "right" to strings.rightHand,
                        "left" to strings.leftHand
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = (userProfile?.handPreference ?: "both") == key,
                            onClick = { onUpdateSettings(null, null, null, null, null, null, null, key, null, null) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        // Toggle Switches
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), glowColor = ElectricBlue) {
                SettingToggleRow(
                    title = strings.vibrationLabel,
                    checked = userProfile?.vibrationEnabled ?: true,
                    onCheckedChange = {
                        onUpdateSettings(null, null, null, null, it, null, null, null, null, null)
                    }
                )
                SettingToggleRow(
                    title = strings.soundLabel,
                    checked = userProfile?.soundEnabled ?: true,
                    onCheckedChange = {
                        onUpdateSettings(null, null, null, it, null, null, null, null, null, null)
                    }
                )
                SettingToggleRow(
                    title = strings.batterySaverLabel,
                    checked = userProfile?.batterySaver ?: false,
                    onCheckedChange = {
                        onUpdateSettings(null, null, null, null, null, it, null, null, null, null)
                    }
                )
                SettingToggleRow(
                    title = strings.pauseOnLeaveLabel,
                    checked = userProfile?.pauseOnLeave ?: true,
                    onCheckedChange = {
                        onUpdateSettings(null, null, null, null, null, null, null, null, it, null)
                    }
                )
                SettingToggleRow(
                    title = strings.syncHistoryLabel,
                    checked = userProfile?.syncHistoryEnabled ?: false,
                    onCheckedChange = {
                        onUpdateSettings(null, null, null, null, null, null, null, null, null, it)
                    }
                )
            }
        }

        // Accessibility Service (Optional & Play-Compliant)
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), glowColor = NeonMint) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.accessibilityTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatusPill(
                        label = if (accessibilityActive) strings.accessibilityEnabled else strings.accessibilityDisabled,
                        active = accessibilityActive,
                        icon = Icons.Filled.AccessibilityNew,
                        activeColor = NeonMint,
                        inactiveColor = AmberGlow
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(strings.accessibilityDesc, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.enableAccessibility, color = ElectricBlue)
                }
            }
        }

        // Account, Notifications, Policies
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), glowColor = CyberViolet) {
                Text(strings.accountSection, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onOpenProfile,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = ElectricBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.profileTitle, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showCloudInfoDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Notifications, contentDescription = null, tint = CyberViolet)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.fcmNoticeTitle, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showGuideDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = ElectricBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.appGuideTitle, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showPolicyDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Policy, contentDescription = null, tint = NeonMint)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${strings.privacyPolicyTitle} & ${strings.termsOfUseTitle}", color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onSignOut,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberViolet, contentColor = TextPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_sign_out_button")
                ) {
                    Icon(Icons.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.signOut, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_delete_account_button")
                ) {
                    Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = CrimsonAlert)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.deleteAccount, color = CrimsonAlert)
                }
            }
        }
    }

    if (showPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPolicyDialog = false },
            title = { Text(strings.privacyPolicyTitle, color = ElectricBlue) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.privacyPolicyBody, style = MaterialTheme.typography.bodyMedium)
                    Text(strings.termsOfUseTitle, color = CyberViolet, style = MaterialTheme.typography.titleMedium)
                    Text(strings.termsOfUseBody, style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { showPolicyDialog = false }) {
                    Text(strings.close, color = ElectricBlue)
                }
            }
        )
    }

    if (showCloudInfoDialog) {
        AlertDialog(
            onDismissRequest = { showCloudInfoDialog = false },
            title = { Text(strings.fcmNoticeTitle, color = ElectricBlue) },
            text = { Text(strings.fcmNoticeBody, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { showCloudInfoDialog = false }) {
                    Text(strings.close, color = ElectricBlue)
                }
            }
        )
    }

    if (showGuideDialog) {
        AppGuideDialog(onDismiss = { showGuideDialog = false })
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(strings.deleteAccount, color = CrimsonAlert) },
            text = { Text(strings.deleteAccountConfirm, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount()
                    }
                ) {
                    Text(strings.delete, color = CrimsonAlert, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianBlack,
                checkedTrackColor = ElectricBlue
            )
        )
    }
}
