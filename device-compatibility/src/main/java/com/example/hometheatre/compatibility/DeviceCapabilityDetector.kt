package com.example.hometheatre.compatibility

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.hometheatre.core.AncCapabilityStatus
import com.example.hometheatre.core.AudioConnectionType
import com.example.hometheatre.core.ConnectedAudioDeviceInfo
import com.example.hometheatre.core.DeviceCompatibilityReport
import com.example.hometheatre.dolby.DolbyCapabilityDetector
import com.example.hometheatre.spatial.SpatialAudioController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Inspects real Android audio routing, Bluetooth headphone profiles, ANC vendor SDK availability,
 * Spatializer hardware, Dolby codec/passthrough capabilities, and Android version features.
 */
class DeviceCapabilityDetector(
    private val context: Context,
    private val spatialController: SpatialAudioController,
    private val dolbyDetector: DolbyCapabilityDetector
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val packageManager = context.packageManager

    private val _report = MutableStateFlow(buildReport())
    val report: StateFlow<DeviceCompatibilityReport> = _report.asStateFlow()

    private var callbackRegistered = false

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            refresh()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            refresh()
        }
    }

    fun startMonitoring() {
        if (!callbackRegistered) {
            audioManager.registerAudioDeviceCallback(
                audioDeviceCallback,
                Handler(Looper.getMainLooper())
            )
            callbackRegistered = true
        }
        spatialController.startMonitoring()
        refresh()
    }

    fun stopMonitoring() {
        if (callbackRegistered) {
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
            callbackRegistered = false
        }
        spatialController.stopMonitoring()
    }

    fun refresh() {
        spatialController.refreshStatus()
        _report.value = buildReport()
    }

    fun hasBluetoothConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun buildReport(): DeviceCompatibilityReport {
        val rawDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val mapped = rawDevices.mapIndexed { idx, info ->
            val connType = mapConnectionType(info.type)
            val encodings = info.encodings.map { encodingName(it) }.distinct()
            val channels = info.channelCounts.toList().ifEmpty { listOf(2) }
            val rates = info.sampleRates.toList().ifEmpty { listOf(44100, 48000) }
            val productName = info.productName?.toString()?.takeIf { it.isNotBlank() }
                ?: connType.label
            val supportsSub = channels.any { it >= 6 } ||
                encodings.any { it.contains("AC-3") || it.contains("DTS") || it.contains("Atmos") }
            val supportsDolby = encodings.any {
                it.contains("AC-3") || it.contains("E-AC-3") || it.contains("AC-4") || it.contains("TrueHD")
            }
            ConnectedAudioDeviceInfo(
                id = info.id,
                name = productName,
                connectionType = connType,
                isActiveOutput = idx == 0 || connType != AudioConnectionType.BUILTIN_SPEAKER,
                channelCounts = channels,
                sampleRates = rates,
                encodings = encodings.ifEmpty { listOf("PCM 16-bit") },
                supportsSubwooferChannel = supportsSub,
                supportsDolbyPassthrough = supportsDolby
            )
        }

        // Prioritize external outputs over built-in speaker as primary active route
        val primary = mapped.firstOrNull {
            it.connectionType != AudioConnectionType.BUILTIN_SPEAKER
        } ?: mapped.firstOrNull()

        val btGranted = hasBluetoothConnectPermission()
        val ancStatus = inspectAncCapabilities(mapped, btGranted)
        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)

        return DeviceCompatibilityReport(
            androidSdkInt = Build.VERSION.SDK_INT,
            androidVersionRelease = Build.VERSION.RELEASE ?: "Unknown",
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
            hasLowLatencyFeature = packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY),
            hasProAudioFeature = packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_PRO),
            hasDynamicsProcessingApi = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P,
            bluetoothPermissionGranted = btGranted,
            connectedOutputs = mapped,
            primaryOutput = primary,
            ancStatus = ancStatus,
            spatialStatus = spatialController.status.value,
            dolbyStatus = dolbyDetector.inspectDolbyCapabilities(),
            currentMusicVolume = currentVol,
            maxMusicVolume = maxVol
        )
    }

    @SuppressLint("MissingPermission")
    private fun inspectAncCapabilities(
        outputs: List<ConnectedAudioDeviceInfo>,
        btGranted: Boolean
    ): AncCapabilityStatus {
        val headphone = outputs.firstOrNull {
            it.connectionType == AudioConnectionType.BLUETOOTH_A2DP ||
                it.connectionType == AudioConnectionType.BLUETOOTH_LE_AUDIO ||
                it.connectionType == AudioConnectionType.WIRED_AUX ||
                it.connectionType == AudioConnectionType.USB_AUDIO
        }

        if (headphone == null) {
            return AncCapabilityStatus(
                connectedHeadphoneName = null,
                connectionType = null,
                hardwareAncDetected = false,
                transparencySupported = false,
                adaptiveAncSupported = false,
                oemSdkIntegrated = false,
                currentModeLabel = "No Headphones Connected",
                diagnosticNote = "Connect Bluetooth, USB-C, or wired headphones to inspect hardware ANC support. Hardware ANC is never simulated."
            )
        }

        var btDeviceName: String? = null
        var a2dpActive = false
        if (btGranted && (headphone.connectionType == AudioConnectionType.BLUETOOTH_A2DP ||
                headphone.connectionType == AudioConnectionType.BLUETOOTH_LE_AUDIO)
        ) {
            try {
                val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val adapter: BluetoothAdapter? = btManager?.adapter
                if (adapter != null && adapter.isEnabled) {
                    a2dpActive = adapter.getProfileConnectionState(BluetoothProfile.A2DP) ==
                        BluetoothProfile.STATE_CONNECTED
                    btDeviceName = headphone.name
                }
            } catch (_: Throwable) {
                // Ignore restricted BT adapter access
            }
        }

        val resolvedName = btDeviceName ?: headphone.name
        val oemVendorHint = detectKnownAncOem(resolvedName)

        val note = when {
            !btGranted && (headphone.connectionType == AudioConnectionType.BLUETOOTH_A2DP ||
                headphone.connectionType == AudioConnectionType.BLUETOOTH_LE_AUDIO) ->
                "Bluetooth permission not granted. Grant Bluetooth Connect permission to inspect headset ANC GATT/vendor capabilities."
            oemVendorHint != null ->
                "$oemVendorHint headset detected ($resolvedName). Android public SDKs do not expose universal third-party ANC toggles; vendor SDK/companion app is required for hardware ANC, Transparency, and Adaptive ANC."
            headphone.connectionType == AudioConnectionType.WIRED_AUX ->
                "Analog AUX headphones ($resolvedName) have no digital ANC control bus from Android."
            else ->
                "Connected ($resolvedName${if (a2dpActive) ", A2DP" else ""}). No proprietary OEM ANC SDK is bound to this headset; hardware ANC controls remain disabled rather than simulated."
        }

        return AncCapabilityStatus(
            connectedHeadphoneName = resolvedName,
            connectionType = headphone.connectionType,
            hardwareAncDetected = false,
            transparencySupported = false,
            adaptiveAncSupported = false,
            oemSdkIntegrated = false,
            currentModeLabel = "OEM SDK / Hardware Control Unavailable",
            diagnosticNote = note
        )
    }

    private fun detectKnownAncOem(name: String): String? {
        val lower = name.lowercase()
        return when {
            "sony" in lower || "wh-1000" in lower || "wf-1000" in lower -> "Sony"
            "bose" in lower || "quietcomfort" in lower -> "Bose"
            "airpods" in lower || "beats" in lower -> "Apple / Beats"
            "galaxy buds" in lower || "samsung" in lower -> "Samsung"
            "sennheiser" in lower || "momentum" in lower -> "Sennheiser"
            "soundcore" in lower || "anker" in lower -> "Anker Soundcore"
            "jbl" in lower -> "JBL"
            else -> null
        }
    }

    private fun mapConnectionType(deviceType: Int): AudioConnectionType {
        return when (deviceType) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> AudioConnectionType.BUILTIN_SPEAKER
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_AUX_LINE -> AudioConnectionType.WIRED_AUX
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_ACCESSORY -> AudioConnectionType.USB_AUDIO
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> AudioConnectionType.BLUETOOTH_A2DP
            AudioDeviceInfo.TYPE_HDMI -> AudioConnectionType.HDMI
            AudioDeviceInfo.TYPE_HDMI_ARC -> AudioConnectionType.HDMI_ARC
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    when (deviceType) {
                        AudioDeviceInfo.TYPE_BLE_HEADSET,
                        AudioDeviceInfo.TYPE_BLE_SPEAKER -> AudioConnectionType.BLUETOOTH_LE_AUDIO
                        AudioDeviceInfo.TYPE_HDMI_EARC -> AudioConnectionType.HDMI_EARC
                        else -> AudioConnectionType.UNKNOWN
                    }
                } else {
                    AudioConnectionType.UNKNOWN
                }
            }
        }
    }

    private fun encodingName(encoding: Int): String {
        return when (encoding) {
            AudioFormat.ENCODING_PCM_16BIT -> "PCM 16-bit"
            AudioFormat.ENCODING_PCM_FLOAT -> "PCM Float"
            AudioFormat.ENCODING_PCM_8BIT -> "PCM 8-bit"
            AudioFormat.ENCODING_AC3 -> "Dolby AC-3"
            AudioFormat.ENCODING_E_AC3 -> "Dolby E-AC-3"
            AudioFormat.ENCODING_DTS -> "DTS"
            AudioFormat.ENCODING_DTS_HD -> "DTS-HD"
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    when (encoding) {
                        AudioFormat.ENCODING_E_AC3_JOC -> "Dolby Atmos (E-AC-3 JOC)"
                        AudioFormat.ENCODING_AC4 -> "Dolby AC-4"
                        AudioFormat.ENCODING_DOLBY_TRUEHD -> "Dolby TrueHD"
                        else -> "Format #$encoding"
                    }
                } else {
                    "Format #$encoding"
                }
            }
        }
    }
}
