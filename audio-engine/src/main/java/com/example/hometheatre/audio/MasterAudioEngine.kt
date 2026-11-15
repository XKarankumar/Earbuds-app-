package com.example.hometheatre.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.example.hometheatre.core.AudioTelemetrySnapshot
import com.example.hometheatre.core.UserAudioPreferences
import com.example.hometheatre.spatial.SpatialAudioController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class ReferenceTestMode(val label: String, val subtitle: String) {
    CINEMA_DEMO("Cinema Soundstage Pulse", "Synthesized sub-bass + stereo harmonic demo"),
    SUB_SWEEP("Subwoofer 28Hz–110Hz Sweep", "Calibrate bass boost, LFE gain, and crossover"),
    SPATIAL_STAGE("3D Spatial Acoustic Test", "Rotating stereo/ambience field for spatializer checks")
}

data class MasterEngineState(
    val isMedia3Playing: Boolean = false,
    val isReferenceSynthPlaying: Boolean = false,
    val activeReferenceMode: ReferenceTestMode? = null,
    val loadedMediaTitle: String = "No file loaded (use Reference Test or Pick Audio)",
    val mediaPositionMs: Long = 0L,
    val mediaDurationMs: Long = 0L,
    val hardwareEffectsBound: Boolean = false,
    val deviceVolume: Int = 8,
    val deviceMaxVolume: Int = 15,
    val safeVolumeWarningActive: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Coordinates Media3 ExoPlayer (with custom DspMedia3AudioProcessor), low-latency AudioTrack
 * reference synthesizer, system AudioEffects, and AudioManager device volume integration.
 */
@OptIn(UnstableApi::class)
class MasterAudioEngine(
    private val context: Context,
    private val spatialController: SpatialAudioController
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())

    val dspChain = TheatreDspChain()
    val analyzer = RealTimeAudioAnalyzer()
    val hardwareEffects = HardwareEffectsBridge()

    private val _telemetry = MutableStateFlow(AudioTelemetrySnapshot())
    val telemetry: StateFlow<AudioTelemetrySnapshot> = _telemetry.asStateFlow()

    private var currentPrefs = UserAudioPreferences()
    private val _state = MutableStateFlow(readInitialEngineState())
    val state: StateFlow<MasterEngineState> = _state.asStateFlow()

    private var synthThread: Thread? = null
    @Volatile private var synthRunning = false

    private val mediaProcessor = DspMedia3AudioProcessor(
        dspChain = dspChain,
        analyzer = analyzer,
        onTelemetry = { snap -> _telemetry.value = snap }
    )

    val exoPlayer: ExoPlayer by lazy {
        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setAudioProcessors(arrayOf(mediaProcessor))
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .build()
            }
        }

        ExoPlayer.Builder(context, renderersFactory).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        stopReferenceSynth()
                    }
                    syncStateWithPlayer()
                    if (!isPlaying && !synthRunning) {
                        _telemetry.value = AudioTelemetrySnapshot(isPlaying = false)
                    }
                }

                override fun onAudioSessionIdChanged(audioSessionId: Int) {
                    hardwareEffects.attachToSession(audioSessionId, currentPrefs)
                    _state.value = _state.value.copy(
                        hardwareEffectsBound = hardwareEffects.hardwareEffectsBound
                    )
                }

                override fun onPlayerError(error: PlaybackException) {
                    _state.value = _state.value.copy(
                        isMedia3Playing = false,
                        errorMessage = error.localizedMessage ?: "Audio playback error"
                    )
                }
            })
        }
    }

    private fun readInitialEngineState(): MasterEngineState {
        val vol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return MasterEngineState(
            deviceVolume = vol,
            deviceMaxVolume = maxVol,
            safeVolumeWarningActive = isUnsafeLevel(vol, maxVol, currentPrefs)
        )
    }

    fun applyPreferences(prefs: UserAudioPreferences) {
        currentPrefs = prefs
        dspChain.configure(prefs, 44100)
        hardwareEffects.applyPreferences(prefs)
        spatialController.setSpatialMode(prefs.spatialMode)
        refreshDeviceVolume()
    }

    fun loadAndPlayUri(uri: Uri, title: String) {
        mainHandler.post {
            stopReferenceSynth()
            mediaProcessor.currentSourceLabel = title
            _state.value = _state.value.copy(
                loadedMediaTitle = title,
                errorMessage = null
            )
            exoPlayer.setMediaItem(MediaItem.fromUri(uri))
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    fun toggleMedia3PlayPause() {
        mainHandler.post {
            if (exoPlayer.mediaItemCount == 0) {
                toggleReferenceSynth(ReferenceTestMode.CINEMA_DEMO)
                return@post
            }
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
            } else {
                stopReferenceSynth()
                exoPlayer.play()
            }
        }
    }

    fun seekMedia3To(positionMs: Long) {
        mainHandler.post {
            if (exoPlayer.mediaItemCount > 0) {
                exoPlayer.seekTo(positionMs.coerceAtLeast(0L))
                syncStateWithPlayer()
            }
        }
    }

    fun pauseAll() {
        mainHandler.post {
            if (exoPlayer.isPlaying) exoPlayer.pause()
        }
        stopReferenceSynth()
    }

    fun refreshDeviceVolume() {
        val vol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        _state.value = _state.value.copy(
            deviceVolume = vol,
            deviceMaxVolume = maxVol,
            safeVolumeWarningActive = isUnsafeLevel(vol, maxVol, currentPrefs)
        )
    }

    fun setDeviceVolume(targetStep: Int) {
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val clamped = targetStep.coerceIn(0, maxVol)
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, clamped, 0)
        } catch (_: SecurityException) {
            // Do-not-disturb policy restriction on some devices
        }
        refreshDeviceVolume()
    }

    fun syncPositionTick() {
        mainHandler.post {
            if (exoPlayer.mediaItemCount > 0) {
                _state.value = _state.value.copy(
                    mediaPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
                    mediaDurationMs = exoPlayer.duration.coerceAtLeast(0L)
                )
            }
            refreshDeviceVolume()
        }
    }

    fun toggleReferenceSynth(mode: ReferenceTestMode) {
        if (synthRunning && _state.value.activeReferenceMode == mode) {
            stopReferenceSynth()
            return
        }
        mainHandler.post {
            if (exoPlayer.isPlaying) exoPlayer.pause()
        }
        startReferenceSynth(mode)
    }

    fun stopReferenceSynth() {
        synthRunning = false
        try {
            synthThread?.join(250)
        } catch (_: InterruptedException) {
        }
        synthThread = null
        _state.value = _state.value.copy(
            isReferenceSynthPlaying = false,
            activeReferenceMode = null
        )
        if (!exoPlayer.isPlaying) {
            _telemetry.value = AudioTelemetrySnapshot(isPlaying = false)
        }
    }

    private fun startReferenceSynth(mode: ReferenceTestMode) {
        stopReferenceSynth()
        synthRunning = true
        _state.value = _state.value.copy(
            isReferenceSynthPlaying = true,
            activeReferenceMode = mode,
            errorMessage = null
        )

        synthThread = thread(name = "TheatreRefSynth", priority = Thread.MAX_PRIORITY) {
            val sampleRate = 44100
            val framesPerBuf = 512
            val minBufBytes = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(framesPerBuf * 4)

            val attrs = spatialController.recommendedAudioAttributes(currentPrefs.spatialMode)
            val format = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build()

            val trackBuilder = AudioTrack.Builder()
                .setAudioAttributes(attrs)
                .setAudioFormat(format)
                .setBufferSizeInBytes(minBufBytes * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                trackBuilder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            }

            val track = try {
                trackBuilder.build()
            } catch (t: Throwable) {
                synthRunning = false
                _state.value = _state.value.copy(
                    isReferenceSynthPlaying = false,
                    activeReferenceMode = null,
                    errorMessage = t.localizedMessage ?: "AudioTrack initialization failed"
                )
                return@thread
            }

            try {
                hardwareEffects.attachToSession(track.audioSessionId, currentPrefs)
                _state.value = _state.value.copy(hardwareEffectsBound = hardwareEffects.hardwareEffectsBound)
                track.play()

                val stereoFloat = FloatArray(framesPerBuf * 2)
                val pcmShorts = ShortArray(framesPerBuf * 2)
                var sampleIdx = 0L
                var bassPhase = 0.0
                var subPhase = 0.0
                var leadPhase = 0.0
                var panPhase = 0.0

                while (synthRunning) {
                    dspChain.configure(currentPrefs, sampleRate)
                    for (i in 0 until framesPerBuf) {
                        val tSec = sampleIdx.toDouble() / sampleRate.toDouble()
                        val (l, r) = when (mode) {
                            ReferenceTestMode.CINEMA_DEMO -> {
                                // 120 BPM punchy cinema kick + warm chord pad + stereo shimmer
                                val beatPos = (tSec * 2.0) % 1.0
                                val kickEnv = exp(-beatPos * 11.0)
                                val kickFreq = 44.0 + 90.0 * exp(-beatPos * 28.0)
                                bassPhase += 2.0 * PI * kickFreq / sampleRate
                                val kick = sin(bassPhase) * kickEnv * 0.68

                                val subFreq = if (((tSec * 0.5).toInt() % 2) == 0) 55.0 else 41.2
                                subPhase += 2.0 * PI * subFreq / sampleRate
                                val sub = sin(subPhase) * 0.28

                                val chordFreq = 220.0 * (1.0 + 0.002 * sin(2.0 * PI * 0.5 * tSec))
                                leadPhase += 2.0 * PI * chordFreq / sampleRate
                                val pad = (sin(leadPhase) + 0.5 * sin(leadPhase * 1.5) + 0.25 * sin(leadPhase * 2.0)) * 0.14

                                val pan = 0.5 + 0.35 * sin(2.0 * PI * 0.25 * tSec)
                                val left = (kick + sub + pad * (1.1 - pan)).toFloat()
                                val right = (kick + sub + pad * (0.4 + pan)).toFloat()
                                left to right
                            }
                            ReferenceTestMode.SUB_SWEEP -> {
                                // Continuous 28 Hz -> 110 Hz logarithmic subwoofer sweep
                                val cycle = (tSec % 6.0) / 6.0
                                val freq = 28.0 + cycle * 82.0
                                subPhase += 2.0 * PI * freq / sampleRate
                                val amp = (sin(subPhase) * 0.55 + 0.12 * sin(subPhase * 2.0)).toFloat()
                                amp to amp
                            }
                            ReferenceTestMode.SPATIAL_STAGE -> {
                                // Rotating 360-degree stereo acoustic soundstage ping + warm pink bed
                                panPhase += 2.0 * PI * 0.35 / sampleRate
                                val angle = panPhase
                                val lGain = (0.5 + 0.5 * sin(angle)).toFloat()
                                val rGain = (0.5 - 0.5 * sin(angle)).toFloat()
                                val pulsePos = (tSec * 3.0) % 1.0
                                val env = exp(-pulsePos * 9.0)
                                leadPhase += 2.0 * PI * 440.0 / sampleRate
                                subPhase += 2.0 * PI * 82.4 / sampleRate
                                val ping = sin(leadPhase) * env * 0.45 + sin(subPhase) * 0.22
                                (ping * lGain).toFloat() to (ping * rGain).toFloat()
                            }
                        }
                        stereoFloat[i * 2] = l
                        stereoFloat[i * 2 + 1] = r
                        sampleIdx++
                    }

                    dspChain.processStereoInPlace(stereoFloat, framesPerBuf)
                    for (j in 0 until framesPerBuf * 2) {
                        pcmShorts[j] = (stereoFloat[j] * 32767f).coerceIn(-32768f, 32767f).toInt().toShort()
                    }
                    track.write(pcmShorts, 0, pcmShorts.size)

                    _telemetry.value = analyzer.analyzeInterleavedStereo(
                        interleaved = stereoFloat,
                        frameCount = framesPerBuf,
                        sampleRateHz = sampleRate,
                        limiterGainReductionDb = dspChain.limiter.lastGainReductionDb,
                        clippingProtected = dspChain.limiter.protectionEngaged,
                        isPlaying = true,
                        sourceLabel = mode.label
                    )
                }
            } finally {
                try {
                    track.stop()
                    track.release()
                } catch (_: Throwable) {
                }
            }
        }
    }

    private fun isUnsafeLevel(vol: Int, maxVol: Int, prefs: UserAudioPreferences): Boolean {
        if (!prefs.safeVolumeWarningEnabled) return false
        val volRatio = vol.toFloat() / maxVol.coerceAtLeast(1)
        return volRatio >= 0.8f || prefs.loudnessGainDb >= 7.0f
    }

    private fun syncStateWithPlayer() {
        val vol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        _state.value = _state.value.copy(
            isMedia3Playing = exoPlayer.isPlaying,
            mediaPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
            mediaDurationMs = exoPlayer.duration.coerceAtLeast(0L),
            deviceVolume = vol,
            deviceMaxVolume = maxVol,
            safeVolumeWarningActive = isUnsafeLevel(vol, maxVol, currentPrefs)
        )
    }

    fun release() {
        stopReferenceSynth()
        hardwareEffects.release()
        mainHandler.post {
            try {
                exoPlayer.release()
            } catch (_: Throwable) {
            }
        }
    }
}
