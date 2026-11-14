package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.FitnessRepository
import com.example.data.local.AppDatabase
import com.example.domain.SmsGatewayService
import com.example.model.AppDestination
import com.example.security.AppPreferencesRepository
import com.example.security.FitnessPreferences
import com.example.security.KeystoreCryptoManager
import com.example.security.TokenSecurityEngine
import com.example.ui.components.AmoledGlassBackground
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.DietScreen
import com.example.ui.screens.ExerciseDetailScreen
import com.example.ui.screens.ExercisesScreen
import com.example.ui.screens.GymManagementScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MemberDetailScreen
import com.example.ui.screens.MembersScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TokenGateScreen
import com.example.ui.screens.TokenManagementScreen
import com.example.ui.screens.WorkoutScreen
import com.example.ui.theme.FitnessHubTheme
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.viewmodel.MainViewModel

private enum class PrimaryNavTab(
    val dest: AppDestination,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(AppDestination.Home, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    EXERCISES(AppDestination.Exercises, "Exercises", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter),
    WORKOUT(AppDestination.Workout, "Workout", Icons.Filled.Schedule, Icons.Outlined.Schedule),
    DIET(AppDestination.Diet, "Diet", Icons.Filled.Restaurant, Icons.Outlined.Restaurant),
    GYM(AppDestination.GymManagement, "Gym", Icons.Filled.Business, Icons.Outlined.Business)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val prefsRepo = remember(context) { AppPreferencesRepository(context.applicationContext) }
            val prefs by prefsRepo.preferencesFlow.collectAsStateWithLifecycle(initialValue = FitnessPreferences())

            FitnessHubTheme(mode = prefs.themeMode) {
                FitnessHubRoot(prefsRepo = prefsRepo)
            }
        }
    }
}

@Composable
private fun FitnessHubRoot(prefsRepo: AppPreferencesRepository) {
    val viewModel: MainViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val db = AppDatabase.getInstance(app)
                val crypto = KeystoreCryptoManager(app)
                val repo = FitnessRepository(db, crypto)
                val tokenEngine = TokenSecurityEngine(crypto, db.tokenDao(), prefsRepo)
                val smsService = SmsGatewayService(
                    context = app,
                    crypto = crypto,
                    prefsRepo = prefsRepo,
                    memberDao = db.memberDao(),
                    attendanceDao = db.attendanceDao(),
                    smsLogDao = db.smsLogDao()
                )
                MainViewModel(app, repo, prefsRepo, crypto, tokenEngine, smsService)
            }
        }
    )

    val showSplash by viewModel.showSplash.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val backStack by viewModel.backStack.collectAsStateWithLifecycle()
    val activeDest = backStack.lastOrNull() ?: AppDestination.Home
    val snackbarHostState = remember { SnackbarHostState() }
    val palette = LocalGlassPalette.current

    LaunchedEffect(viewModel) {
        viewModel.toastMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    AmoledGlassBackground {
        when {
            showSplash -> SplashScreen()
            !session.isAuthenticated -> {
                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        TokenGateScreen(
                            session = session,
                            onVerifyToken = viewModel::verifyAccessToken,
                            onInitializeOwner = viewModel::initializeOwnerConsole
                        )
                    }
                }
            }
            else -> {
                val activeTab = when (activeDest) {
                    AppDestination.Home -> PrimaryNavTab.HOME
                    AppDestination.Exercises, is AppDestination.ExerciseDetail -> PrimaryNavTab.EXERCISES
                    AppDestination.Workout -> PrimaryNavTab.WORKOUT
                    AppDestination.Diet -> PrimaryNavTab.DIET
                    else -> PrimaryNavTab.GYM
                }

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useRail = maxWidth >= 600.dp
                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets.safeDrawing,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        bottomBar = {
                            if (!useRail) {
                                NavigationBar(
                                    containerColor = palette.navSurface,
                                    tonalElevation = 0.dp
                                ) {
                                    PrimaryNavTab.entries.forEach { tab ->
                                        val selected = activeTab == tab
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { viewModel.navigateToRoot(tab.dest) },
                                            modifier = Modifier.testTag("nav_${tab.name.lowercase()}"),
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = palette.accentCyan,
                                                selectedTextColor = palette.accentCyan,
                                                indicatorColor = palette.accentCyan.copy(alpha = 0.16f),
                                                unselectedIconColor = palette.secondaryText,
                                                unselectedTextColor = palette.secondaryText
                                            ),
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = tab.label
                                                )
                                            },
                                            label = { Text(tab.label) }
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                            if (useRail) {
                                NavigationRail(containerColor = palette.navSurface) {
                                    PrimaryNavTab.entries.forEach { tab ->
                                        val selected = activeTab == tab
                                        NavigationRailItem(
                                            selected = selected,
                                            onClick = { viewModel.navigateToRoot(tab.dest) },
                                            modifier = Modifier.testTag("rail_${tab.name.lowercase()}"),
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = tab.label
                                                )
                                            },
                                            label = { Text(tab.label) }
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                Crossfade(
                                    targetState = activeDest,
                                    animationSpec = tween(220),
                                    label = "fitness_hub_nav"
                                ) { screen ->
                                    when (screen) {
                                        AppDestination.Home -> HomeScreen(
                                            viewModel = viewModel,
                                            onNavigate = viewModel::navigateTo
                                        )
                                        AppDestination.Exercises -> ExercisesScreen(
                                            onOpenExercise = { id -> viewModel.navigateTo(AppDestination.ExerciseDetail(id)) }
                                        )
                                        is AppDestination.ExerciseDetail -> ExerciseDetailScreen(
                                            exerciseId = screen.exerciseId,
                                            viewModel = viewModel,
                                            onBack = { viewModel.navigateBack() }
                                        )
                                        AppDestination.Workout -> WorkoutScreen(
                                            viewModel = viewModel,
                                            onNavigate = viewModel::navigateTo
                                        )
                                        AppDestination.Diet -> DietScreen(viewModel = viewModel)
                                        AppDestination.Members -> MembersScreen(
                                            viewModel = viewModel,
                                            onOpenMember = { id -> viewModel.navigateTo(AppDestination.MemberDetail(id)) },
                                            onBack = { viewModel.navigateBack() }
                                        )
                                        is AppDestination.MemberDetail -> MemberDetailScreen(
                                            memberId = screen.memberId,
                                            viewModel = viewModel,
                                            onBack = { viewModel.navigateBack() }
                                        )
                                        AppDestination.Attendance -> AttendanceScreen(
                                            viewModel = viewModel,
                                            onBack = { viewModel.navigateBack() }
                                        )
                                        AppDestination.GymManagement -> GymManagementScreen(
                                            viewModel = viewModel,
                                            onBack = { viewModel.navigateBack() }
                                        )
                                        AppDestination.TokenManagement -> TokenManagementScreen(
                                            viewModel = viewModel,
                                            onBack = { viewModel.navigateBack() }
                                        )
                                        AppDestination.Profile -> ProfileScreen(
                                            viewModel = viewModel,
                                            onBack = { viewModel.navigateBack() }
                                        )
                                        AppDestination.Settings -> SettingsScreen(
                                            viewModel = viewModel,
                                            onBack = { viewModel.navigateBack() }
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
}
