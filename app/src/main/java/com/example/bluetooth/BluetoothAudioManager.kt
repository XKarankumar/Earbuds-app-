package com.example.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import com.example.model.AudioRouteInfo
import com.example.model.BatteryTelemetry
import com.example.model.BluetoothConnectionState
import com.example.model.ConnectionPopupEvent
import com.example.model.DiscoveredBluetoothDevice
import com.example.model.EarbudBrand
import com.example.model.FirmwareInfoState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.ArrayDeque
import java.util.UUID

data class BluetoothEngineSnapshot(
    val hasBluetoothHardware: Boolean = true,
    val isBluetoothEnabled: Boolean = false,
    val connectionState: BluetoothConnectionState = BluetoothConnectionState.DISCONNECTED,
    val connectedDeviceName: String? = null,
    val connectedDeviceAddress: String? = null,
    val connectedBrand: EarbudBrand = EarbudBrand.GENERIC,
    val firmware: FirmwareInfoState = FirmwareInfoState(),
    val statusMessage: String = "Ready · Turn on Bluetooth to pair earbuds",
    val isScanning: Boolean = false,
    val bondedDevices: List<DiscoveredBluetoothDevice> = emptyList(),
    val scannedDevices: List<DiscoveredBluetoothDevice> = emptyList(),
    val battery: BatteryTelemetry = BatteryTelemetry(),
    val audioRouteInfo: AudioRouteInfo = AudioRouteInfo(),
)

class BluetoothAudioManager(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val bluetoothManager: BluetoothManager? =
        appContext.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val audioManager: AudioManager? =
        appContext.getSystemService(AudioManager::class.java)

    private val _snapshot = MutableStateFlow(BluetoothEngineSnapshot())
    val snapshot: StateFlow<BluetoothEngineSnapshot> = _snapshot.asStateFlow()

    private val _connectionPopups = MutableSharedFlow<ConnectionPopupEvent>(extraBufferCapacity = 4)
    val connectionPopups: SharedFlow<ConnectionPopupEvent> = _connectionPopups.asSharedFlow()

    private var a2dpProxy: BluetoothA2dp? = null
    private var headsetProxy: BluetoothHeadset? = null
    private var activeGatt: BluetoothGatt? = null
    private val gattReadQueue = ArrayDeque<BluetoothGattCharacteristic>()
    private var scanTimeoutJob: Job? = null
    private var isStarted = false
    private val discoveredMap = linkedMapOf<String, DiscoveredBluetoothDevice>()
    private var lastKnownConnectedKey: String? = null

    companion object {
        private const val TAG = "BluetoothAudioManager"
        private const val ACTION_BATTERY_LEVEL_CHANGED =
            "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        private const val EXTRA_BATTERY_LEVEL =
            "android.bluetooth.device.extra.BATTERY_LEVEL"

        private val BATTERY_SERVICE_UUID: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        private val BATTERY_LEVEL_CHAR_UUID: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        private val DEVICE_INFO_SERVICE_UUID: UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")
        private val MODEL_NUM_CHAR_UUID: UUID = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")
        private val FIRMWARE_REV_CHAR_UUID: UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")
        private val HARDWARE_REV_CHAR_UUID: UUID = UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb")
        private val SOFTWARE_REV_CHAR_UUID: UUID = UUID.fromString("00002a28-0000-1000-8000-00805f9b34fb")
        private val MANUFACTURER_NAME_CHAR_UUID: UUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")
        private val NORDIC_DFU_SERVICE_UUID: UUID = UUID.fromString("0000fe59-0000-1000-8000-00805f9b34fb")
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            when (profile) {
                BluetoothProfile.A2DP -> a2dpProxy = proxy as? BluetoothA2dp
                BluetoothProfile.HEADSET -> headsetProxy = proxy as? BluetoothHeadset
            }
            refreshState()
        }

        override fun onServiceDisconnected(profile: Int) {
            when (profile) {
                BluetoothProfile.A2DP -> a2dpProxy = null
                BluetoothProfile.HEADSET -> headsetProxy = null
            }
            refreshState()
        }
    }

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            refreshAudioRoute()
            refreshState()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            refreshAudioRoute()
            refreshState()
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            when (action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> refreshState()
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> _snapshot.update { it.copy(isScanning = true) }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> _snapshot.update { it.copy(isScanning = false) }
                BluetoothDevice.ACTION_FOUND -> {
                    if (!PermissionHelper.hasConnectPermission(appContext)) return
                    val device = IntentCompat.getParcelableExtra(
                        intent,
                        BluetoothDevice.EXTRA_DEVICE,
                        BluetoothDevice::class.java,
                    ) ?: return
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                        .takeIf { it != Short.MIN_VALUE }?.toInt()
                    recordDiscoveredDevice(device, rssi, null)
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device = IntentCompat.getParcelableExtra(
                        intent,
                        BluetoothDevice.EXTRA_DEVICE,
                        BluetoothDevice::class.java,
                    )
                    val bondState = intent.getIntExtra(
                        BluetoothDevice.EXTRA_BOND_STATE,
                        BluetoothDevice.BOND_NONE,
                    )
                    if (bondState == BluetoothDevice.BOND_BONDED && device != null) {
                        connectGatt(device)
                    }
                    refreshState()
                }
                BluetoothDevice.ACTION_ACL_CONNECTED,
                BluetoothDevice.ACTION_ACL_DISCONNECTED,
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                    refreshAudioRoute()
                    refreshState(emitPopupOnChange = true)
                }
                ACTION_BATTERY_LEVEL_CHANGED -> {
                    val level = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1)
                    if (level in 0..100) {
                        _snapshot.update { current ->
                            current.copy(
                                battery = current.battery.copy(
                                    leftLevel = level,
                                    isUnifiedSingleReport = true,
                                    sourceLabel = "Unified Bluetooth Battery ($level% · Android BT)",
                                ),
                            )
                        }
                    }
                }
                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                    refreshAudioRoute()
                    refreshState(emitPopupOnChange = true)
                }
            }
        }
    }

    private val bleScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (!PermissionHelper.hasConnectPermission(appContext)) return
            recordDiscoveredDevice(result.device, result.rssi, result.scanRecord)
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "BLE scan failed with code=$errorCode")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                val deviceName = safeDeviceName(gatt.device)
                val brand = BrandRecognizer.identify(deviceName, address = gatt.device.address)
                _snapshot.update {
                    it.copy(
                        connectionState = BluetoothConnectionState.CONNECTED,
                        connectedDeviceName = deviceName,
                        connectedDeviceAddress = gatt.device.address,
                        connectedBrand = brand,
                        statusMessage = "Connected to $deviceName · Syncing GATT services",
                    )
                }
                if (PermissionHelper.hasConnectPermission(appContext)) {
                    runCatching { gatt.discoverServices() }
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (activeGatt == gatt) {
                    runCatching { gatt.close() }
                    activeGatt = null
                }
                refreshState(emitPopupOnChange = true)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS || !PermissionHelper.hasConnectPermission(appContext)) {
                return
            }
            val services = gatt.services.orEmpty()
            val hasDfu = services.any { it.uuid == NORDIC_DFU_SERVICE_UUID }
            _snapshot.update { current ->
                current.copy(
                    firmware = current.firmware.copy(
                        hasStandardDfuService = hasDfu,
                        discoveredServiceCount = services.size,
                    ),
                )
            }

            gattReadQueue.clear()
            gatt.getService(BATTERY_SERVICE_UUID)
                ?.getCharacteristic(BATTERY_LEVEL_CHAR_UUID)
                ?.let { gattReadQueue.add(it) }

            val dis = gatt.getService(DEVICE_INFO_SERVICE_UUID)
            dis?.getCharacteristic(MANUFACTURER_NAME_CHAR_UUID)?.let { gattReadQueue.add(it) }
            dis?.getCharacteristic(MODEL_NUM_CHAR_UUID)?.let { gattReadQueue.add(it) }
            dis?.getCharacteristic(FIRMWARE_REV_CHAR_UUID)?.let { gattReadQueue.add(it) }
            dis?.getCharacteristic(HARDWARE_REV_CHAR_UUID)?.let { gattReadQueue.add(it) }
            dis?.getCharacteristic(SOFTWARE_REV_CHAR_UUID)?.let { gattReadQueue.add(it) }

            if (gattReadQueue.isEmpty()) {
                val reflectedLevel = readDeviceBatteryReflection(gatt.device)
                _snapshot.update { current ->
                    current.copy(
                        battery = if (reflectedLevel != null) {
                            current.battery.copy(
                                leftLevel = reflectedLevel,
                                isUnifiedSingleReport = true,
                                sourceLabel = "Unified Bluetooth Battery ($reflectedLevel% · Android BT)",
                            )
                        } else {
                            current.battery.copy(
                                sourceLabel = "GATT connected · Standard Battery Service not exposed",
                            )
                        },
                        statusMessage = "Connected to ${safeDeviceName(gatt.device)}",
                    )
                }
            } else {
                drainNextGattRead(gatt)
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int,
        ) {
            handleCharacteristicRead(gatt, characteristic, value, status)
        }

        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            @Suppress("DEPRECATION")
            val value = characteristic.value ?: ByteArray(0)
            handleCharacteristicRead(gatt, characteristic, value, status)
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int,
    ) {
        if (status == BluetoothGatt.GATT_SUCCESS && value.isNotEmpty()) {
            val text = value.decodeToString().trim().takeIf { it.isNotEmpty() }
            when (characteristic.uuid) {
                BATTERY_LEVEL_CHAR_UUID -> {
                    val level = (value[0].toInt() and 0xFF).coerceIn(0, 100)
                    _snapshot.update { current ->
                        current.copy(
                            battery = current.battery.copy(
                                leftLevel = level,
                                isUnifiedSingleReport = true,
                                sourceLabel = "GATT Battery Service 0x180F ($level% unified)",
                            ),
                            statusMessage = "Connected to ${safeDeviceName(gatt.device)} · Battery $level%",
                        )
                    }
                }
                MANUFACTURER_NAME_CHAR_UUID -> if (text != null) {
                    _snapshot.update { current ->
                        val brand = BrandRecognizer.identify(
                            current.connectedDeviceName,
                            gattManufacturerName = text,
                            address = current.connectedDeviceAddress,
                        )
                        current.copy(
                            connectedBrand = brand,
                            firmware = current.firmware.copy(manufacturerName = text),
                        )
                    }
                }
                MODEL_NUM_CHAR_UUID -> if (text != null) {
                    _snapshot.update { it.copy(firmware = it.firmware.copy(modelNumber = text)) }
                }
                FIRMWARE_REV_CHAR_UUID -> if (text != null) {
                    _snapshot.update { it.copy(firmware = it.firmware.copy(installedVersion = text)) }
                }
                HARDWARE_REV_CHAR_UUID -> if (text != null) {
                    _snapshot.update { it.copy(firmware = it.firmware.copy(hardwareRevision = text)) }
                }
                SOFTWARE_REV_CHAR_UUID -> if (text != null) {
                    _snapshot.update { it.copy(firmware = it.firmware.copy(softwareRevision = text)) }
                }
            }
        }
        drainNextGattRead(gatt)
    }

    @SuppressLint("MissingPermission")
    private fun drainNextGattRead(gatt: BluetoothGatt) {
        if (!PermissionHelper.hasConnectPermission(appContext)) return
        val next = gattReadQueue.pollFirst() ?: return
        runCatching { gatt.readCharacteristic(next) }
    }

    fun start() {
        if (isStarted) {
            refreshState()
            return
        }
        isStarted = true

        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            addAction(ACTION_BATTERY_LEVEL_CHANGED)
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        }
        ContextCompat.registerReceiver(
            appContext,
            bluetoothReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )

        audioManager?.registerAudioDeviceCallback(audioDeviceCallback, null)
        bindProfileProxies()
        refreshAudioRoute()
        refreshState(emitPopupOnChange = false)
    }

    private fun bindProfileProxies() {
        val adapter = bluetoothAdapter ?: return
        if (!PermissionHelper.hasConnectPermission(appContext)) return
        runCatching {
            adapter.getProfileProxy(appContext, profileListener, BluetoothProfile.A2DP)
            adapter.getProfileProxy(appContext, profileListener, BluetoothProfile.HEADSET)
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshState(emitPopupOnChange: Boolean = false) {
        val adapter = bluetoothAdapter
        if (adapter == null) {
            _snapshot.update {
                it.copy(
                    hasBluetoothHardware = false,
                    isBluetoothEnabled = false,
                    connectionState = BluetoothConnectionState.UNAVAILABLE,
                    statusMessage = "Bluetooth hardware unavailable on this device",
                )
            }
            return
        }

        val hasPerms = PermissionHelper.hasAllBluetoothPermissions(appContext)
        val isBtOn = runCatching { adapter.isEnabled }.getOrDefault(false)

        if (!hasPerms) {
            refreshAudioRoute()
            _snapshot.update {
                it.copy(
                    hasBluetoothHardware = true,
                    isBluetoothEnabled = isBtOn,
                    connectionState = BluetoothConnectionState.PERMISSION_REQUIRED,
                    statusMessage = "Bluetooth permissions needed to detect earbuds & battery",
                )
            }
            return
        }

        if (a2dpProxy == null || headsetProxy == null) {
            bindProfileProxies()
        }

        if (!isBtOn) {
            refreshAudioRoute()
            _snapshot.update {
                it.copy(
                    hasBluetoothHardware = true,
                    isBluetoothEnabled = false,
                    connectionState = BluetoothConnectionState.BLUETOOTH_OFF,
                    connectedDeviceName = null,
                    connectedDeviceAddress = null,
                    connectedBrand = EarbudBrand.GENERIC,
                    bondedDevices = emptyList(),
                    battery = BatteryTelemetry(sourceLabel = "Bluetooth is turned off"),
                    statusMessage = "Bluetooth is off · Turn on Bluetooth to connect earbuds",
                )
            }
            return
        }

        val activeA2dp = runCatching { a2dpProxy?.connectedDevices.orEmpty() }.getOrDefault(emptyList())
        val activeHfp = runCatching { headsetProxy?.connectedDevices.orEmpty() }.getOrDefault(emptyList())
        val activeAudioDevice = activeA2dp.firstOrNull() ?: activeHfp.firstOrNull()
        val activeGattDevice = activeGatt?.let { gatt -> runCatching { gatt.device }.getOrNull() }

        val connectedDevice = activeAudioDevice ?: activeGattDevice
        val bonded = runCatching { adapter.bondedDevices.orEmpty() }.getOrDefault(emptySet())
            .map { dev ->
                val isConn = (connectedDevice?.address == dev.address) ||
                    activeA2dp.any { it.address == dev.address } ||
                    activeHfp.any { it.address == dev.address }
                toDiscoveredModel(dev, rssi = null, scanRecord = null, isBonded = true, isConnected = isConn)
            }
            .sortedWith(
                compareByDescending<DiscoveredBluetoothDevice> { it.isConnected }
                    .thenByDescending { it.isAudioDevice }
                    .thenBy { it.name.lowercase() },
            )

        refreshAudioRoute()
        val route = _snapshot.value.audioRouteInfo

        val resolvedName = connectedDevice?.let { safeDeviceName(it) } ?: route.deviceName
        val resolvedAddress = connectedDevice?.address
        val isActuallyConnected = connectedDevice != null || route.isBluetoothAudioActive
        val resolvedBrand = BrandRecognizer.identify(
            deviceName = resolvedName,
            gattManufacturerName = _snapshot.value.firmware.manufacturerName,
            address = resolvedAddress,
        )

        val reflectedLevel = connectedDevice?.let { readDeviceBatteryReflection(it) }

        val prevKey = lastKnownConnectedKey
        val nextKey = if (isActuallyConnected) (resolvedAddress ?: resolvedName) else null
        if (emitPopupOnChange && prevKey != nextKey) {
            if (nextKey != null && resolvedName != null) {
                _connectionPopups.tryEmit(
                    ConnectionPopupEvent(
                        deviceName = resolvedName,
                        address = resolvedAddress,
                        brand = resolvedBrand,
                        isConnected = true,
                        batteryLevel = reflectedLevel ?: _snapshot.value.battery.leftLevel,
                        transportLabel = route.transportType,
                    ),
                )
            } else if (prevKey != null) {
                _connectionPopups.tryEmit(
                    ConnectionPopupEvent(
                        deviceName = _snapshot.value.connectedDeviceName ?: "Earbuds",
                        address = _snapshot.value.connectedDeviceAddress,
                        brand = _snapshot.value.connectedBrand,
                        isConnected = false,
                        transportLabel = "Disconnected",
                    ),
                )
            }
        }
        lastKnownConnectedKey = nextKey

        _snapshot.update { current ->
            val updatedBattery = if (isActuallyConnected) {
                val lvl = current.battery.leftLevel ?: reflectedLevel
                current.battery.copy(
                    leftLevel = lvl,
                    isUnifiedSingleReport = lvl != null && current.battery.rightLevel == null,
                    sourceLabel = when {
                        lvl != null -> "Unified Bluetooth Battery ($lvl% · Android BT)"
                        else -> "Connected · Standard battery level not exposed"
                    },
                )
            } else {
                BatteryTelemetry(sourceLabel = "Not connected · Connect earbuds to read battery")
            }

            current.copy(
                hasBluetoothHardware = true,
                isBluetoothEnabled = true,
                bondedDevices = bonded,
                connectedDeviceName = resolvedName,
                connectedDeviceAddress = resolvedAddress,
                connectedBrand = resolvedBrand,
                connectionState = when {
                    isActuallyConnected -> BluetoothConnectionState.CONNECTED
                    current.isScanning -> BluetoothConnectionState.SCANNING
                    else -> BluetoothConnectionState.DISCONNECTED
                },
                battery = updatedBattery,
                firmware = if (isActuallyConnected) current.firmware else FirmwareInfoState(),
                statusMessage = when {
                    isActuallyConnected -> "Connected · ${resolvedBrand.displayName} · ${resolvedName ?: "Audio"}"
                    current.isScanning -> "Scanning for nearby Bluetooth audio devices…"
                    bonded.isNotEmpty() -> "Ready · ${bonded.size} paired device(s) available"
                    else -> "Not connected · Scan or pair earbuds in Bluetooth Manager"
                },
            )
        }
    }

    private fun refreshAudioRoute() {
        val manager = audioManager ?: return
        val outputs = runCatching { manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) }.getOrNull()
            ?: return

        val btDevice = outputs.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        }

        val routeInfo = if (btDevice != null) {
            val transport = when (btDevice.type) {
                AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER -> "LE Audio (BLE)"
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth A2DP"
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth HFP/SCO"
                else -> "Bluetooth Audio"
            }
            AudioRouteInfo(
                deviceName = btDevice.productName?.toString()?.takeIf { it.isNotBlank() },
                transportType = transport,
                sampleRateHz = btDevice.sampleRates.maxOrNull(),
                channelCount = btDevice.channelCounts.maxOrNull(),
                codecLabel = when (btDevice.type) {
                    AudioDeviceInfo.TYPE_BLE_HEADSET -> "LC3 / LE Audio"
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "A2DP Stereo"
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Voice SCO"
                    else -> "Bluetooth"
                },
                isBluetoothAudioActive = true,
            )
        } else {
            AudioRouteInfo(
                deviceName = null,
                transportType = "Device Speaker",
                sampleRateHz = null,
                channelCount = null,
                codecLabel = "Standby",
                isBluetoothAudioActive = false,
            )
        }

        _snapshot.update { it.copy(audioRouteInfo = routeInfo) }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        val adapter = bluetoothAdapter ?: return
        if (!PermissionHelper.hasScanPermission(appContext) || !PermissionHelper.hasConnectPermission(appContext)) {
            refreshState()
            return
        }
        if (!adapter.isEnabled) {
            refreshState()
            return
        }

        stopScan()
        discoveredMap.clear()
        _snapshot.update {
            it.copy(
                isScanning = true,
                scannedDevices = emptyList(),
                connectionState = if (it.connectionState == BluetoothConnectionState.CONNECTED) {
                    BluetoothConnectionState.CONNECTED
                } else {
                    BluetoothConnectionState.SCANNING
                },
                statusMessage = "Scanning nearby Bluetooth & BLE audio devices…",
            )
        }

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        runCatching { adapter.bluetoothLeScanner?.startScan(null, scanSettings, bleScanCallback) }
        runCatching { adapter.startDiscovery() }

        scanTimeoutJob = scope.launch {
            delay(12_000L)
            stopScan()
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = null
        val adapter = bluetoothAdapter ?: return
        if (PermissionHelper.hasScanPermission(appContext)) {
            runCatching { adapter.bluetoothLeScanner?.stopScan(bleScanCallback) }
            runCatching { if (adapter.isDiscovering) adapter.cancelDiscovery() }
        }
        _snapshot.update { it.copy(isScanning = false) }
        refreshState()
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(address: String) {
        val adapter = bluetoothAdapter ?: return
        if (!PermissionHelper.hasConnectPermission(appContext)) {
            refreshState()
            return
        }
        stopScan()
        val device = runCatching { adapter.getRemoteDevice(address) }.getOrNull() ?: return
        val name = safeDeviceName(device)
        _snapshot.update {
            it.copy(
                connectionState = BluetoothConnectionState.CONNECTING,
                statusMessage = "Connecting to $name…",
            )
        }

        if (device.bondState == BluetoothDevice.BOND_NONE) {
            val startedBond = runCatching { device.createBond() }.getOrDefault(false)
            if (!startedBond) {
                PermissionHelper.openBluetoothSettings(appContext)
            }
        }
        connectGatt(device)
    }

    @SuppressLint("MissingPermission")
    private fun connectGatt(device: BluetoothDevice) {
        if (!PermissionHelper.hasConnectPermission(appContext)) return
        runCatching {
            activeGatt?.disconnect()
            activeGatt?.close()
        }
        activeGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(appContext, false, gattCallback, BluetoothDevice.TRANSPORT_AUTO)
        } else {
            device.connectGatt(appContext, false, gattCallback)
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnectCurrent() {
        if (PermissionHelper.hasConnectPermission(appContext)) {
            runCatching {
                activeGatt?.disconnect()
                activeGatt?.close()
            }
        }
        activeGatt = null
        _snapshot.update {
            it.copy(
                connectedDeviceName = null,
                connectedDeviceAddress = null,
                connectedBrand = EarbudBrand.GENERIC,
                firmware = FirmwareInfoState(),
                battery = BatteryTelemetry(sourceLabel = "Disconnected"),
                connectionState = BluetoothConnectionState.DISCONNECTED,
                statusMessage = "GATT session closed · System Bluetooth controls A2DP audio",
            )
        }
        refreshState()
    }

    @SuppressLint("MissingPermission")
    private fun recordDiscoveredDevice(
        device: BluetoothDevice,
        rssi: Int?,
        scanRecord: ScanRecord?,
    ) {
        val model = toDiscoveredModel(
            device = device,
            rssi = rssi,
            scanRecord = scanRecord,
            isBonded = device.bondState == BluetoothDevice.BOND_BONDED,
            isConnected = _snapshot.value.connectedDeviceAddress == device.address,
        )
        discoveredMap[model.address] = model
        val sorted = discoveredMap.values
            .sortedWith(
                compareByDescending<DiscoveredBluetoothDevice> { it.isAudioDevice }
                    .thenByDescending { it.brand != EarbudBrand.GENERIC }
                    .thenByDescending { it.name != "Unnamed Device" }
                    .thenByDescending { it.rssi ?: Int.MIN_VALUE },
            )
        _snapshot.update { it.copy(scannedDevices = sorted) }
    }

    @SuppressLint("MissingPermission")
    private fun toDiscoveredModel(
        device: BluetoothDevice,
        rssi: Int?,
        scanRecord: ScanRecord?,
        isBonded: Boolean,
        isConnected: Boolean,
    ): DiscoveredBluetoothDevice {
        val name = safeDeviceName(device)
        val brand = BrandRecognizer.identify(name, scanRecord = scanRecord, address = device.address)
        val majorClass = runCatching { device.bluetoothClass?.majorDeviceClass }.getOrNull()
        val isAudioClass = majorClass == BluetoothClass.Device.Major.AUDIO_VIDEO
        val isAudio = isAudioClass || brand != EarbudBrand.GENERIC || BrandRecognizer.isLikelyAudioName(name)
        val typeLabel = when {
            isAudio && brand != EarbudBrand.GENERIC -> "${brand.displayName} Audio"
            isAudio -> "Audio / Earbuds"
            device.type == BluetoothDevice.DEVICE_TYPE_LE -> "BLE Device"
            device.type == BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual-Mode BT"
            else -> "Bluetooth"
        }
        return DiscoveredBluetoothDevice(
            name = name,
            address = device.address,
            brand = brand,
            rssi = rssi,
            isBonded = isBonded,
            isAudioDevice = isAudio,
            isConnected = isConnected,
            deviceTypeLabel = typeLabel,
        )
    }

    @SuppressLint("MissingPermission")
    private fun safeDeviceName(device: BluetoothDevice): String {
        if (!PermissionHelper.hasConnectPermission(appContext)) return "Bluetooth Device"
        return runCatching { device.name }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Unnamed Device"
    }

    private fun readDeviceBatteryReflection(device: BluetoothDevice): Int? {
        return try {
            val method = device.javaClass.getMethod("getBatteryLevel")
            (method.invoke(device) as? Int)?.takeIf { it in 0..100 }
        } catch (_: Throwable) {
            null
        }
    }

    fun release() {
        stopScan()
        if (isStarted) {
            runCatching { appContext.unregisterReceiver(bluetoothReceiver) }
            runCatching { audioManager?.unregisterAudioDeviceCallback(audioDeviceCallback) }
            isStarted = false
        }
        val adapter = bluetoothAdapter
        if (adapter != null) {
            a2dpProxy?.let { runCatching { adapter.closeProfileProxy(BluetoothProfile.A2DP, it) } }
            headsetProxy?.let { runCatching { adapter.closeProfileProxy(BluetoothProfile.HEADSET, it) } }
        }
        runCatching {
            if (PermissionHelper.hasConnectPermission(appContext)) {
                activeGatt?.disconnect()
                activeGatt?.close()
            }
        }
        activeGatt = null
        scope.cancel()
    }
}
