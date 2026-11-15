package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.smartcool.ui.components.LiquidGlassBackdrop
import com.example.smartcool.ui.screens.BatteryScreen
import com.example.smartcool.ui.screens.CoolingScreen
import com.example.smartcool.ui.screens.ModesAndReportsScreen
import com.example.smartcool.ui.screens.OverviewScreen
import com.example.smartcool.ui.screens.PerformanceScreen
import com.example.smartcool.ui.screens.SettingsAndThemesScreen
import com.example.smartcool.ui.theme.LocalLiquidPalette
import com.example.smartcool.ui.theme.SmartCoolProTheme
import com.example.smartcool.ui.viewmodel.SmartCoolViewModel
import kotlinx.serialization.Serializable

@Serializable
sealed interface SmartCoolRoute {
    @Serializable data object Overview : SmartCoolRoute
    @Serializable data object Cooling : SmartCoolRoute
    @Serializable data object Battery : SmartCoolRoute
    @Serializable data object Performance : SmartCoolRoute
    @Serializable data object Modes : SmartCoolRoute
    @Serializable data object Settings : SmartCoolRoute
}

private enum class SmartCoolTab(
    val route: SmartCoolRoute,
    val routeKey: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val showOnCompactBar: Boolean
) {
    OVERVIEW(SmartCoolRoute.Overview, "Overview", R.string.nav_overview, Icons.Filled.Dashboard, Icons.Outlined.Dashboard, true),
    COOLING(SmartCoolRoute.Cooling, "Cooling", R.string.nav_cooling, Icons.Filled.AcUnit, Icons.Outlined.AcUnit, true),
    BATTERY(SmartCoolRoute.Battery, "Battery", R.string.nav_battery, Icons.Filled.BatteryChargingFull, Icons.Outlined.BatteryChargingFull, true),
    PERFORMANCE(SmartCoolRoute.Performance, "Performance", R.string.nav_performance, Icons.Filled.Memory, Icons.Outlined.Memory, true),
    MODES(SmartCoolRoute.Modes, "Modes", R.string.nav_modes, Icons.Filled.Assessment, Icons.Outlined.Assessment, false),
    SETTINGS(SmartCoolRoute.Settings, "Settings", R.string.nav_settings, Icons.Filled.Tune, Icons.Outlined.Tune, true)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: SmartCoolViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            SmartCoolProTheme(
                preset = settings.selectedTheme,
                customConfig = settings.customTheme
            ) {
                SmartCoolAppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SmartCoolAppRoot(viewModel: SmartCoolViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val battery by viewModel.batteryTelemetry.collectAsStateWithLifecycle()
    val thermal by viewModel.thermalTelemetry.collectAsStateWithLifecycle()
    val memory by viewModel.memorySnapshot.collectAsStateWithLifecycle()
    val usage by viewModel.usageInsights.collectAsStateWithLifecycle()
    val history by viewModel.historySamples.collectAsStateWithLifecycle()
    val report by viewModel.dailyReport.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRouteString = backStackEntry?.destination?.route.orEmpty()
    val snackbarHostState = remember { SnackbarHostState() }
    val palette = LocalLiquidPalette.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.startMonitoring()
                Lifecycle.Event.ON_PAUSE -> viewModel.pauseMonitoring()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.toastEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    fun navigateTopTab(route: SmartCoolRoute) {
        navController.navigate(route) {
            popUpTo(SmartCoolRoute.Overview) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    LiquidGlassBackdrop {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!useRail) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .background(palette.navGlassSurface)
                                .border(1.dp, palette.specularTopBorder, RoundedCornerShape(26.dp))
                        ) {
                            NavigationBar(
                                containerColor = Color.Transparent,
                                tonalElevation = 0.dp
                            ) {
                                SmartCoolTab.entries.filter { it.showOnCompactBar }.forEach { tab ->
                                    val selected = currentRouteString.contains(tab.routeKey)
                                    val label = stringResource(id = tab.labelRes)
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateTopTab(tab.route) },
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
                                        label = { Text(label, maxLines = 1) }
                                    )
                                }
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
                    if (useRail) {
                        NavigationRail(
                            containerColor = palette.navGlassSurface,
                            modifier = Modifier.border(1.dp, palette.borderStroke)
                        ) {
                            Spacer(modifier = Modifier.height(12.dp))
                            SmartCoolTab.entries.forEach { tab ->
                                val selected = currentRouteString.contains(tab.routeKey)
                                val label = stringResource(id = tab.labelRes)
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTopTab(tab.route) },
                                    modifier = Modifier.testTag("rail_${tab.name.lowercase()}"),
                                    colors = NavigationRailItemDefaults.colors(
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

                    Box(modifier = Modifier.weight(1f)) {
                        NavHost(
                            navController = navController,
                            startDestination = SmartCoolRoute.Overview
                        ) {
                            composable<SmartCoolRoute.Overview> {
                                OverviewScreen(
                                    settings = settings,
                                    battery = battery,
                                    thermal = thermal,
                                    memory = memory,
                                    history = history,
                                    onSelectMode = { mode ->
                                        viewModel.selectSmartMode(mode)
                                        if (mode == com.example.smartcool.model.SmartMode.EXTREME_BATTERY_SAVER) {
                                            navigateTopTab(SmartCoolRoute.Modes)
                                        }
                                    },
                                    onRefresh = { viewModel.refreshAllTelemetry(recordSample = true) },
                                    onNavigateCooling = { navigateTopTab(SmartCoolRoute.Cooling) },
                                    onNavigateBattery = { navigateTopTab(SmartCoolRoute.Battery) },
                                    onNavigatePerformance = { navigateTopTab(SmartCoolRoute.Performance) },
                                    onNavigateModes = {
                                        navController.navigate(SmartCoolRoute.Modes) {
                                            launchSingleTop = true
                                        }
                                    },
                                    onNavigateThemes = { navigateTopTab(SmartCoolRoute.Settings) }
                                )
                            }
                            composable<SmartCoolRoute.Cooling> {
                                CoolingScreen(
                                    settings = settings,
                                    battery = battery,
                                    thermal = thermal,
                                    history = history,
                                    onToggleCoolingMode = {
                                        val next = if (settings.smartMode == com.example.smartcool.model.SmartMode.COOLING) {
                                            com.example.smartcool.model.SmartMode.NORMAL
                                        } else {
                                            com.example.smartcool.model.SmartMode.COOLING
                                        }
                                        viewModel.selectSmartMode(next)
                                    },
                                    onClearHistory = { viewModel.clearRecordedHistory() }
                                )
                            }
                            composable<SmartCoolRoute.Battery> {
                                BatteryScreen(
                                    settings = settings,
                                    battery = battery,
                                    history = history
                                )
                            }
                            composable<SmartCoolRoute.Performance> {
                                PerformanceScreen(
                                    memory = memory,
                                    thermal = thermal,
                                    usage = usage,
                                    onRefresh = { viewModel.refreshAllTelemetry(recordSample = false) }
                                )
                            }
                            composable<SmartCoolRoute.Modes> {
                                BackHandler { navController.popBackStack() }
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { navController.popBackStack() },
                                            modifier = Modifier.testTag("modes_back_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = stringResource(id = R.string.cd_back),
                                                tint = palette.textPrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Smart Modes & Reports",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = palette.textPrimary
                                        )
                                    }
                                    ModesAndReportsScreen(
                                        settings = settings,
                                        battery = battery,
                                        report = report,
                                        onSelectMode = viewModel::selectSmartMode,
                                        onSelectTheme = viewModel::selectTheme
                                    )
                                }
                            }
                            composable<SmartCoolRoute.Settings> {
                                SettingsAndThemesScreen(
                                    settings = settings,
                                    battery = battery,
                                    memory = memory,
                                    hasNotificationPermission = viewModel.hasNotificationPermission(),
                                    onSelectTheme = viewModel::selectTheme,
                                    onUpdateCustomTheme = viewModel::updateCustomTheme,
                                    onSetUseFahrenheit = viewModel::setUseFahrenheit,
                                    onSetHighTempAlert = viewModel::setHighTempAlert,
                                    onSetHighTempThreshold = viewModel::setHighTempThreshold,
                                    onSetChargingHeatAlert = viewModel::setChargingHeatAlert,
                                    onSetLowBatteryAlert = viewModel::setLowBatteryAlert,
                                    onSetLowBatteryThreshold = viewModel::setLowBatteryThreshold,
                                    onSetMonitorInterval = viewModel::setMonitorIntervalSeconds,
                                    onSendTestNotification = viewModel::sendTestNotification,
                                    onClearHistory = viewModel::clearRecordedHistory
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
