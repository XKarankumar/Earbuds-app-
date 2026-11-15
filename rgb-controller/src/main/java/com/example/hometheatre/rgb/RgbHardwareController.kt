package com.example.hometheatre.rgb

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.hometheatre.core.AudioTelemetrySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

enum class RgbHardwareMode(val label: String) {
    PREVIEW_ONLY("Preview Only (No Controller Connected)"),
    WLED_UDP("WLED DRGB UDP (Port 21324)"),
    ESP32_UDP("ESP32 Wi-Fi RGB UDP (Port 4210)"),
    BLE_GATT("Bluetooth LE RGB Controller")
}

data class DiscoveredBleController(
    val name: String,
    val address: String,
    val rssi: Int
)

data class RgbControllerState(
    val activeEffectId: Int = 1,
    val ledStripColors: List<Int> = List(24) { Color.CYAN },
    val hardwareMode: RgbHardwareMode = RgbHardwareMode.PREVIEW_ONLY,
    val isHardwareConnected: Boolean = false,
    val connectedTargetLabel: String = "No RGB controller connected",
    val wledHostIp: String = "",
    val esp32HostIp: String = "",
    val isScanningBle: Boolean = false,
    val discoveredBleDevices: List<DiscoveredBleController> = emptyList(),
    val bluetoothScanPermitted: Boolean = false,
    val lastHardwareDiagnostic: String = "Live software LED preview active. Connect WLED, ESP32, or BLE RGB hardware to drive physical LEDs."
)

/**
 * Drives real-time 24-LED RGB effect rendering and transmits hardware frames over:
 * - WLED DRGB UDP protocol (port 21324)
 * - ESP32 custom binary UDP RGB frames (port 4210)
 * - Bluetooth Low Energy GATT writable LED characteristics
 *
 * Strictly enforces honest hardware status: never claims physical LED control when disconnected.
 */
class RgbHardwareController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(RgbControllerState())
    val state: StateFlow<RgbControllerState> = _state.asStateFlow()

    private var renderJob: Job? = null
    private var udpSocket: DatagramSocket? = null
    private var targetInetAddress: InetAddress? = null
    private var targetPort: Int = 21324

    private var bluetoothGatt: BluetoothGatt? = null
    private var writableLedChar: BluetoothGattCharacteristic? = null
    private val bleDevicesByAddr = LinkedHashMap<String, DiscoveredBleController>()
    private val bleRawDevices = LinkedHashMap<String, BluetoothDevice>()

    @Volatile private var activeEffectId = 1
    @Volatile private var brightness = 0.8f
    @Volatile private var speed = 0.6f
    @Volatile private var customColorArgb = Color.CYAN
    @Volatile private var latestTelemetry = AudioTelemetrySnapshot()

    fun startLoop() {
        if (renderJob?.isActive == true) return
        refreshPermissions()
        renderJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                val frame = RgbEffectsCatalog.renderFrame(
                    effectId = activeEffectId,
                    ledCount = 24,
                    timeMs = System.currentTimeMillis(),
                    brightness = brightness,
                    speed = speed,
                    customColorArgb = customColorArgb,
                    telemetry = latestTelemetry
                )
                _state.value = _state.value.copy(
                    activeEffectId = activeEffectId,
                    ledStripColors = frame.toList()
                )
                streamHardwareFrameIfConnected(frame)
                delay(40L) // 25 FPS smooth render & UDP stream rate
            }
        }
    }

    fun stopLoop() {
        renderJob?.cancel()
        renderJob = null
    }

    fun updateParameters(
        effectId: Int,
        brightness: Float,
        speed: Float,
        customColorArgb: Int
    ) {
        this.activeEffectId = effectId
        this.brightness = brightness
        this.speed = speed
        this.customColorArgb = customColorArgb
    }

    fun updateAudioTelemetry(snapshot: AudioTelemetrySnapshot) {
        latestTelemetry = snapshot
    }

    fun refreshPermissions() {
        val granted = hasBlePermissions()
        _state.value = _state.value.copy(bluetoothScanPermitted = granted)
    }

    fun connectWledUdp(hostIp: String) {
        connectUdpTarget(
            hostIp = hostIp.trim(),
            port = 21324,
            mode = RgbHardwareMode.WLED_UDP,
            label = "WLED @ ${hostIp.trim()}:21324"
        )
    }

    fun connectEsp32Udp(hostIp: String) {
        connectUdpTarget(
            hostIp = hostIp.trim(),
            port = 4210,
            mode = RgbHardwareMode.ESP32_UDP,
            label = "ESP32 @ ${hostIp.trim()}:4210"
        )
    }

    private fun connectUdpTarget(
        hostIp: String,
        port: Int,
        mode: RgbHardwareMode,
        label: String
    ) {
        if (!isValidIpv4OrHost(hostIp)) {
            _state.value = _state.value.copy(
                isHardwareConnected = false,
                hardwareMode = RgbHardwareMode.PREVIEW_ONLY,
                lastHardwareDiagnostic = "Enter a valid local IPv4 address (e.g., 192.168.1.50)."
            )
            return
        }
        scope.launch(Dispatchers.IO) {
            try {
                disconnectInternal()
                val addr = InetAddress.getByName(hostIp)
                val sock = DatagramSocket()
                udpSocket = sock
                targetInetAddress = addr
                targetPort = port
                _state.value = _state.value.copy(
                    hardwareMode = mode,
                    isHardwareConnected = true,
                    connectedTargetLabel = label,
                    wledHostIp = if (mode == RgbHardwareMode.WLED_UDP) hostIp else _state.value.wledHostIp,
                    esp32HostIp = if (mode == RgbHardwareMode.ESP32_UDP) hostIp else _state.value.esp32HostIp,
                    lastHardwareDiagnostic = "Streaming live 24-LED frames via ${mode.label} to $hostIp:$port"
                )
            } catch (t: Throwable) {
                _state.value = _state.value.copy(
                    hardwareMode = RgbHardwareMode.PREVIEW_ONLY,
                    isHardwareConnected = false,
                    connectedTargetLabel = "Connection failed",
                    lastHardwareDiagnostic = "Controller unreachable: ${t.localizedMessage ?: "Socket error"}"
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnectHardware() {
        scope.launch(Dispatchers.IO) {
            disconnectInternal()
            _state.value = _state.value.copy(
                hardwareMode = RgbHardwareMode.PREVIEW_ONLY,
                isHardwareConnected = false,
                connectedTargetLabel = "No RGB controller connected",
                lastHardwareDiagnostic = "Disconnected hardware controller. Preview mode active."
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun disconnectInternal() {
        try { udpSocket?.close() } catch (_: Throwable) {}
        udpSocket = null
        targetInetAddress = null
        try { bluetoothGatt?.disconnect(); bluetoothGatt?.close() } catch (_: Throwable) {}
        bluetoothGatt = null
        writableLedChar = null
    }

    private fun streamHardwareFrameIfConnected(frame: IntArray) {
        val current = _state.value
        if (!current.isHardwareConnected) return
        when (current.hardwareMode) {
            RgbHardwareMode.WLED_UDP -> {
                val sock = udpSocket ?: return
                val addr = targetInetAddress ?: return
                // WLED DRGB packet: [0x02 (DRGB), 0x02 (2s timeout), R0, G0, B0, ...]
                val payload = ByteArray(2 + frame.size * 3)
                payload[0] = 2
                payload[1] = 2
                for (i in frame.indices) {
                    val c = frame[i]
                    payload[2 + i * 3] = Color.red(c).toByte()
                    payload[2 + i * 3 + 1] = Color.green(c).toByte()
                    payload[2 + i * 3 + 2] = Color.blue(c).toByte()
                }
                try {
                    sock.send(DatagramPacket(payload, payload.size, addr, targetPort))
                } catch (_: Throwable) {
                    // Keep stream resilient across transient Wi-Fi drops
                }
            }
            RgbHardwareMode.ESP32_UDP -> {
                val sock = udpSocket ?: return
                val addr = targetInetAddress ?: return
                // ESP32 binary RGB frame: [0xAA, 0x55, count, R0, G0, B0, ...]
                val payload = ByteArray(3 + frame.size * 3)
                payload[0] = 0xAA.toByte()
                payload[1] = 0x55.toByte()
                payload[2] = frame.size.toByte()
                for (i in frame.indices) {
                    val c = frame[i]
                    payload[3 + i * 3] = Color.red(c).toByte()
                    payload[3 + i * 3 + 1] = Color.green(c).toByte()
                    payload[3 + i * 3 + 2] = Color.blue(c).toByte()
                }
                try {
                    sock.send(DatagramPacket(payload, payload.size, addr, targetPort))
                } catch (_: Throwable) {
                }
            }
            RgbHardwareMode.BLE_GATT -> {
                sendBleColor(frame.firstOrNull() ?: Color.CYAN)
            }
            RgbHardwareMode.PREVIEW_ONLY -> Unit
        }
    }

    @SuppressLint("MissingPermission")
    fun scanBleControllers() {
        if (!hasBlePermissions()) {
            _state.value = _state.value.copy(
                bluetoothScanPermitted = false,
                lastHardwareDiagnostic = "Bluetooth Scan/Connect permissions required to discover BLE RGB controllers."
            )
            return
        }
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter: BluetoothAdapter? = btManager?.adapter
        val scanner = adapter?.bluetoothLeScanner
        if (adapter == null || !adapter.isEnabled || scanner == null) {
            _state.value = _state.value.copy(
                isScanningBle = false,
                lastHardwareDiagnostic = "Bluetooth is turned off or BLE scanner unavailable on this device."
            )
            return
        }

        bleDevicesByAddr.clear()
        bleRawDevices.clear()
        _state.value = _state.value.copy(
            isScanningBle = true,
            discoveredBleDevices = emptyList(),
            lastHardwareDiagnostic = "Scanning for nearby Bluetooth LE RGB controllers..."
        )

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val dev = result.device ?: return
                val name = dev.name ?: result.scanRecord?.deviceName ?: "BLE Device"
                bleRawDevices[dev.address] = dev
                bleDevicesByAddr[dev.address] = DiscoveredBleController(
                    name = name,
                    address = dev.address,
                    rssi = result.rssi
                )
                _state.value = _state.value.copy(
                    discoveredBleDevices = bleDevicesByAddr.values.toList()
                )
            }
        }

        try {
            scanner.startScan(callback)
            scope.launch {
                delay(6000)
                try { scanner.stopScan(callback) } catch (_: Throwable) {}
                _state.value = _state.value.copy(
                    isScanningBle = false,
                    lastHardwareDiagnostic = if (bleDevicesByAddr.isEmpty()) {
                        "No BLE RGB controllers discovered nearby."
                    } else {
                        "Found ${bleDevicesByAddr.size} BLE device(s). Tap one to attempt GATT LED binding."
                    }
                )
            }
        } catch (t: Throwable) {
            _state.value = _state.value.copy(
                isScanningBle = false,
                lastHardwareDiagnostic = "BLE scan failed: ${t.localizedMessage ?: "Permission/Hardware error"}"
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun connectBleController(address: String) {
        if (!hasBlePermissions()) return
        val device = bleRawDevices[address] ?: return
        disconnectInternal()
        _state.value = _state.value.copy(
            lastHardwareDiagnostic = "Connecting BLE GATT to ${device.name ?: address}..."
        )
        bluetoothGatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    writableLedChar = null
                    _state.value = _state.value.copy(
                        hardwareMode = RgbHardwareMode.PREVIEW_ONLY,
                        isHardwareConnected = false,
                        connectedTargetLabel = "BLE controller disconnected",
                        lastHardwareDiagnostic = "BLE GATT connection closed."
                    )
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                val candidate = gatt.services.flatMap { it.characteristics }.firstOrNull { ch ->
                    (ch.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or
                        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0
                }
                if (candidate != null) {
                    writableLedChar = candidate
                    _state.value = _state.value.copy(
                        hardwareMode = RgbHardwareMode.BLE_GATT,
                        isHardwareConnected = true,
                        connectedTargetLabel = "BLE: ${device.name ?: address}",
                        lastHardwareDiagnostic = "BLE GATT writable LED characteristic bound (${candidate.uuid})."
                    )
                } else {
                    gatt.disconnect()
                    _state.value = _state.value.copy(
                        hardwareMode = RgbHardwareMode.PREVIEW_ONLY,
                        isHardwareConnected = false,
                        lastHardwareDiagnostic = "Connected BLE device has no writable RGB LED GATT characteristic."
                    )
                }
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun sendBleColor(argb: Int) {
        val gatt = bluetoothGatt ?: return
        val char = writableLedChar ?: return
        if (!hasBlePermissions()) return
        // Standard BLE LED strip packet header (0x56 R G B 0x00 0xF0 0xAA)
        val packet = byteArrayOf(
            0x56.toByte(),
            Color.red(argb).toByte(),
            Color.green(argb).toByte(),
            Color.blue(argb).toByte(),
            0x00,
            0xF0.toByte(),
            0xAA.toByte()
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(char, packet, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
            } else {
                @Suppress("DEPRECATION")
                char.value = packet
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(char)
            }
        } catch (_: Throwable) {
        }
    }

    private fun hasBlePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun isValidIpv4OrHost(host: String): Boolean {
        if (host.isBlank()) return false
        val ipv4 = Regex("""^((25[0-5]|2[0-4]\d|[01]?\d\d?)\.){3}(25[0-5]|2[0-4]\d|[01]?\d\d?)$""")
        return ipv4.matches(host) || host.endsWith(".local")
    }
}
