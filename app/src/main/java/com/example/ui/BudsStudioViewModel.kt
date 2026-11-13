package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioEqualizerEngine
import com.example.bluetooth.BluetoothAudioManager
import com.example.bluetooth.FirmwareManager
import com.example.bluetooth.PermissionHelper
import com.example.data.StudioPreferencesRepository
import com.example.model.AncStrength
import com.example.model.AnimationStyle
import com.example.model.BluetoothConnectionState
import com.example.model.BudsStudioUiState
import com.example.model.ConnectionPopupEvent
import com.example.model.EarbudBrand
import com.example.model.EarbudControlsConfig
import com.example.model.EqPreset
import com.example.model.FirmwareCheckStage
import com.example.model.FirmwareInfoState
import com.example.model.GestureAction
import com.example.model.LayoutDensity
import com.example.model.NoiseControlMode
import com.example.model.NotificationStyle
import com.example.model.PopupStyle
import com.example.model.ThemePreference
import com.example.service.BluetoothMonitorService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BudsStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val prefsRepo = StudioPreferencesRepository(context)
    private val bluetoothManager = BluetoothAudioManager(context)
    private val equalizerEngine = AudioEqualizerEngine()
    private val firmwareManager = FirmwareManager(context)

    private val _firmwareOverride = MutableStateFlow<FirmwareInfoState?>(null)
    private val _popupEvent = MutableStateFlow<ConnectionPopupEvent?>(null)
    private val _uiState = MutableStateFlow(BudsStudioUiState())
    val uiState: StateFlow<BudsStudioUiState> = _uiState.asStateFlow()

    init {
        val eqStatus = equalizerEngine.initialize()
        _uiState.update {
            it.copy(
                isHardwareEqAttached = eqStatus.attached,
                hardwareEqStatus = eqStatus.message,
            )
        }
        bluetoothManager.start()
        refreshPermissions()

        viewModelScope.launch {
            bluetoothManager.connectionPopups.collect { event ->
                if (_uiState.value.customization.autoConnectionPopupEnabled) {
                    _popupEvent.value = event
                }
            }
        }

        viewModelScope.launch {
            combine(
                prefsRepo.prefsFlow,
                bluetoothManager.snapshot,
                _firmwareOverride,
                _popupEvent,
            ) { prefs, bt, fwOverride, popup ->
                val current = _uiState.value
                val checklist = PermissionHelper.buildChecklist(context)
                val allBtGranted = PermissionHelper.hasAllBluetoothPermissions(context)
                val hasNotif = PermissionHelper.hasNotificationPermission(context)

                val mergedFirmware = fwOverride?.copy(
                    installedVersion = bt.firmware.installedVersion ?: fwOverride.installedVersion,
                    hardwareRevision = bt.firmware.hardwareRevision ?: fwOverride.hardwareRevision,
                    softwareRevision = bt.firmware.softwareRevision ?: fwOverride.softwareRevision,
                    manufacturerName = bt.firmware.manufacturerName ?: fwOverride.manufacturerName,
                    modelNumber = bt.firmware.modelNumber ?: fwOverride.modelNumber,
                    hasStandardDfuService = bt.firmware.hasStandardDfuService,
                    discoveredServiceCount = bt.firmware.discoveredServiceCount,
                ) ?: bt.firmware

                current.copy(
                    connectionState = bt.connectionState,
                    connectedDeviceName = bt.connectedDeviceName,
                    connectedDeviceAddress = bt.connectedDeviceAddress,
                    connectedBrand = bt.connectedBrand,
                    firmware = mergedFirmware,
                    statusBanner = bt.statusMessage,
                    isScanning = bt.isScanning,
                    isBluetoothEnabled = bt.isBluetoothEnabled,
                    hasBluetoothHardware = bt.hasBluetoothHardware,
                    hasAllBluetoothPermissions = allBtGranted,
                    hasNotificationPermission = hasNotif,
                    permissionChecklist = checklist,
                    bondedDevices = bt.bondedDevices,
                    scannedDevices = bt.scannedDevices,
                    audioRouteInfo = bt.audioRouteInfo,
                    battery = bt.battery.copy(lowBatteryThreshold = prefs.lowBatteryThreshold),
                    activePopupEvent = popup,
                    customization = prefs.customization,
                    noiseControlMode = prefs.noiseControlMode,
                    ancStrength = prefs.ancStrength,
                    personalisedAnc = prefs.personalisedAnc,
                    windNoiseReduction = prefs.windNoiseReduction,
                    transparencyVoiceFocus = prefs.transparencyVoiceFocus,
                    dualConnection = prefs.dualConnection,
                    inEarDetection = prefs.inEarDetection,
                    lowLatencyMode = prefs.lowLatencyMode,
                    rememberedDeviceAddresses = prefs.rememberedDeviceAddresses,
                    eqPreset = prefs.eqPreset,
                    eqBandsDb = prefs.eqBandsDb,
                    bassBoostStrength = prefs.bassBoostStrength,
                    controls = prefs.controls,
                )
            }.collect { merged ->
                _uiState.value = merged
                if (merged.connectionState == BluetoothConnectionState.CONNECTED &&
                    merged.connectedDeviceAddress != null &&
                    merged.connectedDeviceName != null
                ) {
                    prefsRepo.recordConnectedDevice(
                        address = merged.connectedDeviceAddress,
                        name = merged.connectedDeviceName,
                    )
                }
                if (merged.connectionState != BluetoothConnectionState.CONNECTED && _firmwareOverride.value != null) {
                    _firmwareOverride.value = null
                }
                equalizerEngine.applyCurve(merged.eqBandsDb, merged.bassBoostStrength)
            }
        }
    }

    fun dismissPopup() {
        _popupEvent.value = null
    }

    fun previewConnectionPopup(brand: EarbudBrand = EarbudBrand.VIVO) {
        _popupEvent.value = ConnectionPopupEvent(
            deviceName = "${brand.displayName} Showcase Preview",
            address = null,
            brand = brand,
            isConnected = true,
            isPreview = true,
            batteryLevel = null,
            transportLabel = "White Liquid Glass Preview",
        )
    }

    fun refreshPermissions() {
        val checklist = PermissionHelper.buildChecklist(context)
        val allGranted = PermissionHelper.hasAllBluetoothPermissions(context)
        val hasNotif = PermissionHelper.hasNotificationPermission(context)
        _uiState.update {
            it.copy(
                hasAllBluetoothPermissions = allGranted,
                hasNotificationPermission = hasNotif,
                permissionChecklist = checklist,
            )
        }
        bluetoothManager.refreshState()

        if (_uiState.value.customization.backgroundMonitoringEnabled && allGranted) {
            BluetoothMonitorService.start(context)
        }
    }

    fun startBluetoothScan() {
        refreshPermissions()
        if (!_uiState.value.hasAllBluetoothPermissions) return
        bluetoothManager.startScan()
    }

    fun stopBluetoothScan() {
        bluetoothManager.stopScan()
    }

    fun connectDevice(address: String) {
        bluetoothManager.connectToDevice(address)
    }

    fun disconnectDevice() {
        bluetoothManager.disconnectCurrent()
    }

    fun checkOfficialFirmware() {
        val state = _uiState.value
        val isConnected = state.connectionState == BluetoothConnectionState.CONNECTED
        _firmwareOverride.value = state.firmware.copy(
            checkStage = FirmwareCheckStage.VERIFYING_GATT_AND_OEM,
            statusHeadline = "Verifying GATT & official channel…",
            statusDetail = "Checking Device Information Service (0x180A) and official ${state.connectedBrand.displayName} support portal.",
            checkProgress = 0.45f,
        )
        viewModelScope.launch {
            val result = firmwareManager.verifyOfficialFirmwareChannel(
                current = state.firmware,
                brand = state.connectedBrand,
                isConnected = isConnected,
                batteryLevel = state.battery.leftLevel,
            )
            _firmwareOverride.value = result
        }
    }

    fun setThemePreference(theme: ThemePreference) {
        viewModelScope.launch { prefsRepo.setThemePreference(theme) }
    }

    fun setPopupStyle(style: PopupStyle) {
        viewModelScope.launch { prefsRepo.setPopupStyle(style) }
    }

    fun setAnimationStyle(style: AnimationStyle) {
        viewModelScope.launch { prefsRepo.setAnimationStyle(style) }
    }

    fun setNotificationStyle(style: NotificationStyle) {
        viewModelScope.launch { prefsRepo.setNotificationStyle(style) }
    }

    fun setGlassTransparency(alpha: Float) {
        viewModelScope.launch { prefsRepo.setGlassTransparency(alpha) }
    }

    fun setGlassBlurDp(blurDp: Float) {
        viewModelScope.launch { prefsRepo.setGlassBlurDp(blurDp) }
    }

    fun setSilverReflection(intensity: Float) {
        viewModelScope.launch { prefsRepo.setSilverReflection(intensity) }
    }

    fun setCardCornerRadius(radiusDp: Float) {
        viewModelScope.launch { prefsRepo.setCardCornerRadius(radiusDp) }
    }

    fun setAnimationSpeed(multiplier: Float) {
        viewModelScope.launch { prefsRepo.setAnimationSpeed(multiplier) }
    }

    fun setEnable3dAutoOrbit(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setEnable3dAutoOrbit(enabled) }
    }

    fun setLayoutDensity(density: LayoutDensity) {
        viewModelScope.launch { prefsRepo.setLayoutDensity(density) }
    }

    fun setShowEqCardOnHome(show: Boolean) {
        viewModelScope.launch { prefsRepo.setShowEqCardOnHome(show) }
    }

    fun setAutoConnectionPopupEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setAutoConnectionPopupEnabled(enabled) }
    }

    fun setBackgroundMonitoringEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setBackgroundMonitoringEnabled(enabled)
            if (enabled && PermissionHelper.hasConnectPermission(context)) {
                BluetoothMonitorService.start(context)
            } else {
                BluetoothMonitorService.stop(context)
            }
        }
    }

    fun setLowBatteryThreshold(threshold: Int) {
        viewModelScope.launch { prefsRepo.setLowBatteryThreshold(threshold) }
    }

    fun setNoiseControlMode(mode: NoiseControlMode) {
        viewModelScope.launch { prefsRepo.setNoiseMode(mode) }
    }

    fun setAncStrength(strength: AncStrength) {
        viewModelScope.launch { prefsRepo.setAncStrength(strength) }
    }

    fun setPersonalisedAnc(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setPersonalisedAnc(enabled) }
    }

    fun setWindNoiseReduction(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setWindReduction(enabled) }
    }

    fun setTransparencyVoiceFocus(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setTransparencyVoiceFocus(enabled) }
    }

    fun setDualConnection(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setDualConnection(enabled) }
    }

    fun setInEarDetection(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setInEarDetection(enabled) }
    }

    fun setLowLatencyMode(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setLowLatencyMode(enabled) }
    }

    fun selectEqPreset(preset: EqPreset) {
        viewModelScope.launch {
            val bands = if (preset == EqPreset.CUSTOM) _uiState.value.eqBandsDb else preset.bandsDb
            val status = equalizerEngine.applyCurve(bands, _uiState.value.bassBoostStrength)
            _uiState.update {
                it.copy(
                    isHardwareEqAttached = status.attached,
                    hardwareEqStatus = status.message,
                )
            }
            prefsRepo.setEqualizer(preset, bands, _uiState.value.bassBoostStrength)
        }
    }

    fun updateEqBand(index: Int, valueDb: Float) {
        if (index !in 0..4) return
        val updated = _uiState.value.eqBandsDb.toMutableList().apply {
            this[index] = valueDb.coerceIn(-12f, 12f)
        }
        viewModelScope.launch {
            val status = equalizerEngine.applyCurve(updated, _uiState.value.bassBoostStrength)
            _uiState.update {
                it.copy(
                    isHardwareEqAttached = status.attached,
                    hardwareEqStatus = status.message,
                )
            }
            prefsRepo.setEqualizer(EqPreset.CUSTOM, updated, _uiState.value.bassBoostStrength)
        }
    }

    fun updateBassBoost(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        viewModelScope.launch {
            val status = equalizerEngine.applyCurve(_uiState.value.eqBandsDb, clamped)
            _uiState.update {
                it.copy(
                    isHardwareEqAttached = status.attached,
                    hardwareEqStatus = status.message,
                )
            }
            prefsRepo.setEqualizer(_uiState.value.eqPreset, _uiState.value.eqBandsDb, clamped)
        }
    }

    fun updateControls(transform: (EarbudControlsConfig) -> EarbudControlsConfig) {
        val updated = transform(_uiState.value.controls)
        viewModelScope.launch { prefsRepo.setControls(updated) }
    }

    fun setDoubleTapLeft(action: GestureAction) = updateControls { it.copy(doubleTapLeft = action) }
    fun setDoubleTapRight(action: GestureAction) = updateControls { it.copy(doubleTapRight = action) }
    fun setTripleTap(action: GestureAction) = updateControls { it.copy(tripleTap = action) }
    fun setPressAndHold(action: GestureAction) = updateControls { it.copy(pressAndHold = action) }

    override fun onCleared() {
        bluetoothManager.release()
        equalizerEngine.release()
        super.onCleared()
    }
}
