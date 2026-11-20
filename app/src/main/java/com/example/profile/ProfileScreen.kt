package com.example.profile

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.firebase.GestureConfig
import com.example.firebase.UserProfile
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlassCard
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.NeonMint
import com.example.ui.theme.StatusPill
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    firebaseUser: FirebaseUser?,
    userProfile: UserProfile?,
    gestures: List<GestureConfig>,
    onUpdateProfileInfo: (name: String, username: String, photo: String?) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val strings = LocalAppStrings.current
    val lang = LocalAppLanguage.current
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUpdateProfileInfo(
                userProfile?.name ?: firebaseUser?.displayName ?: "Negm User",
                userProfile?.username ?: "negm_user",
                uri.toString()
            )
        }
    }

    val createdDate = remember(userProfile?.createdAt, lang) {
        val date = userProfile?.createdAt?.toDate() ?: Date()
        SimpleDateFormat("yyyy-MM-dd", if (lang == "ar") Locale("ar") else Locale.US).format(date)
    }

    val customCount = remember(gestures) { gestures.count { it.custom } }
    val emailVerified = firebaseUser?.isEmailVerified == true

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("profile_back_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Text(
                    text = strings.profileTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // User Avatar & Identity Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), glowColor = ElectricBlue) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .border(2.dp, Brush.linearGradient(listOf(ElectricBlue, CyberViolet)), CircleShape)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("profile_avatar_picker"),
                        contentAlignment = Alignment.Center
                    ) {
                        val photo = userProfile?.photo.orEmpty()
                        if (photo.isNotBlank()) {
                            AsyncImage(
                                model = photo,
                                contentDescription = strings.profileTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = strings.profileTitle,
                                tint = ElectricBlue,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(ElectricBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = userProfile?.name ?: firebaseUser?.displayName ?: "Negm User",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "@${userProfile?.username ?: "negm_user"}",
                        style = MaterialTheme.typography.titleMedium,
                        color = ElectricBlue
                    )
                    Text(
                        text = userProfile?.email?.ifBlank { firebaseUser?.email ?: "" } ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    StatusPill(
                        label = if (emailVerified) strings.emailVerified else strings.emailNotVerified,
                        active = emailVerified,
                        icon = Icons.Filled.Verified,
                        activeColor = NeonMint,
                        inactiveColor = AmberGlow
                    )
                }
            }
        }

        // Stats Summary Card
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard(modifier = Modifier.weight(1f), glowColor = ElectricBlue) {
                    Text(
                        text = (userProfile?.totalCommandsExecuted ?: 0L).toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = ElectricBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.recognizedCountLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                GlassCard(modifier = Modifier.weight(1f), glowColor = CyberViolet) {
                    Text(
                        text = customCount.toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = CyberViolet,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.customGesturesCountLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${strings.createdAtLabel}: $createdDate",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
            }
        }

        // Actions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_profile_button")
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, tint = ElectricBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.editProfile, color = TextPrimary)
                }

                Button(
                    onClick = onSignOut,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberViolet, contentColor = TextPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_sign_out_button")
                ) {
                    Icon(Icons.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.signOut, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = CrimsonAlert)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.deleteAccount, color = CrimsonAlert)
                }
            }
        }
    }

    if (showEditDialog) {
        var nameInput by remember(userProfile?.name) { mutableStateOf(userProfile?.name ?: "") }
        var usernameInput by remember(userProfile?.username) { mutableStateOf(userProfile?.username ?: "") }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(strings.editProfile, color = ElectricBlue) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text(if (lang == "ar") "الاسم الكامل" else "Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text(if (lang == "ar") "اسم المستخدم" else "Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nameInput.isNotBlank() && usernameInput.trim().length >= 2) {
                            onUpdateProfileInfo(nameInput.trim(), usernameInput.trim(), null)
                            showEditDialog = false
                        }
                    }
                ) {
                    Text(strings.save, color = ElectricBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(strings.deleteAccount, color = CrimsonAlert) },
            text = { Text(strings.deleteAccountConfirm) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteAccount()
                    }
                ) {
                    Text(strings.delete, color = CrimsonAlert, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(strings.cancel, color = TextSecondary)
                }
            }
        )
    }
}
