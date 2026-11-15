package com.example.hometheatre.core

enum class AudioConnectionType(val label: String) {
    BUILTIN_SPEAKER("Built-in Speaker"),
    WIRED_AUX("Wired AUX / 3.5mm"),
    USB_AUDIO("USB-C DAC / Audio"),
    BLUETOOTH_A2DP("Bluetooth Audio"),
    BLUETOOTH_LE_AUDIO("Bluetooth LE Audio"),
    HDMI("HDMI Output"),
    HDMI_ARC("HDMI ARC"),
    HDMI_EARC("HDMI eARC"),
    WIFI_CONTROLLER("Wi-Fi Controller"),
    UNKNOWN("Other Audio Route")
}

data class ConnectedAudioDeviceInfo(
    val id: Int,
    val name: String,
    val connectionType: AudioConnectionType,
    val isActiveOutput: Boolean,
    val channelCounts: List<Int>,
    val sampleRates: List<Int>,
    val encodings: List<String>,
    val supportsSubwooferChannel: Boolean,
    val supportsDolbyPassthrough: Boolean
)

data class AncCapabilityStatus(
    val connectedHeadphoneName: String?,
    val connectionType: AudioConnectionType?,
    val hardwareAncDetected: Boolean,
    val transparencySupported: Boolean,
    val adaptiveAncSupported: Boolean,
    val oemSdkIntegrated: Boolean,
    val currentModeLabel: String,
    val diagnosticNote: String
)

data class SpatialCapabilityStatus(
    val apiSupported: Boolean,
    val isAvailable: Boolean,
    val isEnabled: Boolean,
    val immersiveLevelLabel: String,
    val headTrackerAvailable: Boolean,
    val headTrackerEnabled: Boolean,
    val fallbackVirtualizerActive: Boolean,
    val diagnosticNote: String
)

data class DolbyCapabilityStatus(
    val dolbyAtmosDecoderPresent: Boolean,
    val dolbyAc3DecoderPresent: Boolean,
    val dolbyEac3DecoderPresent: Boolean,
    val dolbyAc4DecoderPresent: Boolean,
    val sinkSupportsAtmosPassthrough: Boolean,
    val hdmiArcOrEarcDetected: Boolean,
    val activeRouteSupportsDirectAtmos: Boolean,
    val detectedCodecs: List<String>,
    val honestyStatusBanner: String
)

data class DeviceCompatibilityReport(
    val androidSdkInt: Int,
    val androidVersionRelease: String,
    val deviceModel: String,
    val hasLowLatencyFeature: Boolean,
    val hasProAudioFeature: Boolean,
    val hasDynamicsProcessingApi: Boolean,
    val bluetoothPermissionGranted: Boolean,
    val connectedOutputs: List<ConnectedAudioDeviceInfo>,
    val primaryOutput: ConnectedAudioDeviceInfo?,
    val ancStatus: AncCapabilityStatus,
    val spatialStatus: SpatialCapabilityStatus,
    val dolbyStatus: DolbyCapabilityStatus,
    val currentMusicVolume: Int,
    val maxMusicVolume: Int
)
