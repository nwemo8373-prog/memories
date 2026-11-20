package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.authentication.AuthScreen
import com.example.authentication.authStateFlow
import com.example.authentication.signOutUser
import com.example.camera.CameraControlScreen
import com.example.customgestures.CustomGesturesScreen
import com.example.dashboard.DashboardScreen
import com.example.dashboard.NegmMainViewModel
import com.example.firebase.ControlProfile
import com.example.firebase.GestureConfig
import com.example.firebase.NegmFirebaseRepository
import com.example.firebase.UiState
import com.example.firebase.UserProfile
import com.example.gesturelibrary.GestureLibraryScreen
import com.example.history.HistoryDatabase
import com.example.history.HistoryLocalRepository
import com.example.history.HistoryScreen
import com.example.profile.ProfileScreen
import com.example.settings.SettingsScreen
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.NegmGestureTheme
import com.example.ui.theme.ObsidianBackdrop
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object NegmRoutes {
    const val DASHBOARD = "dashboard"
    const val LIBRARY = "library"
    const val MY_GESTURES = "my_gestures"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val PROFILE = "profile"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NegmAppRoot()
        }
    }
}

@Composable
fun NegmAppRoot() {
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var guestLanguage by rememberSaveable { mutableStateOf("ar") }

    val user = currentUser
    if (user == null) {
        NegmGestureTheme(language = guestLanguage) {
            AuthScreen(
                onAuthSuccess = {},
                onToggleLanguage = { guestLanguage = it }
            )
        }
    } else {
        key(user.uid) {
            AuthenticatedShell(
                firebaseUser = user,
                fallbackLanguage = guestLanguage
            )
        }
    }
}

@Composable
private fun AuthenticatedShell(
    firebaseUser: FirebaseUser,
    fallbackLanguage: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: NegmMainViewModel = viewModel(
        key = firebaseUser.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val firebaseRepo = NegmFirebaseRepository(db)
                val historyRepo = HistoryLocalRepository(HistoryDatabase.getInstance(app).activityLogDao())
                NegmMainViewModel(
                    firebaseRepository = firebaseRepo,
                    historyRepository = historyRepo,
                    currentUserId = firebaseUser.uid,
                    firebaseUser = firebaseUser
                )
            }
        }
    )

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val gesturesState by viewModel.gesturesState.collectAsStateWithLifecycle()
    val profilesState by viewModel.profilesState.collectAsStateWithLifecycle()
    val historyLogs by viewModel.historyLogs.collectAsStateWithLifecycle()
    val lastConfidence by viewModel.lastConfidence.collectAsStateWithLifecycle()
    val stageIndex by viewModel.stageIndex.collectAsStateWithLifecycle()
    val stagePlaying by viewModel.stagePlaying.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val userProfile: UserProfile? = (profileState as? UiState.Success)?.data
    val gestures: List<GestureConfig> = (gesturesState as? UiState.Success)?.data ?: emptyList()
    val profiles: List<ControlProfile> = (profilesState as? UiState.Success)?.data ?: emptyList()
    val activeLanguage = userProfile?.language ?: fallbackLanguage

    var showCameraControl by rememberSaveable { mutableStateOf(false) }
    var testTargetGestureKey by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingNavigationRoute by rememberSaveable { mutableStateOf<String?>(null) }

    NegmGestureTheme(language = activeLanguage) {
        val strings = LocalAppStrings.current
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(toastMessage) {
            toastMessage?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.clearToast()
            }
        }

        if (showCameraControl) {
            val activeProfile = profiles.firstOrNull { it.id == (userProfile?.activeProfileId ?: "default_profile") }
                ?: profiles.firstOrNull()
            CameraControlScreen(
                userProfile = userProfile,
                gestures = gestures,
                activeProfile = activeProfile,
                testTargetGestureKey = testTargetGestureKey,
                onClose = {
                    showCameraControl = false
                    testTargetGestureKey = null
                },
                onInAppNavigate = { dest ->
                    showCameraControl = false
                    testTargetGestureKey = null
                    pendingNavigationRoute = when (dest) {
                        "history" -> NegmRoutes.HISTORY
                        "settings" -> NegmRoutes.SETTINGS
                        "media_stage" -> NegmRoutes.DASHBOARD
                        else -> NegmRoutes.DASHBOARD
                    }
                },
                onStageCommand = { cmd -> viewModel.handleStageCommand(cmd) },
                onRecordExecution = { gKey, gLabel, aKey, aLabel, ok, detail, conf, sync ->
                    viewModel.recordGestureExecution(gKey, gLabel, aKey, aLabel, ok, detail, conf, sync)
                }
            )
            return@NegmGestureTheme
        }

        val navController = rememberNavController()
        LaunchedEffect(pendingNavigationRoute) {
            pendingNavigationRoute?.let { route ->
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
                pendingNavigationRoute = null
            }
        }
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route ?: NegmRoutes.DASHBOARD

        val navItems = listOf(
            Triple(NegmRoutes.DASHBOARD, strings.navDashboard, Icons.Filled.Dashboard),
            Triple(NegmRoutes.LIBRARY, strings.navLibrary, Icons.AutoMirrored.Filled.LibraryBooks),
            Triple(NegmRoutes.MY_GESTURES, strings.navMyGestures, Icons.Filled.Tune),
            Triple(NegmRoutes.HISTORY, strings.navHistory, Icons.Filled.History),
            Triple(NegmRoutes.SETTINGS, strings.navSettings, Icons.Filled.Settings)
        )

        fun navigateTopTab(route: String) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }

        val handleSignOut: () -> Unit = {
            signOutUser(credentialManager, scope)
        }

        val handleDeleteAccount: () -> Unit = {
            scope.launch {
                viewModel.deleteAccountData()
                try {
                    firebaseUser.delete().await()
                } catch (_: Exception) {
                    signOutUser(credentialManager, scope)
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 700.dp

            ObsidianBackdrop {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (useRail && currentRoute != NegmRoutes.PROFILE) {
                        NavigationRail(
                            containerColor = MidnightSurface.copy(alpha = 0.92f)
                        ) {
                            navItems.forEach { (route, label, icon) ->
                                NavigationRailItem(
                                    selected = currentRoute == route,
                                    onClick = { navigateTopTab(route) },
                                    icon = { Icon(icon, contentDescription = label) },
                                    label = { Text(label) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = ElectricBlue,
                                        selectedTextColor = ElectricBlue,
                                        indicatorColor = ElectricBlue.copy(alpha = 0.16f),
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }

                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets.safeDrawing,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        bottomBar = {
                            if (!useRail && currentRoute != NegmRoutes.PROFILE) {
                                NavigationBar(
                                    containerColor = MidnightSurface.copy(alpha = 0.94f),
                                    windowInsets = WindowInsets.navigationBars
                                ) {
                                    navItems.forEach { (route, label, icon) ->
                                        NavigationBarItem(
                                            selected = currentRoute == route,
                                            onClick = { navigateTopTab(route) },
                                            icon = { Icon(icon, contentDescription = label) },
                                            label = { Text(label, maxLines = 1) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = ElectricBlue,
                                                selectedTextColor = ElectricBlue,
                                                indicatorColor = CyberViolet.copy(alpha = 0.25f),
                                                unselectedIconColor = TextSecondary,
                                                unselectedTextColor = TextSecondary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        if (profileState is UiState.Loading && userProfile == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = ElectricBlue)
                            }
                        } else {
                            NavHost(
                                navController = navController,
                                startDestination = NegmRoutes.DASHBOARD,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                composable(NegmRoutes.DASHBOARD) {
                                    DashboardScreen(
                                        userProfile = userProfile,
                                        historyLogs = historyLogs,
                                        lastConfidence = lastConfidence,
                                        stageItem = viewModel.stageItems[stageIndex],
                                        stagePlaying = stagePlaying,
                                        onStageCommand = { viewModel.handleStageCommand(it) },
                                        onStartGesture = {
                                            testTargetGestureKey = null
                                            showCameraControl = true
                                        },
                                        onOpenLibrary = { navigateTopTab(NegmRoutes.LIBRARY) },
                                        onOpenMyGestures = { navigateTopTab(NegmRoutes.MY_GESTURES) },
                                        onOpenHistory = { navigateTopTab(NegmRoutes.HISTORY) },
                                        onOpenSettings = { navigateTopTab(NegmRoutes.SETTINGS) },
                                        onOpenProfile = { navController.navigate(NegmRoutes.PROFILE) }
                                    )
                                }

                                composable(NegmRoutes.LIBRARY) {
                                    GestureLibraryScreen(
                                        gestures = gestures,
                                        onToggleEnabled = { item, enabled ->
                                            viewModel.saveGesture(item.copy(enabled = enabled))
                                        },
                                        onSaveGesture = { viewModel.saveGesture(it) },
                                        onTestGesture = { gKey ->
                                            testTargetGestureKey = gKey
                                            showCameraControl = true
                                        }
                                    )
                                }

                                composable(NegmRoutes.MY_GESTURES) {
                                    CustomGesturesScreen(
                                        gestures = gestures,
                                        profiles = profiles,
                                        activeProfileId = userProfile?.activeProfileId ?: "default_profile",
                                        onSelectProfile = { pid ->
                                            viewModel.updateSettings(activeProfileId = pid)
                                        },
                                        onCreateProfile = { name, mappings ->
                                            viewModel.createControlProfile(name, mappings)
                                        },
                                        onDeleteProfile = { pid ->
                                            viewModel.deleteControlProfile(pid)
                                        },
                                        onUpdateProfileMapping = { prof, gKey, act ->
                                            viewModel.updateControlProfileMapping(prof, gKey, act)
                                        },
                                        onSaveGesture = { viewModel.saveGesture(it) },
                                        onDeleteCustomGesture = { gid ->
                                            viewModel.deleteCustomGesture(gid)
                                        },
                                        onTestGesture = { gKey ->
                                            testTargetGestureKey = gKey
                                            showCameraControl = true
                                        }
                                    )
                                }

                                composable(NegmRoutes.HISTORY) {
                                    HistoryScreen(
                                        logs = historyLogs,
                                        onClearHistory = { viewModel.clearLocalHistory() }
                                    )
                                }

                                composable(NegmRoutes.SETTINGS) {
                                    SettingsScreen(
                                        userProfile = userProfile,
                                        onUpdateSettings = { l, s, h, snd, vib, bat, cam, hand, pause, sync ->
                                            viewModel.updateSettings(l, s, h, snd, vib, bat, cam, hand, pause, sync)
                                        },
                                        onOpenProfile = { navController.navigate(NegmRoutes.PROFILE) },
                                        onSignOut = handleSignOut,
                                        onDeleteAccount = handleDeleteAccount
                                    )
                                }

                                composable(NegmRoutes.PROFILE) {
                                    ProfileScreen(
                                        firebaseUser = firebaseUser,
                                        userProfile = userProfile,
                                        gestures = gestures,
                                        onUpdateProfileInfo = { name, username, photo ->
                                            viewModel.updateProfileInfo(name, username, photo)
                                        },
                                        onSignOut = handleSignOut,
                                        onDeleteAccount = handleDeleteAccount,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
