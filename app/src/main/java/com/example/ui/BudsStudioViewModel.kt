package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioEqualizerEngine
import com.example.bluetooth.BluetoothAudioManager
import com.example.bluetooth.PermissionHelper
import com.example.data.StudioPreferencesRepository
import com.example.model.AncStrength
import com.example.model.BudsStudioUiState
import com.example.model.EarbudControlsConfig
import com.example.model.EqPreset
import com.example.model.GestureAction
import com.example.model.GlassSubMenu
import com.example.model.NoiseControlMode
import com.example.model.StudioTab
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
            combine(
                prefsRepo.prefsFlow,
                bluetoothManager.snapshot,
            ) { prefs, bt ->
                val current = _uiState.value
                val checklist = PermissionHelper.buildChecklist(context)
                val allGranted = PermissionHelper.hasAllBluetoothPermissions(context)
                current.copy(
                    connectionState = bt.connectionState,
                    connectedDeviceName = bt.connectedDeviceName ?: prefs.lastDeviceName,
                    connectedDeviceAddress = bt.connectedDeviceAddress ?: prefs.lastDeviceAddress,
                    firmwareVersion = bt.firmwareVersion,
                    statusBanner = bt.statusMessage,
                    isScanning = bt.isScanning,
                    isBluetoothEnabled = bt.isBluetoothEnabled,
                    hasBluetoothHardware = bt.hasBluetoothHardware,
                    hasAllPermissions = allGranted,
                    permissionChecklist = checklist,
                    bondedDevices = bt.bondedDevices,
                    scannedDevices = bt.scannedDevices,
                    audioRouteInfo = bt.audioRouteInfo,
                    battery = bt.battery,
                    noiseControlMode = prefs.noiseControlMode,
                    ancStrength = prefs.ancStrength,
                    personalisedAnc = prefs.personalisedAnc,
                    windNoiseReduction = prefs.windNoiseReduction,
                    transparencyVoiceFocus = prefs.transparencyVoiceFocus,
                    dualConnection = prefs.dualConnection,
                    inEarDetection = prefs.inEarDetection,
                    lowLatencyMode = prefs.lowLatencyMode,
                    rememberedDeviceAddresses = prefs.rememberedDeviceAddresses,
                    autoReconnectEnabled = prefs.autoReconnectEnabled,
                    eqPreset = prefs.eqPreset,
                    eqBandsDb = prefs.eqBandsDb,
                    bassBoostStrength = prefs.bassBoostStrength,
                    controls = prefs.controls,
                )
            }.collect { merged ->
                _uiState.value = merged
                if (merged.connectionState == com.example.model.BluetoothConnectionState.CONNECTED &&
                    merged.connectedDeviceAddress != null &&
                    merged.connectedDeviceName != null
                ) {
                    prefsRepo.recordConnectedDevice(
                        address = merged.connectedDeviceAddress,
                        name = merged.connectedDeviceName,
                    )
                }
                equalizerEngine.applyCurve(merged.eqBandsDb, merged.bassBoostStrength)
            }
        }
    }

    fun selectTab(tab: StudioTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun openSubMenu(subMenu: GlassSubMenu) {
        refreshPermissions()
        _uiState.update { it.copy(activeSubMenu = subMenu) }
    }

    fun closeSubMenu() {
        if (_uiState.value.isScanning) {
            bluetoothManager.stopScan()
        }
        _uiState.update { it.copy(activeSubMenu = null) }
    }

    fun refreshPermissions() {
        val checklist = PermissionHelper.buildChecklist(context)
        val allGranted = PermissionHelper.hasAllBluetoothPermissions(context)
        _uiState.update {
            it.copy(
                hasAllPermissions = allGranted,
                permissionChecklist = checklist,
            )
        }
        bluetoothManager.refreshState()
    }

    fun startBluetoothScan() {
        refreshPermissions()
        if (!_uiState.value.hasAllPermissions) {
            openSubMenu(GlassSubMenu.PermissionHelper)
            return
        }
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

    fun setNoiseControlMode(mode: NoiseControlMode) {
        viewModelScope.launch {
            prefsRepo.setNoiseMode(mode)
        }
    }

    fun setAncStrength(strength: AncStrength) {
        viewModelScope.launch {
            prefsRepo.setAncStrength(strength)
        }
    }

    fun setPersonalisedAnc(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setPersonalisedAnc(enabled)
        }
    }

    fun setWindNoiseReduction(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setWindReduction(enabled)
        }
    }

    fun setTransparencyVoiceFocus(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setTransparencyVoiceFocus(enabled)
        }
    }

    fun setDualConnection(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setDualConnection(enabled)
        }
    }

    fun setInEarDetection(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setInEarDetection(enabled)
        }
    }

    fun setLowLatencyMode(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setLowLatencyMode(enabled)
        }
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
        viewModelScope.launch {
            prefsRepo.setControls(updated)
        }
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
