package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.bluetooth.PermissionHelper
import com.example.model.BluetoothConnectionState
import com.example.model.BudsStudioUiState
import com.example.model.GlassSubMenu
import com.example.model.StudioTab
import com.example.ui.BudsStudioViewModel
import com.example.ui.components.BatteryUiSection
import com.example.ui.components.EqualizerPreviewCard
import com.example.ui.components.GlassModalOverlay
import com.example.ui.components.HeroEarbudsSection
import com.example.ui.components.NoiseControlSection
import com.example.ui.components.StudioTopBar
import com.example.ui.components.backdropGlassBlur
import com.example.ui.sheets.AboutStudioSheetContent
import com.example.ui.sheets.BluetoothManagerSheetContent
import com.example.ui.sheets.DeviceManagementSheetContent
import com.example.ui.sheets.EarbudControlsSettingsSheetContent
import com.example.ui.sheets.EqualizerSettingsSheetContent
import com.example.ui.sheets.NoiseControlSettingsSheetContent
import com.example.ui.sheets.PermissionHelperSheetContent
import com.example.ui.theme.StudioMintSoft

@Composable
fun KaranBudsStudioRoute(
    viewModel: BudsStudioViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
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

    val requestBluetoothPermissions = {
        permissionLauncher.launch(PermissionHelper.requiredRuntimePermissions())
    }

    val requestEnableBluetooth: () -> Unit = {
        runCatching {
            enableBluetoothLauncher.launch(PermissionHelper.createEnableBluetoothIntent())
        }.onFailure {
            PermissionHelper.openBluetoothSettings(context)
        }
    }

    val onPrimaryConnectClick: () -> Unit = {
        when (uiState.connectionState) {
            BluetoothConnectionState.PERMISSION_REQUIRED -> {
                viewModel.openSubMenu(GlassSubMenu.PermissionHelper)
            }
            BluetoothConnectionState.BLUETOOTH_OFF -> {
                requestEnableBluetooth()
            }
            else -> {
                viewModel.openSubMenu(GlassSubMenu.BluetoothManager)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MainStudioScaffold(
            uiState = uiState,
            viewModel = viewModel,
            onPrimaryConnectClick = onPrimaryConnectClick,
            modifier = Modifier
                .fillMaxSize()
                .backdropGlassBlur(isBlurred = uiState.activeSubMenu != null),
        )

        val activeMenu = uiState.activeSubMenu
        val (title, subtitle, icon) = when (activeMenu) {
            GlassSubMenu.BluetoothManager -> Triple(
                stringResource(R.string.bluetooth_manager_title),
                "Pair, scan & inspect audio routes",
                Icons.Rounded.Bluetooth,
            )
            GlassSubMenu.PermissionHelper -> Triple(
                stringResource(R.string.permission_helper_title),
                "Bluetooth & audio readiness checklist",
                Icons.Rounded.Security,
            )
            GlassSubMenu.NoiseControlSettings -> Triple(
                stringResource(R.string.noise_control_title),
                "ANC depth, wind & voice pass-through",
                Icons.Rounded.GraphicEq,
            )
            GlassSubMenu.EqualizerSettings -> Triple(
                stringResource(R.string.equalizer_title),
                "5-band acoustic curve & Bass Boost",
                Icons.Rounded.Equalizer,
            )
            GlassSubMenu.EarbudControlsSettings -> Triple(
                stringResource(R.string.earbud_controls_title),
                "Custom tap & press-hold gesture map",
                Icons.Rounded.TouchApp,
            )
            GlassSubMenu.DeviceManagement -> Triple(
                stringResource(R.string.device_management_title),
                "Dual connection, wear sensor & latency",
                Icons.Rounded.Devices,
            )
            GlassSubMenu.AboutStudio -> Triple(
                stringResource(R.string.about_studio_title),
                "Karan Audio Lab · Compatibility & telemetry",
                Icons.Rounded.Info,
            )
            null -> Triple("", "", Icons.Rounded.Info)
        }

        GlassModalOverlay(
            visible = activeMenu != null,
            title = title,
            subtitle = subtitle,
            icon = icon,
            onDismiss = viewModel::closeSubMenu,
        ) {
            when (activeMenu) {
                GlassSubMenu.BluetoothManager -> BluetoothManagerSheetContent(
                    uiState = uiState,
                    onRequestPermissions = { viewModel.openSubMenu(GlassSubMenu.PermissionHelper) },
                    onEnableBluetooth = requestEnableBluetooth,
                    onStartScan = viewModel::startBluetoothScan,
                    onStopScan = viewModel::stopBluetoothScan,
                    onConnectDevice = viewModel::connectDevice,
                    onDisconnectCurrent = viewModel::disconnectDevice,
                    onOpenSystemBluetoothSettings = { PermissionHelper.openBluetoothSettings(context) },
                )
                GlassSubMenu.PermissionHelper -> PermissionHelperSheetContent(
                    uiState = uiState,
                    onRequestPermissions = requestBluetoothPermissions,
                    onEnableBluetooth = requestEnableBluetooth,
                    onOpenAppSettings = { PermissionHelper.openAppSettings(context) },
                    onRefresh = viewModel::refreshPermissions,
                )
                GlassSubMenu.NoiseControlSettings -> NoiseControlSettingsSheetContent(
                    uiState = uiState,
                    onSelectMode = viewModel::setNoiseControlMode,
                    onSelectStrength = viewModel::setAncStrength,
                    onTogglePersonalisedAnc = viewModel::setPersonalisedAnc,
                    onToggleWindReduction = viewModel::setWindNoiseReduction,
                    onToggleTransparencyVoiceFocus = viewModel::setTransparencyVoiceFocus,
                )
                GlassSubMenu.EqualizerSettings -> EqualizerSettingsSheetContent(
                    uiState = uiState,
                    onSelectPreset = viewModel::selectEqPreset,
                    onUpdateBand = viewModel::updateEqBand,
                    onUpdateBassBoost = viewModel::updateBassBoost,
                )
                GlassSubMenu.EarbudControlsSettings -> EarbudControlsSettingsSheetContent(
                    uiState = uiState,
                    onSetDoubleTapLeft = viewModel::setDoubleTapLeft,
                    onSetDoubleTapRight = viewModel::setDoubleTapRight,
                    onSetTripleTap = viewModel::setTripleTap,
                    onSetPressAndHold = viewModel::setPressAndHold,
                )
                GlassSubMenu.DeviceManagement -> DeviceManagementSheetContent(
                    uiState = uiState,
                    onToggleDualConnection = viewModel::setDualConnection,
                    onToggleInEarDetection = viewModel::setInEarDetection,
                    onToggleLowLatency = viewModel::setLowLatencyMode,
                )
                GlassSubMenu.AboutStudio -> AboutStudioSheetContent(uiState = uiState)
                null -> Unit
            }
        }
    }
}

@Composable
private fun MainStudioScaffold(
    uiState: BudsStudioUiState,
    viewModel: BudsStudioViewModel,
    onPrimaryConnectClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val useRail = maxWidth >= 700.dp

        if (useRail) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.statusBars),
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    StudioTab.entries.forEach { tab ->
                        NavigationRailItem(
                            selected = uiState.selectedTab == tab,
                            onClick = { viewModel.selectTab(tab) },
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
                    StudioTabContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        isWideLayout = true,
                        onPrimaryConnectClick = onPrimaryConnectClick,
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
                    )
                }
            }
        } else {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.statusBars,
                bottomBar = {
                    FrostedBottomNavBar(
                        selectedTab = uiState.selectedTab,
                        onSelectTab = viewModel::selectTab,
                    )
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    StudioTabContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        isWideLayout = false,
                        onPrimaryConnectClick = onPrimaryConnectClick,
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FrostedBottomNavBar(
    selectedTab: StudioTab,
    onSelectTab: (StudioTab) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.45f),
                        StudioMintSoft.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.45f),
                    ),
                ),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            ),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            modifier = Modifier.navigationBarsPadding(),
        ) {
            StudioTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = selectedTab == tab,
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
        StudioTab.DEVICE -> Icons.Rounded.Headphones
        StudioTab.EQUALIZER -> Icons.Rounded.Equalizer
        StudioTab.CONTROLS -> Icons.Rounded.Tune
    }
}

@Composable
private fun StudioTabContent(
    uiState: BudsStudioUiState,
    viewModel: BudsStudioViewModel,
    isWideLayout: Boolean,
    onPrimaryConnectClick: () -> Unit,
    contentPadding: PaddingValues,
) {
    AnimatedContent(
        targetState = uiState.selectedTab,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "studio_tab_transition",
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = if (isWideLayout) 1040.dp else 640.dp),
    ) { tab ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            StudioTopBar(
                hasAllPermissions = uiState.hasAllPermissions,
                isBluetoothEnabled = uiState.isBluetoothEnabled,
                onOpenPermissions = { viewModel.openSubMenu(GlassSubMenu.PermissionHelper) },
                onOpenBluetoothManager = { viewModel.openSubMenu(GlassSubMenu.BluetoothManager) },
                onOpenAbout = { viewModel.openSubMenu(GlassSubMenu.AboutStudio) },
            )

            when (tab) {
                StudioTab.DEVICE -> {
                    val deviceTitle = uiState.connectedDeviceName
                        ?: stringResource(R.string.device_model_default)

                    if (isWideLayout) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(18.dp),
                            ) {
                                HeroEarbudsSection(
                                    deviceTitle = deviceTitle,
                                    connectionState = uiState.connectionState,
                                    statusBanner = uiState.statusBanner,
                                    audioRouteInfo = uiState.audioRouteInfo,
                                    onPrimaryConnectClick = onPrimaryConnectClick,
                                    onOpenBluetoothManager = {
                                        viewModel.openSubMenu(GlassSubMenu.BluetoothManager)
                                    },
                                )
                                BatteryUiSection(battery = uiState.battery)
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(18.dp),
                            ) {
                                NoiseControlSection(
                                    mode = uiState.noiseControlMode,
                                    ancStrength = uiState.ancStrength,
                                    personalisedAnc = uiState.personalisedAnc,
                                    onSelectMode = viewModel::setNoiseControlMode,
                                    onSelectStrength = viewModel::setAncStrength,
                                    onTogglePersonalisedAnc = viewModel::setPersonalisedAnc,
                                    onOpenNoiseSettings = {
                                        viewModel.openSubMenu(GlassSubMenu.NoiseControlSettings)
                                    },
                                )
                                EqualizerPreviewCard(
                                    preset = uiState.eqPreset,
                                    bandsDb = uiState.eqBandsDb,
                                    hardwareStatus = uiState.hardwareEqStatus,
                                    onOpenEqualizer = {
                                        viewModel.openSubMenu(GlassSubMenu.EqualizerSettings)
                                    },
                                )
                                DeviceManagementSection(uiState = uiState, viewModel = viewModel)
                            }
                        }
                    } else {
                        HeroEarbudsSection(
                            deviceTitle = deviceTitle,
                            connectionState = uiState.connectionState,
                            statusBanner = uiState.statusBanner,
                            audioRouteInfo = uiState.audioRouteInfo,
                            onPrimaryConnectClick = onPrimaryConnectClick,
                            onOpenBluetoothManager = {
                                viewModel.openSubMenu(GlassSubMenu.BluetoothManager)
                            },
                        )

                        BatteryUiSection(battery = uiState.battery)

                        NoiseControlSection(
                            mode = uiState.noiseControlMode,
                            ancStrength = uiState.ancStrength,
                            personalisedAnc = uiState.personalisedAnc,
                            onSelectMode = viewModel::setNoiseControlMode,
                            onSelectStrength = viewModel::setAncStrength,
                            onTogglePersonalisedAnc = viewModel::setPersonalisedAnc,
                            onOpenNoiseSettings = {
                                viewModel.openSubMenu(GlassSubMenu.NoiseControlSettings)
                            },
                        )

                        EqualizerPreviewCard(
                            preset = uiState.eqPreset,
                            bandsDb = uiState.eqBandsDb,
                            hardwareStatus = uiState.hardwareEqStatus,
                            onOpenEqualizer = {
                                viewModel.openSubMenu(GlassSubMenu.EqualizerSettings)
                            },
                        )

                        DeviceManagementSection(uiState = uiState, viewModel = viewModel)
                    }
                }

                StudioTab.EQUALIZER -> {
                    Text(
                        text = stringResource(R.string.equalizer_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    EqualizerSettingsSheetContent(
                        uiState = uiState,
                        onSelectPreset = viewModel::selectEqPreset,
                        onUpdateBand = viewModel::updateEqBand,
                        onUpdateBassBoost = viewModel::updateBassBoost,
                    )
                }

                StudioTab.CONTROLS -> {
                    Text(
                        text = stringResource(R.string.earbud_controls_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    EarbudControlsSettingsSheetContent(
                        uiState = uiState,
                        onSetDoubleTapLeft = viewModel::setDoubleTapLeft,
                        onSetDoubleTapRight = viewModel::setDoubleTapRight,
                        onSetTripleTap = viewModel::setTripleTap,
                        onSetPressAndHold = viewModel::setPressAndHold,
                    )
                    DeviceManagementSheetContent(
                        uiState = uiState,
                        onToggleDualConnection = viewModel::setDualConnection,
                        onToggleInEarDetection = viewModel::setInEarDetection,
                        onToggleLowLatency = viewModel::setLowLatencyMode,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceManagementSection(
    uiState: BudsStudioUiState,
    viewModel: BudsStudioViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "DEVICE MANAGEMENT",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(24.dp),
                ),
        ) {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Dual connection",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Stay connected to two audio sources simultaneously",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = uiState.dualConnection,
                        onCheckedChange = viewModel::setDualConnection,
                        modifier = Modifier.testTag("dual_connection_switch"),
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                SettingsMenuRow(
                    icon = Icons.Rounded.Bluetooth,
                    title = "Bluetooth Manager",
                    subtitle = uiState.audioRouteInfo.transportType,
                    onClick = { viewModel.openSubMenu(GlassSubMenu.BluetoothManager) },
                    testTag = "row_bluetooth_manager",
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                SettingsMenuRow(
                    icon = Icons.Rounded.TouchApp,
                    title = "Earbud controls",
                    subtitle = "Tap: ${uiState.controls.doubleTapLeft.label} / ${uiState.controls.doubleTapRight.label}",
                    onClick = { viewModel.openSubMenu(GlassSubMenu.EarbudControlsSettings) },
                    testTag = "row_earbud_controls",
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                SettingsMenuRow(
                    icon = Icons.Rounded.Settings,
                    title = "Device settings & telemetry",
                    subtitle = "In-ear wear detection, low latency & firmware",
                    onClick = { viewModel.openSubMenu(GlassSubMenu.DeviceManagement) },
                    testTag = "row_device_management",
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                SettingsMenuRow(
                    icon = Icons.Rounded.Security,
                    title = "Permission Helper",
                    subtitle = if (uiState.hasAllPermissions) "All Bluetooth permissions granted" else "Tap to review & grant permissions",
                    onClick = { viewModel.openSubMenu(GlassSubMenu.PermissionHelper) },
                    testTag = "row_permission_helper",
                )
            }
        }
    }
}

@Composable
private fun SettingsMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
