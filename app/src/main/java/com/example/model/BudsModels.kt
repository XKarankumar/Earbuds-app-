package com.example.model

enum class NoiseControlMode(val label: String, val shortLabel: String) {
    ANC("Noise cancellation", "ANC"),
    OFF("Off", "Off"),
    TRANSPARENCY("Transparency", "Transparency"),
}

enum class AncStrength(val label: String, val depthDb: Int) {
    LOW("Low", 24),
    MEDIUM("Medium", 36),
    HIGH("High", 49),
}

enum class EqPreset(
    val label: String,
    val description: String,
    val bandsDb: List<Float>,
) {
    BALANCED(
        label = "Balanced",
        description = "Natural reference studio curve",
        bandsDb = listOf(0f, 0f, 0f, 0f, 0f),
    ),
    BASS_BOOST(
        label = "Bass Boost",
        description = "Deep sub-bass & warm low-end punch",
        bandsDb = listOf(5.5f, 4.0f, 0.5f, -0.5f, 1.0f),
    ),
    VOCAL(
        label = "Vocal",
        description = "Forward midrange clarity for voices",
        bandsDb = listOf(-2.0f, 0.5f, 4.5f, 3.5f, 1.0f),
    ),
    TREBLE_BOOST(
        label = "Treble Boost",
        description = "Crisp air, cymbals & acoustic detail",
        bandsDb = listOf(-1.0f, -0.5f, 1.0f, 4.5f, 6.0f),
    ),
    PODCAST(
        label = "Podcast",
        description = "Tight rumble cut & speech presence",
        bandsDb = listOf(-4.5f, -1.5f, 3.5f, 4.0f, 1.5f),
    ),
    CUSTOM(
        label = "Custom",
        description = "User-tuned 5-band acoustic curve",
        bandsDb = listOf(2.0f, 1.0f, 0.0f, 1.5f, 2.5f),
    ),
}

enum class GestureAction(val label: String) {
    NEXT_TRACK("Next track"),
    PREVIOUS_TRACK("Previous track"),
    PLAY_PAUSE("Play / Pause"),
    VOICE_ASSISTANT("Voice assistant"),
    SWITCH_NOISE_CONTROL("Switch noise control"),
    VOLUME_CONTROL("Volume control"),
}

data class EarbudControlsConfig(
    val doubleTapLeft: GestureAction = GestureAction.NEXT_TRACK,
    val doubleTapRight: GestureAction = GestureAction.PLAY_PAUSE,
    val tripleTap: GestureAction = GestureAction.PREVIOUS_TRACK,
    val pressAndHold: GestureAction = GestureAction.SWITCH_NOISE_CONTROL,
)

data class BatteryTelemetry(
    val leftLevel: Int? = null,
    val rightLevel: Int? = null,
    val caseLevel: Int? = null,
    val isLeftCharging: Boolean = false,
    val isRightCharging: Boolean = false,
    val isCaseCharging: Boolean = false,
    val sourceLabel: String = "Awaiting Bluetooth connection",
) {
    val hasAnyLevel: Boolean
        get() = leftLevel != null || rightLevel != null || caseLevel != null
}

data class ConnectedDeviceDetail(
    val address: String,
    val name: String,
    val batteryLevel: Int? = null,
    val batterySource: String = "Battery telemetry pending",
    val codecLabel: String = "A2DP / GATT",
    val sampleRateHz: Int? = null,
    val bitsPerSample: Int? = null,
    val channelCount: Int? = null,
    val firmwareVersion: String? = null,
    val manufacturerName: String? = null,
    val profiles: List<String> = emptyList(),
    val rssi: Int? = null,
    val isPrimaryAudioRoute: Boolean = false,
)

data class DiscoveredBluetoothDevice(
    val name: String,
    val address: String,
    val rssi: Int? = null,
    val isBonded: Boolean = false,
    val isAudioDevice: Boolean = false,
    val isConnected: Boolean = false,
    val deviceTypeLabel: String = "Bluetooth",
)

data class AudioRouteInfo(
    val deviceName: String? = null,
    val transportType: String = "Phone Speaker",
    val sampleRateHz: Int? = null,
    val channelCount: Int? = null,
    val codecLabel: String = "Standby",
    val isBluetoothAudioActive: Boolean = false,
)

enum class BluetoothConnectionState {
    UNAVAILABLE,
    BLUETOOTH_OFF,
    PERMISSION_REQUIRED,
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
}

data class PermissionItemState(
    val permission: String,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val isRuntimeRequestable: Boolean = true,
)

enum class StudioTab(val label: String) {
    DEVICE("Device"),
    EQUALIZER("Equalizer"),
    CONTROLS("Controls"),
}

sealed interface GlassSubMenu {
    data object BluetoothManager : GlassSubMenu
    data object PermissionHelper : GlassSubMenu
    data object NoiseControlSettings : GlassSubMenu
    data object EqualizerSettings : GlassSubMenu
    data object EarbudControlsSettings : GlassSubMenu
    data object DeviceManagement : GlassSubMenu
    data object AboutStudio : GlassSubMenu
}

data class BudsStudioUiState(
    val selectedTab: StudioTab = StudioTab.DEVICE,
    val activeSubMenu: GlassSubMenu? = null,
    val connectionState: BluetoothConnectionState = BluetoothConnectionState.DISCONNECTED,
    val connectedDeviceName: String? = null,
    val connectedDeviceAddress: String? = null,
    val connectedDevices: List<ConnectedDeviceDetail> = emptyList(),
    val rememberedDeviceAddresses: Set<String> = emptySet(),
    val autoReconnectEnabled: Boolean = true,
    val firmwareVersion: String? = null,
    val statusBanner: String = "Ready · Tap Connect earbuds or open Bluetooth Manager",
    val isScanning: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val hasBluetoothHardware: Boolean = true,
    val hasAllPermissions: Boolean = false,
    val permissionChecklist: List<PermissionItemState> = emptyList(),
    val bondedDevices: List<DiscoveredBluetoothDevice> = emptyList(),
    val scannedDevices: List<DiscoveredBluetoothDevice> = emptyList(),
    val audioRouteInfo: AudioRouteInfo = AudioRouteInfo(),
    val battery: BatteryTelemetry = BatteryTelemetry(),
    val noiseControlMode: NoiseControlMode = NoiseControlMode.ANC,
    val ancStrength: AncStrength = AncStrength.HIGH,
    val personalisedAnc: Boolean = true,
    val windNoiseReduction: Boolean = false,
    val transparencyVoiceFocus: Boolean = true,
    val dualConnection: Boolean = false,
    val inEarDetection: Boolean = true,
    val lowLatencyMode: Boolean = false,
    val eqPreset: EqPreset = EqPreset.BALANCED,
    val eqBandsDb: List<Float> = EqPreset.BALANCED.bandsDb,
    val bassBoostStrength: Float = 0.25f,
    val isHardwareEqAttached: Boolean = false,
    val hardwareEqStatus: String = "Studio DSP profile ready",
    val controls: EarbudControlsConfig = EarbudControlsConfig(),
)
