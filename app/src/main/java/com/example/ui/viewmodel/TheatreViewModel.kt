package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.UserPreferencesStore
import com.example.hometheatre.audio.MasterAudioEngine
import com.example.hometheatre.audio.MasterEngineState
import com.example.hometheatre.audio.ReferenceTestMode
import com.example.hometheatre.compatibility.DeviceCapabilityDetector
import com.example.hometheatre.core.AudioTelemetrySnapshot
import com.example.hometheatre.core.CustomThemeConfig
import com.example.hometheatre.core.DefaultEqCatalog
import com.example.hometheatre.core.DeviceCompatibilityReport
import com.example.hometheatre.core.EqPreset
import com.example.hometheatre.core.SavedRgbPreset
import com.example.hometheatre.core.SpatialMode
import com.example.hometheatre.core.ThemeManager
import com.example.hometheatre.core.ThemePreset
import com.example.hometheatre.core.UserAudioPreferences
import com.example.hometheatre.dj.DjConsoleState
import com.example.hometheatre.dj.DualDeckDjEngine
import com.example.hometheatre.dj.PerformancePadType
import com.example.hometheatre.dolby.DolbyCapabilityDetector
import com.example.hometheatre.rgb.RgbControllerState
import com.example.hometheatre.rgb.RgbHardwareController
import com.example.hometheatre.spatial.SpatialAudioController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TheatreViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsStore = UserPreferencesStore(application)
    val themeManager = ThemeManager(application)
    val spatialController = SpatialAudioController(application)
    val dolbyDetector = DolbyCapabilityDetector(application)
    val capabilityDetector = DeviceCapabilityDetector(application, spatialController, dolbyDetector)
    val masterAudioEngine = MasterAudioEngine(application, spatialController)
    val djEngine = DualDeckDjEngine(application)
    val rgbController = RgbHardwareController(application, viewModelScope)

    val preferences: StateFlow<UserAudioPreferences> = combine(
        prefsStore.preferencesFlow,
        themeManager.themeStateFlow
    ) { audioPrefs, themeState ->
        audioPrefs.copy(
            themePreset = themeState.selectedPreset,
            customTheme = themeState.customConfig
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UserAudioPreferences())

    val masterEngineState: StateFlow<MasterEngineState> = masterAudioEngine.state
    val djState: StateFlow<DjConsoleState> = djEngine.state
    val rgbState: StateFlow<RgbControllerState> = rgbController.state
    val compatibilityReport: StateFlow<DeviceCompatibilityReport> = capabilityDetector.report

    val telemetry: StateFlow<AudioTelemetrySnapshot> = combine(
        masterAudioEngine.telemetry,
        djEngine.telemetry
    ) { master, dj ->
        when {
            dj.isPlaying -> dj
            master.isPlaying -> master
            else -> master
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AudioTelemetrySnapshot())

    private val _statusMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val statusMessage: SharedFlow<String> = _statusMessage.asSharedFlow()

    private val _safeVolumeWarningAck = MutableStateFlow(false)
    val safeVolumeWarningDismissed: StateFlow<Boolean> = _safeVolumeWarningAck.asStateFlow()

    init {
        capabilityDetector.startMonitoring()
        rgbController.startLoop()

        viewModelScope.launch {
            preferences.collect { prefs ->
                masterAudioEngine.applyPreferences(prefs)
                rgbController.updateParameters(
                    effectId = prefs.rgbEffectId,
                    brightness = prefs.rgbBrightness,
                    speed = prefs.rgbSpeed,
                    customColorArgb = prefs.rgbCustomColorHex.toInt()
                )
            }
        }

        viewModelScope.launch {
            telemetry.collect { snap ->
                rgbController.updateAudioTelemetry(snap)
            }
        }

        // Periodic position & hardware volume refresh
        viewModelScope.launch {
            while (true) {
                masterAudioEngine.syncPositionTick()
                delay(400)
            }
        }
    }

    // --- Theme & Preferences ---
    fun selectTheme(preset: ThemePreset) {
        viewModelScope.launch {
            themeManager.selectTheme(preset)
        }
    }

    fun updateCustomTheme(config: CustomThemeConfig) {
        viewModelScope.launch {
            themeManager.updateCustomTheme(config)
        }
    }

    // --- Master Studio Audio & Volume ---
    fun toggleStudioPlayPause() {
        djEngine.pauseAllDecks()
        masterAudioEngine.toggleMedia3PlayPause()
    }

    fun toggleReferenceTest(mode: ReferenceTestMode) {
        djEngine.pauseAllDecks()
        masterAudioEngine.toggleReferenceSynth(mode)
    }

    fun loadStudioAudioFile(uri: Uri) {
        val title = resolveFileName(getApplication(), uri) ?: "Selected Audio Track"
        djEngine.pauseAllDecks()
        masterAudioEngine.loadAndPlayUri(uri, title)
        emitToast("Loaded \"$title\" into Studio Master DSP")
    }

    fun seekStudioAudio(positionMs: Long) {
        masterAudioEngine.seekMedia3To(positionMs)
    }

    fun setDeviceVolume(step: Int) {
        masterAudioEngine.setDeviceVolume(step)
        capabilityDetector.refresh()
    }

    fun setLoudnessGainDb(gainDb: Float) {
        viewModelScope.launch {
            prefsStore.update { it.copy(loudnessGainDb = gainDb.coerceIn(0f, 12f)) }
        }
    }

    fun setLimiterEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsStore.update { it.copy(limiterEnabled = enabled) }
        }
    }

    fun setSafeVolumeWarningEnabled(enabled: Boolean) {
        _safeVolumeWarningAck.value = false
        viewModelScope.launch {
            prefsStore.update { it.copy(safeVolumeWarningEnabled = enabled) }
        }
    }

    fun dismissSafeVolumeAlert() {
        _safeVolumeWarningAck.value = true
    }

    // --- Bass, Subwoofer & 10-Band EQ ---
    fun setBassBoost(level: Float) {
        viewModelScope.launch {
            prefsStore.update { it.copy(bassBoostLevel = level.coerceIn(0f, 1f)) }
        }
    }

    fun setSubwooferGainDb(gainDb: Float) {
        viewModelScope.launch {
            prefsStore.update { it.copy(subwooferGainDb = gainDb.coerceIn(-6f, 12f)) }
        }
    }

    fun setDeepBassEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsStore.update { it.copy(deepBassEnabled = enabled) }
        }
    }

    fun setCinemaBassEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsStore.update { it.copy(cinemaBassEnabled = enabled) }
        }
    }

    fun setMidGainDb(gainDb: Float) {
        viewModelScope.launch {
            prefsStore.update { it.copy(midGainDb = gainDb.coerceIn(-12f, 12f)) }
        }
    }

    fun setTrebleGainDb(gainDb: Float) {
        viewModelScope.launch {
            prefsStore.update { it.copy(trebleGainDb = gainDb.coerceIn(-12f, 12f)) }
        }
    }

    fun setEqBandGain(index: Int, gainDb: Float) {
        viewModelScope.launch {
            prefsStore.update { current ->
                val updated = current.customBandGainsDb.toMutableList()
                while (updated.size < 10) updated.add(0f)
                if (index in 0..9) {
                    updated[index] = gainDb.coerceIn(-12f, 12f)
                }
                current.copy(
                    selectedEqPresetId = "custom_manual",
                    customBandGainsDb = updated
                )
            }
        }
    }

    fun selectEqPreset(preset: EqPreset) {
        viewModelScope.launch {
            prefsStore.update {
                it.copy(
                    selectedEqPresetId = preset.id,
                    customBandGainsDb = preset.bandsDb.take(10)
                )
            }
        }
    }

    fun saveCustomEqPreset(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            prefsStore.update { current ->
                val id = "user_eq_${System.currentTimeMillis()}"
                val newPreset = EqPreset(
                    id = id,
                    name = clean,
                    category = "Custom",
                    bandsDb = current.customBandGainsDb.take(10),
                    isBuiltIn = false
                )
                current.copy(
                    selectedEqPresetId = id,
                    savedEqPresets = current.savedEqPresets + newPreset
                )
            }
            emitToast("Saved EQ preset \"$clean\"")
        }
    }

    fun deleteCustomEqPreset(id: String) {
        viewModelScope.launch {
            prefsStore.update { current ->
                current.copy(savedEqPresets = current.savedEqPresets.filterNot { it.id == id })
            }
        }
    }

    fun resetEqFlat() {
        selectEqPreset(DefaultEqCatalog.builtInPresets.first())
    }

    // --- DJ Console ---
    fun toggleDjPlayPause(deckId: String) {
        masterAudioEngine.pauseAll()
        djEngine.togglePlayPause(deckId)
    }

    fun triggerDjCue(deckId: String) = djEngine.triggerCue(deckId)
    fun seekDjDeck(deckId: String, norm: Float) = djEngine.seekNormalized(deckId, norm)
    fun scratchDjDeck(deckId: String, deltaRadians: Float) {
        masterAudioEngine.pauseAll()
        djEngine.scratch(deckId, deltaRadians)
    }

    fun toggleDjLoop(deckId: String, beats: Int) = djEngine.toggleLoop(deckId, beats)
    fun triggerDjHotCue(deckId: String, slot: Int) {
        masterAudioEngine.pauseAll()
        djEngine.triggerHotCue(deckId, slot)
    }

    fun clearDjHotCue(deckId: String, slot: Int) = djEngine.clearHotCue(deckId, slot)
    fun setDjTempo(deckId: String, mult: Float) = djEngine.setTempo(deckId, mult)
    fun syncDjDeck(deckId: String) = djEngine.syncDeckToOther(deckId)
    fun setDjDeckEq(deckId: String, low: Float? = null, mid: Float? = null, high: Float? = null, filter: Float? = null) =
        djEngine.setDeckEq(deckId, low, mid, high, filter)

    fun setDjCrossfader(value: Float) = djEngine.setCrossfader(value)
    fun setDjEcho(value: Float) = djEngine.setEchoMix(value)
    fun setDjReverb(value: Float) = djEngine.setReverbMix(value)

    fun triggerDjPad(padType: PerformancePadType) {
        masterAudioEngine.pauseAll()
        djEngine.triggerPerformancePad(padType)
    }

    fun loadDjBuiltInTrack(deckId: String, trackId: String) {
        djEngine.loadBuiltInTrack(deckId, trackId)
    }

    fun loadDjAudioFile(deckId: String, uri: Uri) {
        val title = resolveFileName(getApplication(), uri) ?: "Deck $deckId Audio"
        emitToast("Decoding \"$title\" for Deck $deckId...")
        viewModelScope.launch(Dispatchers.IO) {
            val ok = djEngine.loadAudioFile(deckId, uri, title)
            if (ok) {
                emitToast("Deck $deckId loaded: $title (BPM detected)")
            } else {
                emitToast("Could not decode \"$title\". Try MP3, WAV, M4A, or FLAC.")
            }
        }
    }

    // --- RGB Studio ---
    fun selectRgbEffect(effectId: Int) {
        viewModelScope.launch {
            prefsStore.update { it.copy(rgbEffectId = effectId) }
        }
    }

    fun setRgbBrightness(brightness: Float) {
        viewModelScope.launch {
            prefsStore.update { it.copy(rgbBrightness = brightness.coerceIn(0.05f, 1f)) }
        }
    }

    fun setRgbSpeed(speed: Float) {
        viewModelScope.launch {
            prefsStore.update { it.copy(rgbSpeed = speed.coerceIn(0.05f, 1f)) }
        }
    }

    fun setRgbCustomColor(argbHex: Long) {
        viewModelScope.launch {
            prefsStore.update { it.copy(rgbCustomColorHex = argbHex) }
        }
    }

    fun saveRgbPreset(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            prefsStore.update { current ->
                val preset = SavedRgbPreset(
                    id = "rgb_${System.currentTimeMillis()}",
                    name = clean,
                    effectId = current.rgbEffectId,
                    brightness = current.rgbBrightness,
                    speed = current.rgbSpeed,
                    customColorHex = current.rgbCustomColorHex
                )
                current.copy(savedRgbPresets = current.savedRgbPresets + preset)
            }
            emitToast("Saved RGB preset \"$clean\"")
        }
    }

    fun applyRgbPreset(preset: SavedRgbPreset) {
        viewModelScope.launch {
            prefsStore.update {
                it.copy(
                    rgbEffectId = preset.effectId,
                    rgbBrightness = preset.brightness,
                    rgbSpeed = preset.speed,
                    rgbCustomColorHex = preset.customColorHex
                )
            }
        }
    }

    fun connectWled(ip: String) {
        viewModelScope.launch { prefsStore.update { it.copy(wledHostIp = ip.trim()) } }
        rgbController.connectWledUdp(ip)
    }

    fun connectEsp32(ip: String) {
        viewModelScope.launch { prefsStore.update { it.copy(esp32HostIp = ip.trim()) } }
        rgbController.connectEsp32Udp(ip)
    }

    fun disconnectRgbHardware() = rgbController.disconnectHardware()
    fun scanBleRgbControllers() = rgbController.scanBleControllers()
    fun connectBleRgbController(address: String) = rgbController.connectBleController(address)

    // --- Spatial & System ---
    fun setSpatialMode(mode: SpatialMode) {
        viewModelScope.launch {
            prefsStore.update {
                it.copy(
                    spatialMode = mode,
                    virtualSurroundEnabled = mode == SpatialMode.VIRTUAL_SURROUND
                )
            }
        }
        spatialController.setSpatialMode(mode)
        capabilityDetector.refresh()
    }

    fun refreshHardwareCapabilities() {
        rgbController.refreshPermissions()
        capabilityDetector.refresh()
        emitToast("Hardware capabilities & audio routes refreshed")
    }

    private fun emitToast(msg: String) {
        viewModelScope.launch { _statusMessage.emit(msg) }
    }

    private fun resolveFileName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && idx >= 0) cursor.getString(idx) else null
            }
        } catch (_: Throwable) {
            uri.lastPathSegment
        }
    }

    override fun onCleared() {
        capabilityDetector.stopMonitoring()
        rgbController.stopLoop()
        djEngine.stopEngine()
        masterAudioEngine.release()
        super.onCleared()
    }
}
