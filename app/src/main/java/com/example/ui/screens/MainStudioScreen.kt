package com.example.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.bluetooth.PermissionHelper
import com.example.model.BluetoothConnectionState
import com.example.model.CkRoutes
import com.example.model.StudioTab
import com.example.ui.BudsStudioViewModel
import com.example.ui.components.BudsConnectionPopupOverlay
import com.example.ui.components.LiquidGlassStageBackground
import com.example.ui.components.backdropGlassBlur
import com.example.ui.theme.LocalLiquidGlassStyle
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SilverEdgeRim

@Composable
fun CkStudioRoute(
    viewModel: BudsStudioViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: CkRoutes.HOME

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        viewModel.refreshPermissions()
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        viewModel.refreshPermissions()
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.refreshPermissions()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshPermissions()
    }

    val requestBluetoothPermissions: () -> Unit = {
        btPermissionLauncher.launch(PermissionHelper.requiredBluetoothPermissions())
    }

    val requestNotificationPermission: () -> Unit = {
        val perms = PermissionHelper.requiredNotificationPermission()
        if (perms.isNotEmpty()) {
            notifPermissionLauncher.launch(perms)
        }
    }

    val requestEnableBluetooth: () -> Unit = {
        runCatching {
            enableBluetoothLauncher.launch(PermissionHelper.createEnableBluetoothIntent())
        }.onFailure {
            PermissionHelper.openBluetoothSettings(context)
        }
    }

    val handleBackgroundMonitorToggle: (Boolean) -> Unit = { enabled ->
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !uiState.hasNotificationPermission) {
            requestNotificationPermission()
        }
        if (enabled && !uiState.hasAllBluetoothPermissions) {
            requestBluetoothPermissions()
        }
        viewModel.setBackgroundMonitoringEnabled(enabled)
    }

    val navigateTo: (String) -> Unit = { route ->
        if (currentRoute != route) {
            navController.navigate(route) {
                launchSingleTop = true
            }
        }
    }

    val navigateTopTab: (StudioTab) -> Unit = { tab ->
        if (currentRoute != tab.route) {
            navController.navigate(tab.route) {
                popUpTo(CkRoutes.HOME) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val onPrimaryConnectClick: () -> Unit = {
        when (uiState.connectionState) {
            BluetoothConnectionState.PERMISSION_REQUIRED -> navigateTo(CkRoutes.PERMISSIONS)
            BluetoothConnectionState.BLUETOOTH_OFF -> requestEnableBluetooth()
            else -> navigateTo(CkRoutes.BLUETOOTH_DEVICES)
        }
    }

    val showPopupBlur = uiState.activePopupEvent != null
    val glassStyle = LocalLiquidGlassStyle.current

    LiquidGlassStageBackground {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .backdropGlassBlur(isBlurred = showPopupBlur),
        ) {
            val useRail = maxWidth >= 700.dp

            if (useRail) {
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRail(
                        containerColor = if (glassStyle.isDark) {
                            MaterialTheme.colorScheme.surface.copy(alpha = glassStyle.glassAlpha)
                        } else {
                            PureWhite.copy(alpha = (glassStyle.glassAlpha + 0.08f).coerceAtMost(0.95f))
                        },
                        modifier = Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.statusBars),
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        StudioTab.entries.forEach { tab ->
                            NavigationRailItem(
                                selected = currentRoute == tab.route,
                                onClick = { navigateTopTab(tab) },
                                icon = { Icon(tabIcon(tab), contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_rail_${tab.name.lowercase()}"),
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .navigationBarsPadding(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        CkNavHost(
                            navController = navController,
                            uiState = uiState,
                            viewModel = viewModel,
                            isWideLayout = true,
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
                            onPrimaryConnectClick = onPrimaryConnectClick,
                            onToggleBackgroundMonitor = handleBackgroundMonitorToggle,
                            onNavigate = navigateTo,
                            onRequestBluetoothPermissions = requestBluetoothPermissions,
                            onRequestNotificationPermission = requestNotificationPermission,
                            onEnableBluetooth = requestEnableBluetooth,
                        )
                    }
                }
            } else {
                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets.statusBars,
                    bottomBar = {
                        FrostedWhiteBottomNavBar(
                            currentRoute = currentRoute,
                            onSelectTab = navigateTopTab,
                        )
                    },
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        CkNavHost(
                            navController = navController,
                            uiState = uiState,
                            viewModel = viewModel,
                            isWideLayout = false,
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                            onPrimaryConnectClick = onPrimaryConnectClick,
                            onToggleBackgroundMonitor = handleBackgroundMonitorToggle,
                            onNavigate = navigateTo,
                            onRequestBluetoothPermissions = requestBluetoothPermissions,
                            onRequestNotificationPermission = requestNotificationPermission,
                            onEnableBluetooth = requestEnableBluetooth,
                        )
                    }
                }
            }
        }

        BudsConnectionPopupOverlay(
            event = uiState.activePopupEvent,
            popupStyle = uiState.customization.popupStyle,
            onDismiss = viewModel::dismissPopup,
            onOpenBluetoothDevices = { navigateTo(CkRoutes.BLUETOOTH_DEVICES) },
        )
    }
}

@Composable
private fun CkNavHost(
    navController: androidx.navigation.NavHostController,
    uiState: com.example.model.BudsStudioUiState,
    viewModel: BudsStudioViewModel,
    isWideLayout: Boolean,
    contentPadding: PaddingValues,
    onPrimaryConnectClick: () -> Unit,
    onToggleBackgroundMonitor: (Boolean) -> Unit,
    onNavigate: (String) -> Unit,
    onRequestBluetoothPermissions: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onEnableBluetooth: () -> Unit,
) {
    val context = LocalContext.current
    val animMs = (280 / uiState.customization.animationSpeedMultiplier.coerceIn(0.6f, 1.5f)).toInt()

    NavHost(
        navController = navController,
        startDestination = CkRoutes.HOME,
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = if (isWideLayout) 1040.dp else 640.dp),
        enterTransition = {
            fadeIn(tween(animMs)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                tween(animMs),
            )
        },
        exitTransition = {
            fadeOut(tween(animMs)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                tween(animMs),
            )
        },
        popEnterTransition = {
            fadeIn(tween(animMs)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                tween(animMs),
            )
        },
        popExitTransition = {
            fadeOut(tween(animMs)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                tween(animMs),
            )
        },
    ) {
        composable(CkRoutes.HOME) {
            HomeScreen(
                uiState = uiState,
                viewModel = viewModel,
                isWideLayout = isWideLayout,
                contentPadding = contentPadding,
                onPrimaryConnectClick = onPrimaryConnectClick,
                onToggleBackgroundMonitor = onToggleBackgroundMonitor,
                onNavigate = onNavigate,
            )
        }

        composable(CkRoutes.FIRMWARE_UPDATE) {
            FirmwareUpdateScreen(
                uiState = uiState,
                contentPadding = contentPadding,
                onBack = { navController.popBackStack() },
                onCheckOfficialFirmware = viewModel::checkOfficialFirmware,
                onOpenOfficialSupport = { url -> PermissionHelper.openUrl(context, url) },
                onOpenBluetoothSettings = { PermissionHelper.openBluetoothSettings(context) },
            )
        }

        composable(CkRoutes.BLUETOOTH_DEVICES) {
            BluetoothDevicesScreen(
                uiState = uiState,
                contentPadding = contentPadding,
                onBack = { navController.popBackStack() },
                onNavigatePermissions = { onNavigate(CkRoutes.PERMISSIONS) },
                onEnableBluetooth = onEnableBluetooth,
                onStartScan = viewModel::startBluetoothScan,
                onStopScan = viewModel::stopBluetoothScan,
                onConnectDevice = viewModel::connectDevice,
                onDisconnectCurrent = viewModel::disconnectDevice,
                onOpenSystemBluetoothSettings = { PermissionHelper.openBluetoothSettings(context) },
            )
        }

        composable(CkRoutes.BATTERY_DASHBOARD) {
            BatteryDashboardScreen(
                uiState = uiState,
                contentPadding = contentPadding,
                onBack = { navController.popBackStack() },
                onUpdateLowBatteryThreshold = viewModel::setLowBatteryThreshold,
                onOpenBluetoothDevices = { onNavigate(CkRoutes.BLUETOOTH_DEVICES) },
            )
        }

        composable(CkRoutes.ANIMATION_SETTINGS) {
            AnimationSettingsScreen(
                uiState = uiState,
                viewModel = viewModel,
                contentPadding = contentPadding,
                onBack = { navController.popBackStack() },
            )
        }

        composable(CkRoutes.GLASS_CUSTOMIZATION) {
            GlassCustomizationScreen(
                uiState = uiState,
                viewModel = viewModel,
                contentPadding = contentPadding,
                onBack = { navController.popBackStack() },
                onOpenAnimationSettings = { onNavigate(CkRoutes.ANIMATION_SETTINGS) },
                onToggleBackgroundMonitor = onToggleBackgroundMonitor,
            )
        }

        composable(CkRoutes.PERMISSIONS) {
            PermissionsScreen(
                uiState = uiState,
                contentPadding = contentPadding,
                onBack = { navController.popBackStack() },
                onRequestBluetoothPermissions = onRequestBluetoothPermissions,
                onRequestNotificationPermission = onRequestNotificationPermission,
                onEnableBluetooth = onEnableBluetooth,
                onOpenAppSettings = { PermissionHelper.openAppSettings(context) },
                onRefresh = viewModel::refreshPermissions,
            )
        }

        composable(CkRoutes.ABOUT_CK) {
            AboutCkScreen(
                uiState = uiState,
                contentPadding = contentPadding,
                onBack = { navController.popBackStack() },
            )
        }

        composable(CkRoutes.EQUALIZER) {
            EqualizerScreen(
                uiState = uiState,
                viewModel = viewModel,
                contentPadding = contentPadding,
                onBack = { navController.popBackStack() },
            )
        }

        composable(CkRoutes.CONTROLS) {
            ControlsAndDeviceScreen(
                uiState = uiState,
                viewModel = viewModel,
                contentPadding = contentPadding,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun FrostedWhiteBottomNavBar(
    currentRoute: String,
    onSelectTab: (StudioTab) -> Unit,
) {
    val glassStyle = LocalLiquidGlassStyle.current
    val borderBrush = if (glassStyle.isDark) {
        Brush.horizontalGradient(
            listOf(
                Color.White.copy(alpha = 0.25f),
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.25f),
            ),
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                PureWhite,
                SilverEdgeRim,
                PureWhite,
            ),
        )
    }

    Surface(
        color = if (glassStyle.isDark) {
            MaterialTheme.colorScheme.surface.copy(alpha = (glassStyle.glassAlpha + 0.08f).coerceAtMost(0.95f))
        } else {
            PureWhite.copy(alpha = (glassStyle.glassAlpha + 0.12f).coerceAtMost(0.96f))
        },
        shadowElevation = 12.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            ),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            modifier = Modifier.navigationBarsPadding(),
        ) {
            StudioTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = currentRoute == tab.route,
                    onClick = { onSelectTab(tab) },
                    icon = { Icon(tabIcon(tab), contentDescription = tab.label) },
                    label = { Text(tab.label) },
                    modifier = Modifier.testTag("bottom_nav_${tab.name.lowercase()}"),
                )
            }
        }
    }
}

private fun tabIcon(tab: StudioTab): ImageVector {
    return when (tab) {
        StudioTab.HOME -> Icons.Rounded.Headphones
        StudioTab.DEVICES -> Icons.Rounded.Bluetooth
        StudioTab.BATTERY -> Icons.Rounded.BatteryChargingFull
        StudioTab.STYLE -> Icons.Rounded.Palette
    }
}
