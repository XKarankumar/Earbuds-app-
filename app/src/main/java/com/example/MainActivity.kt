package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.StudioBackdrop
import com.example.ui.screens.BassEqScreen
import com.example.ui.screens.DjConsoleScreen
import com.example.ui.screens.RgbStudioScreen
import com.example.ui.screens.StudioDashboardScreen
import com.example.ui.screens.SystemHubScreen
import com.example.ui.screens.ThemeEditorScreen
import com.example.ui.theme.HomeTheatreProTheme
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.viewmodel.TheatreViewModel
import kotlinx.serialization.Serializable

@Serializable
sealed interface TheatreRoute {
    @Serializable data object Studio : TheatreRoute
    @Serializable data object BassEq : TheatreRoute
    @Serializable data object DjConsole : TheatreRoute
    @Serializable data object RgbStudio : TheatreRoute
    @Serializable data object SystemHub : TheatreRoute
    @Serializable data object ThemeEditor : TheatreRoute
}

private enum class TheatreNavTab(
    val route: TheatreRoute,
    val routeName: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    STUDIO(TheatreRoute.Studio, "Studio", R.string.nav_studio, Icons.Filled.GraphicEq, Icons.Outlined.GraphicEq),
    BASS_EQ(TheatreRoute.BassEq, "BassEq", R.string.nav_bass_eq, Icons.Filled.Equalizer, Icons.Outlined.Equalizer),
    DJ(TheatreRoute.DjConsole, "DjConsole", R.string.nav_dj, Icons.Filled.MusicNote, Icons.Outlined.MusicNote),
    RGB(TheatreRoute.RgbStudio, "RgbStudio", R.string.nav_rgb, Icons.Filled.Lightbulb, Icons.Outlined.Lightbulb),
    SYSTEM(TheatreRoute.SystemHub, "SystemHub", R.string.nav_system, Icons.Filled.Tune, Icons.Outlined.Tune)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: TheatreViewModel = viewModel()
            val prefs by viewModel.preferences.collectAsStateWithLifecycle()
            val rgbState by viewModel.rgbState.collectAsStateWithLifecycle()

            val liveRgbColor = rgbState.ledStripColors.firstOrNull()?.let { Color(it) }

            HomeTheatreProTheme(
                preset = prefs.themePreset,
                customConfig = prefs.customTheme,
                liveRgbAccent = liveRgbColor
            ) {
                TheatreProRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TheatreProRoot(viewModel: TheatreViewModel) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val engineState by viewModel.masterEngineState.collectAsStateWithLifecycle()
    val djState by viewModel.djState.collectAsStateWithLifecycle()
    val rgbState by viewModel.rgbState.collectAsStateWithLifecycle()
    val report by viewModel.compatibilityReport.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val safeWarningDismissed by viewModel.safeVolumeWarningDismissed.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRouteString = backStackEntry?.destination?.route.orEmpty()
    val snackbarHostState = remember { SnackbarHostState() }
    val palette = LocalStudioPalette.current

    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.statusMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val isThemeEditor = currentRouteString.contains("ThemeEditor")

    StudioBackdrop {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!useRail && !isThemeEditor) {
                        NavigationBar(
                            containerColor = palette.navSurface,
                            tonalElevation = 0.dp
                        ) {
                            TheatreNavTab.entries.forEach { tab ->
                                val selected = currentRouteString.contains(tab.routeName)
                                val label = stringResource(id = tab.labelRes)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(tab.route) {
                                            popUpTo(TheatreRoute.Studio) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    modifier = Modifier.testTag("nav_${tab.name.lowercase()}"),
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = palette.primaryAccent,
                                        selectedTextColor = palette.primaryAccent,
                                        indicatorColor = palette.primaryAccent.copy(alpha = 0.16f),
                                        unselectedIconColor = palette.textSecondary,
                                        unselectedTextColor = palette.textSecondary
                                    ),
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail && !isThemeEditor) {
                        NavigationRail(containerColor = palette.navSurface) {
                            TheatreNavTab.entries.forEach { tab ->
                                val selected = currentRouteString.contains(tab.routeName)
                                val label = stringResource(id = tab.labelRes)
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(tab.route) {
                                            popUpTo(TheatreRoute.Studio) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    modifier = Modifier.testTag("rail_${tab.name.lowercase()}"),
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        NavHost(
                            navController = navController,
                            startDestination = TheatreRoute.Studio
                        ) {
                            composable<TheatreRoute.Studio> {
                                StudioDashboardScreen(
                                    viewModel = viewModel,
                                    prefs = prefs,
                                    engineState = engineState,
                                    telemetry = telemetry,
                                    rgbState = rgbState,
                                    report = report,
                                    safeWarningDismissed = safeWarningDismissed,
                                    onOpenThemeEditor = { navController.navigate(TheatreRoute.ThemeEditor) },
                                    onOpenSystemTab = {
                                        navController.navigate(TheatreRoute.SystemHub) {
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                            composable<TheatreRoute.BassEq> {
                                BassEqScreen(
                                    viewModel = viewModel,
                                    prefs = prefs,
                                    telemetry = telemetry,
                                    report = report
                                )
                            }
                            composable<TheatreRoute.DjConsole> {
                                DjConsoleScreen(
                                    viewModel = viewModel,
                                    djState = djState,
                                    telemetry = telemetry
                                )
                            }
                            composable<TheatreRoute.RgbStudio> {
                                RgbStudioScreen(
                                    viewModel = viewModel,
                                    prefs = prefs,
                                    rgbState = rgbState
                                )
                            }
                            composable<TheatreRoute.SystemHub> {
                                SystemHubScreen(
                                    viewModel = viewModel,
                                    prefs = prefs,
                                    report = report,
                                    onOpenThemeEditor = { navController.navigate(TheatreRoute.ThemeEditor) }
                                )
                            }
                            composable<TheatreRoute.ThemeEditor> {
                                ThemeEditorScreen(
                                    viewModel = viewModel,
                                    prefs = prefs,
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
