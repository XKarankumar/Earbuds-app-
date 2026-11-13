package com.example.model

enum class EarbudBrand(
    val displayName: String,
    val shortCode: String,
    val officialCompanionName: String,
    val officialSupportUrl: String,
    val protocolNote: String,
) {
    VIVO(
        displayName = "vivo / iQOO",
        shortCode = "VIVO",
        officialCompanionName = "vivo TWS / OriginOS Audio",
        officialSupportUrl = "https://www.vivo.com/en/support",
        protocolNote = "Standard A2DP/HFP telemetry supported; split case battery & proprietary ANC use vivo BLE vendor packets.",
    ),
    ONEPLUS(
        displayName = "OnePlus",
        shortCode = "1+",
        officialCompanionName = "OnePlus Wireless Earphones / HeyMelody",
        officialSupportUrl = "https://service.oneplus.com/",
        protocolNote = "Standard A2DP/HFP & GATT supported; OEM firmware signing & split battery use HeyMelody BLE protocol.",
    ),
    SAMSUNG(
        displayName = "Samsung",
        shortCode = "SMG",
        officialCompanionName = "Galaxy Wearable",
        officialSupportUrl = "https://www.samsung.com/support/",
        protocolNote = "Standard Bluetooth audio supported; Galaxy Buds per-bud telemetry & OTA require Galaxy Wearable SPP.",
    ),
    JBL(
        displayName = "JBL",
        shortCode = "JBL",
        officialCompanionName = "JBL Headphones",
        officialSupportUrl = "https://support.jbl.com/",
        protocolNote = "Standard A2DP/HFP supported; Harman/JBL EQ curves & OTA use proprietary BLE GATT service.",
    ),
    REALME(
        displayName = "realme",
        shortCode = "RME",
        officialCompanionName = "realme Link",
        officialSupportUrl = "https://www.realme.com/support",
        protocolNote = "Standard Bluetooth audio supported; realme Link controls & firmware OTA require OEM signed packets.",
    ),
    OPPO(
        displayName = "OPPO",
        shortCode = "OPPO",
        officialCompanionName = "HeyMelody",
        officialSupportUrl = "https://support.oppo.com/",
        protocolNote = "Standard A2DP/HFP supported; Enco dual-bud telemetry & firmware OTA use OPPO/HeyMelody protocol.",
    ),
    BOAT(
        displayName = "boAt",
        shortCode = "boAt",
        officialCompanionName = "boAt Hearables",
        officialSupportUrl = "https://support.boat-lifestyle.com/",
        protocolNote = "Standard A2DP/HFP supported; Airdopes/Nirvana OEM OTA & custom profiles use boAt Hearables BLE.",
    ),
    APPLE(
        displayName = "Apple",
        shortCode = "AAPL",
        officialCompanionName = "Apple Continuity BLE",
        officialSupportUrl = "https://support.apple.com/airpods",
        protocolNote = "Standard A2DP/HFP audio works on Android; Continuity proximity packets are proprietary to Apple.",
    ),
    SONY(
        displayName = "Sony",
        shortCode = "SONY",
        officialCompanionName = "Sony Sound Connect",
        officialSupportUrl = "https://www.sony.com/electronics/support",
        protocolNote = "Standard A2DP/LDAC/HFP supported; Sony DSEE/ANC & signed OTA use Sony proprietary SPP.",
    ),
    NOTHING(
        displayName = "Nothing / CMF",
        shortCode = "NTH",
        officialCompanionName = "Nothing X",
        officialSupportUrl = "https://support.nothing.tech/",
        protocolNote = "Standard A2DP/HFP supported; Nothing X ANC & OTA require Nothing X protocol.",
    ),
    GENERIC(
        displayName = "Bluetooth Audio",
        shortCode = "BT",
        officialCompanionName = "Standard Android Bluetooth",
        officialSupportUrl = "https://source.android.com/docs/core/connect/bluetooth",
        protocolNote = "Using standard Android A2DP, HFP battery level, AudioFX DSP, and GATT 0x180F/0x180A profiles.",
    ),
}

enum class ThemePreference(val label: String, val subtitle: String) {
    WHITE("Pure White Glass", "Signature luminous white & frosted silver"),
    AUTO("Adaptive", "Follows system light or dark appearance"),
    BLACK("Obsidian Glass", "Deep black frosted monochrome"),
}

enum class PopupStyle(val label: String, val description: String) {
    LIQUID_SHEET("iOS Liquid Sheet", "Bottom frosted glass card with 3D earbuds"),
    DYNAMIC_CAPSULE("Dynamic Capsule", "Top floating pill expanding with telemetry"),
    MINIMAL_CARD("Minimal Card", "Compact white glass status toast"),
}

enum class AnimationStyle(val label: String, val description: String) {
    LIQUID_SPRING("Liquid Spring", "Organic iOS-inspired spring physics"),
    ORBITAL_3D("Orbital 3D", "Continuous 3D rotation & floating depth"),
    CRISP_MINIMAL("Crisp Minimal", "Fast, understated micro-transitions"),
}

enum class NotificationStyle(val label: String, val description: String) {
    DETAILED_TELEMETRY("Detailed Telemetry", "Brand, battery level, codec & audio route"),
    COMPACT_MONOCHROME("Compact", "Minimal connection status line"),
    QUIET_STATUS("Quiet Monitor", "Silent status badge without pop-up alerts"),
}

enum class LayoutDensity(val label: String) {
    EXPANDED_STUDIO("Studio Showcase"),
    COMPACT_MINIMAL("Compact Grid"),
}

data class UiCustomizationConfig(
    val themePreference: ThemePreference = ThemePreference.WHITE,
    val popupStyle: PopupStyle = PopupStyle.LIQUID_SHEET,
    val animationStyle: AnimationStyle = AnimationStyle.LIQUID_SPRING,
    val notificationStyle: NotificationStyle = NotificationStyle.DETAILED_TELEMETRY,
    val glassTransparency: Float = 0.80f,
    val glassBlurDp: Float = 20f,
    val silverReflectionIntensity: Float = 0.72f,
    val cardCornerRadiusDp: Float = 26f,
    val animationSpeedMultiplier: Float = 1.0f,
    val enable3dAutoOrbit: Boolean = true,
    val layoutDensity: LayoutDensity = LayoutDensity.EXPANDED_STUDIO,
    val showEqCardOnHome: Boolean = true,
    val autoConnectionPopupEnabled: Boolean = true,
    val backgroundMonitoringEnabled: Boolean = false,
)

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
        description = "Neutral studio reference curve",
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
    val isUnifiedSingleReport: Boolean = false,
    val lowBatteryThreshold: Int = 20,
    val sourceLabel: String = "Awaiting Bluetooth connection",
) {
    val hasAnyLevel: Boolean
        get() = leftLevel != null || rightLevel != null || caseLevel != null

    val isLowBattery: Boolean
        get() = listOfNotNull(leftLevel, rightLevel, caseLevel).any { it <= lowBatteryThreshold }
}

data class DiscoveredBluetoothDevice(
    val name: String,
    val address: String,
    val brand: EarbudBrand = EarbudBrand.GENERIC,
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
    val whyNeeded: String,
    val isGranted: Boolean,
    val isRuntimeRequestable: Boolean = true,
)

enum class FirmwareCheckStage {
    IDLE,
    VERIFYING_GATT_AND_OEM,
    UP_TO_DATE_STANDARD,
    PROPRIETARY_OEM_PROTOCOL,
    DFU_SERVICE_READY,
    OFFLINE_OR_DISCONNECTED,
}

data class FirmwareInfoState(
    val installedVersion: String? = null,
    val hardwareRevision: String? = null,
    val softwareRevision: String? = null,
    val manufacturerName: String? = null,
    val modelNumber: String? = null,
    val hasStandardDfuService: Boolean = false,
    val discoveredServiceCount: Int = 0,
    val checkStage: FirmwareCheckStage = FirmwareCheckStage.IDLE,
    val statusHeadline: String = "Ready to verify connected earbuds",
    val statusDetail: String = "Connect earbuds to read GATT Device Information (0x180A) and verify official firmware channels.",
    val officialReachable: Boolean? = null,
    val verificationSteps: List<String> = emptyList(),
    val checkProgress: Float = 0f,
    val lastCheckedLabel: String? = null,
)

data class ConnectionPopupEvent(
    val deviceName: String,
    val address: String?,
    val brand: EarbudBrand,
    val isConnected: Boolean,
    val isPreview: Boolean = false,
    val batteryLevel: Int? = null,
    val transportLabel: String = "Bluetooth Audio",
    val eventId: Long = System.currentTimeMillis(),
)

object CkRoutes {
    const val HOME = "home"
    const val BLUETOOTH_DEVICES = "bluetooth_devices"
    const val BATTERY_DASHBOARD = "battery_dashboard"
    const val FIRMWARE_UPDATE = "firmware_update"
    const val ANIMATION_SETTINGS = "animation_settings"
    const val GLASS_CUSTOMIZATION = "glass_customization"
    const val PERMISSIONS = "app_permissions"
    const val ABOUT_CK = "about_ck"
    const val EQUALIZER = "equalizer"
    const val CONTROLS = "controls"
}

// Unused legacy aliases kept absent; all tabs and screens route through CkRoutes.
enum class StudioTab(val label: String, val route: String) {
    HOME("Home", CkRoutes.HOME),
    DEVICES("Devices", CkRoutes.BLUETOOTH_DEVICES),
    BATTERY("Battery", CkRoutes.BATTERY_DASHBOARD),
    STYLE("Glass", CkRoutes.GLASS_CUSTOMIZATION),
}

data class BudsStudioUiState(
    val connectionState: BluetoothConnectionState = BluetoothConnectionState.DISCONNECTED,
    val connectedDeviceName: String? = null,
    val connectedDeviceAddress: String? = null,
    val connectedBrand: EarbudBrand = EarbudBrand.GENERIC,
    val rememberedDeviceAddresses: Set<String> = emptySet(),
    val statusBanner: String = "Ready · Tap Connect Earbuds or scan nearby devices",
    val isScanning: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val hasBluetoothHardware: Boolean = true,
    val hasAllBluetoothPermissions: Boolean = false,
    val hasNotificationPermission: Boolean = true,
    val permissionChecklist: List<PermissionItemState> = emptyList(),
    val bondedDevices: List<DiscoveredBluetoothDevice> = emptyList(),
    val scannedDevices: List<DiscoveredBluetoothDevice> = emptyList(),
    val audioRouteInfo: AudioRouteInfo = AudioRouteInfo(),
    val battery: BatteryTelemetry = BatteryTelemetry(),
    val firmware: FirmwareInfoState = FirmwareInfoState(),
    val activePopupEvent: ConnectionPopupEvent? = null,
    val customization: UiCustomizationConfig = UiCustomizationConfig(),
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
