package com.example.hometheatre.dj

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.Uri
import android.os.Build
import com.example.hometheatre.audio.RealTimeAudioAnalyzer
import com.example.hometheatre.audio.SoftClipLimiter
import com.example.hometheatre.audio.StereoBiquadFilter
import com.example.hometheatre.core.AudioTelemetrySnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

data class DjDeckUiState(
    val deckId: String,
    val trackTitle: String,
    val trackArtist: String,
    val originalBpm: Float,
    val effectiveBpm: Float,
    val tempoMultiplier: Float = 1.0f,
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val cuePointNormalized: Float = 0f,
    val hotCuesNormalized: List<Float?> = listOf(null, null, null, null),
    val loopBeats: Int? = null,
    val lowEqDb: Float = 0f,
    val midEqDb: Float = 0f,
    val highEqDb: Float = 0f,
    val filterSweep: Float = 0f, // -1.0 LPF, 0.0 off, +1.0 HPF
    val waveformPeaks: List<Float> = List(96) { 0.25f },
    val scratchVelocity: Float = 0f
)

data class DjConsoleState(
    val deckA: DjDeckUiState,
    val deckB: DjDeckUiState,
    val crossfader: Float = 0f, // -1.0 Deck A .. 0.0 Center .. +1.0 Deck B
    val echoMix: Float = 0f,
    val reverbMix: Float = 0f,
    val masterDjGain: Float = 0.9f,
    val builtInLibrary: List<DecodedAudioTrack> = emptyList(),
    val activePadFlashId: Int? = null,
    val engineRunning: Boolean = false
)

private class InternalDeckProcessor(
    val deckId: String,
    var track: DecodedAudioTrack
) {
    @Volatile var isPlaying: Boolean = false
    @Volatile var playheadFrame: Double = 0.0
    @Volatile var tempoMultiplier: Float = 1.0f
    @Volatile var scratchRateOffset: Float = 0f
    @Volatile var cueFrame: Double = 0.0
    val hotCueFrames = Array<Double?>(4) { null }

    @Volatile var loopBeats: Int? = null
    @Volatile var loopStartFrame: Double = 0.0
    @Volatile var loopLengthFrames: Double = 0.0

    @Volatile var lowDb: Float = 0f
    @Volatile var midDb: Float = 0f
    @Volatile var highDb: Float = 0f
    @Volatile var filterSweep: Float = 0f

    val lowFilter = StereoBiquadFilter()
    val midFilter = StereoBiquadFilter()
    val highFilter = StereoBiquadFilter()
    val sweepFilter = StereoBiquadFilter()

    fun updateFilters(sampleRate: Double) {
        lowFilter.configureLowShelf(220.0, lowDb.toDouble(), sampleRate)
        midFilter.configurePeaking(1000.0, 0.9, midDb.toDouble(), sampleRate)
        highFilter.configureHighShelf(3800.0, highDb.toDouble(), sampleRate)

        when {
            filterSweep < -0.05f -> {
                val cutoff = 18000.0 * (0.015).pow((-filterSweep).toDouble())
                sweepFilter.configureLowPass(cutoff.coerceIn(110.0, 18000.0), 1.35, sampleRate)
            }
            filterSweep > 0.05f -> {
                val cutoff = 40.0 * (160.0).pow(filterSweep.toDouble())
                sweepFilter.configureHighPass(cutoff.coerceIn(40.0, 7500.0), 1.35, sampleRate)
            }
            else -> sweepFilter.setBypass()
        }
    }

    fun readNextStereoFrame(sampleRate: Int): Pair<Float, Float> {
        val total = track.totalFrames
        if (total <= 1) return 0f to 0f

        val activeScratch = kotlin.math.abs(scratchRateOffset) > 0.02f
        if (!isPlaying && !activeScratch) return 0f to 0f

        val step = if (activeScratch) scratchRateOffset.toDouble() else tempoMultiplier.toDouble()
        playheadFrame += step

        // Loop wrapping
        val activeLoop = loopBeats
        if (activeLoop != null && loopLengthFrames > 512.0 && !activeScratch) {
            val loopEnd = loopStartFrame + loopLengthFrames
            if (playheadFrame >= loopEnd) {
                playheadFrame = loopStartFrame + (playheadFrame - loopEnd)
            } else if (playheadFrame < loopStartFrame) {
                playheadFrame = loopStartFrame
            }
        }

        while (playheadFrame >= total - 1) playheadFrame -= (total - 1)
        while (playheadFrame < 0.0) playheadFrame += (total - 1)

        val idx0 = playheadFrame.toInt().coerceIn(0, total - 2)
        val frac = (playheadFrame - idx0).toFloat()
        val pcm = track.stereoInterleaved
        var l = pcm[idx0 * 2] * (1f - frac) + pcm[(idx0 + 1) * 2] * frac
        var r = pcm[idx0 * 2 + 1] * (1f - frac) + pcm[(idx0 + 1) * 2 + 1] * frac

        l = lowFilter.processLeft(l)
        r = lowFilter.processRight(r)
        l = midFilter.processLeft(l)
        r = midFilter.processRight(r)
        l = highFilter.processLeft(l)
        r = highFilter.processRight(r)
        l = sweepFilter.processLeft(l)
        r = sweepFilter.processRight(r)

        // Decay scratch impulse smoothly back to 0
        if (activeScratch) {
            scratchRateOffset *= 0.9965f
        }
        return l to r
    }

    fun toUiState(): DjDeckUiState {
        val progress = (playheadFrame / track.totalFrames.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)
        val cueNorm = (cueFrame / track.totalFrames.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)
        val hotCues = hotCueFrames.map { f ->
            f?.let { (it / track.totalFrames.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f) }
        }
        return DjDeckUiState(
            deckId = deckId,
            trackTitle = track.title,
            trackArtist = track.artist,
            originalBpm = track.detectedBpm,
            effectiveBpm = ((track.detectedBpm * tempoMultiplier) * 10f).toInt() / 10f,
            tempoMultiplier = tempoMultiplier,
            isPlaying = isPlaying,
            progress = progress,
            cuePointNormalized = cueNorm,
            hotCuesNormalized = hotCues,
            loopBeats = loopBeats,
            lowEqDb = lowDb,
            midEqDb = midDb,
            highEqDb = highDb,
            filterSweep = filterSweep,
            waveformPeaks = track.waveformPeaks,
            scratchVelocity = scratchRateOffset
        )
    }
}

/**
 * Real-time dual-deck stereo audio mixing engine with equal-power crossfader,
 * Echo delay line, Schroeder multi-tap Reverb, 3-band per-deck EQ, resonant filter,
 * BPM sync, loop quantization, scratch scrubbing, and 16 live performance pads.
 */
class DualDeckDjEngine(
    private val context: Context
) {
    private val builtInTracks = PcmTrackDecoder.buildBuiltInTracks()
    private val deckAProc = InternalDeckProcessor("A", builtInTracks[0])
    private val deckBProc = InternalDeckProcessor("B", builtInTracks[1])

    private val activePadVoices = CopyOnWriteArrayList<PerformancePadVoice>()
    private val limiter = SoftClipLimiter()
    private val analyzer = RealTimeAudioAnalyzer()

    @Volatile private var crossfader = 0f
    @Volatile private var echoMix = 0f
    @Volatile private var reverbMix = 0f
    @Volatile private var masterGain = 0.9f
    @Volatile private var running = false
    private var mixerThread: Thread? = null

    // Echo & Reverb delay buffers
    private val echoBufL = FloatArray(44100)
    private val echoBufR = FloatArray(44100)
    private var echoWrite = 0

    private val revBuf1 = FloatArray(3113)
    private val revBuf2 = FloatArray(4217)
    private val revBuf3 = FloatArray(5399)
    private var revIdx1 = 0
    private var revIdx2 = 0
    private var revIdx3 = 0

    private val _state = MutableStateFlow(snapshotState())
    val state: StateFlow<DjConsoleState> = _state.asStateFlow()

    private val _telemetry = MutableStateFlow(AudioTelemetrySnapshot())
    val telemetry: StateFlow<AudioTelemetrySnapshot> = _telemetry.asStateFlow()

    private fun deckProc(deckId: String) = if (deckId == "A") deckAProc else deckBProc

    fun startEngineIfNeeded() {
        if (running) return
        running = true
        mixerThread = thread(name = "DualDeckDjMixer", priority = Thread.MAX_PRIORITY) {
            val sampleRate = PcmTrackDecoder.TARGET_SAMPLE_RATE
            val framesPerBuf = 512
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(framesPerBuf * 4)

            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val fmt = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build()

            val trackBuilder = AudioTrack.Builder()
                .setAudioAttributes(attrs)
                .setAudioFormat(fmt)
                .setBufferSizeInBytes(minBuf * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                trackBuilder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            }

            val track = try {
                trackBuilder.build()
            } catch (_: Throwable) {
                running = false
                emitState()
                return@thread
            }

            val mixStereo = FloatArray(framesPerBuf * 2)
            val pcmShorts = ShortArray(framesPerBuf * 2)
            var uiTickCounter = 0

            try {
                track.play()
                emitState()

                while (running) {
                    val activeAudio = deckAProc.isPlaying ||
                        deckBProc.isPlaying ||
                        kotlin.math.abs(deckAProc.scratchRateOffset) > 0.02f ||
                        kotlin.math.abs(deckBProc.scratchRateOffset) > 0.02f ||
                        activePadVoices.isNotEmpty()

                    if (!activeAudio) {
                        java.lang.Thread.sleep(24)
                        uiTickCounter++
                        if (uiTickCounter % 4 == 0) {
                            _telemetry.value = AudioTelemetrySnapshot(isPlaying = false)
                        }
                        continue
                    }

                    deckAProc.updateFilters(sampleRate.toDouble())
                    deckBProc.updateFilters(sampleRate.toDouble())

                    // Equal-power crossfader law
                    val xfNorm = ((crossfader + 1f) * 0.5f).coerceIn(0f, 1f)
                    val gainA = cos(xfNorm * PI * 0.5).toFloat()
                    val gainB = sin(xfNorm * PI * 0.5).toFloat()

                    val avgBpm = ((deckAProc.track.detectedBpm * deckAProc.tempoMultiplier) +
                        (deckBProc.track.detectedBpm * deckBProc.tempoMultiplier)) * 0.5f
                    val delaySamples = ((sampleRate * (60.0 / avgBpm.coerceIn(60f, 180f)) * 0.5).toInt())
                        .coerceIn(2000, echoBufL.size - 1)

                    for (i in 0 until framesPerBuf) {
                        val (aL, aR) = deckAProc.readNextStereoFrame(sampleRate)
                        val (bL, bR) = deckBProc.readNextStereoFrame(sampleRate)
                        mixStereo[i * 2] = aL * gainA + bL * gainB
                        mixStereo[i * 2 + 1] = aR * gainA + bR * gainB
                    }

                    // Mix active performance pad voices
                    if (activePadVoices.isNotEmpty()) {
                        val finished = mutableListOf<PerformancePadVoice>()
                        for (voice in activePadVoices) {
                            for (i in 0 until framesPerBuf) {
                                voice.renderNextStereo(mixStereo, i)
                                if (voice.isFinished) break
                            }
                            if (voice.isFinished) finished.add(voice)
                        }
                        if (finished.isNotEmpty()) activePadVoices.removeAll(finished.toSet())
                    }

                    // Apply Master Echo + Reverb + SoftClipLimiter
                    for (i in 0 until framesPerBuf) {
                        var l = mixStereo[i * 2]
                        var r = mixStereo[i * 2 + 1]

                        if (echoMix > 0.01f) {
                            val readIdx = (echoWrite - delaySamples + echoBufL.size) % echoBufL.size
                            val dL = echoBufL[readIdx]
                            val dR = echoBufR[readIdx]
                            echoBufL[echoWrite] = l + dR * 0.42f
                            echoBufR[echoWrite] = r + dL * 0.42f
                            echoWrite = (echoWrite + 1) % echoBufL.size
                            l += dL * echoMix * 0.65f
                            r += dR * echoMix * 0.65f
                        }

                        if (reverbMix > 0.01f) {
                            val mono = 0.5f * (l + r)
                            val c1 = revBuf1[revIdx1]
                            val c2 = revBuf2[revIdx2]
                            val c3 = revBuf3[revIdx3]
                            revBuf1[revIdx1] = mono + c1 * 0.62f
                            revBuf2[revIdx2] = mono + c2 * 0.58f
                            revBuf3[revIdx3] = mono + c3 * 0.54f
                            revIdx1 = (revIdx1 + 1) % revBuf1.size
                            revIdx2 = (revIdx2 + 1) % revBuf2.size
                            revIdx3 = (revIdx3 + 1) % revBuf3.size
                            val wetL = (c1 + c3 * 0.7f) * 0.35f * reverbMix
                            val wetR = (c2 + c3 * 0.7f) * 0.35f * reverbMix
                            l += wetL
                            r += wetR
                        }

                        l = limiter.processSample(l * masterGain, enabled = true)
                        r = limiter.processSample(r * masterGain, enabled = true)
                        mixStereo[i * 2] = l
                        mixStereo[i * 2 + 1] = r
                        pcmShorts[i * 2] = (l * 32767f).coerceIn(-32768f, 32767f).toInt().toShort()
                        pcmShorts[i * 2 + 1] = (r * 32767f).coerceIn(-32768f, 32767f).toInt().toShort()
                    }
                    limiter.decayTelemetry()

                    track.write(pcmShorts, 0, pcmShorts.size)

                    uiTickCounter++
                    if (uiTickCounter % 3 == 0) {
                        emitState()
                        _telemetry.value = analyzer.analyzeInterleavedStereo(
                            interleaved = mixStereo,
                            frameCount = framesPerBuf,
                            sampleRateHz = sampleRate,
                            limiterGainReductionDb = limiter.lastGainReductionDb,
                            clippingProtected = limiter.protectionEngaged,
                            isPlaying = true,
                            sourceLabel = "DJ Dual Deck Mix"
                        )
                    }
                }
            } finally {
                try { track.stop(); track.release() } catch (_: Throwable) {}
                running = false
                emitState()
            }
        }
    }

    fun stopEngine() {
        deckAProc.isPlaying = false
        deckBProc.isPlaying = false
        running = false
        try { mixerThread?.join(250) } catch (_: InterruptedException) {}
        mixerThread = null
        emitState()
    }

    fun pauseAllDecks() {
        deckAProc.isPlaying = false
        deckBProc.isPlaying = false
        emitState()
    }

    fun togglePlayPause(deckId: String) {
        startEngineIfNeeded()
        val deck = deckProc(deckId)
        deck.isPlaying = !deck.isPlaying
        emitState()
    }

    fun triggerCue(deckId: String) {
        startEngineIfNeeded()
        val deck = deckProc(deckId)
        if (deck.isPlaying) {
            deck.isPlaying = false
            deck.playheadFrame = deck.cueFrame
        } else {
            deck.cueFrame = deck.playheadFrame
        }
        emitState()
    }

    fun seekNormalized(deckId: String, normalized: Float) {
        val deck = deckProc(deckId)
        deck.playheadFrame = (normalized.coerceIn(0f, 1f) * deck.track.totalFrames).toDouble()
        emitState()
    }

    fun scratch(deckId: String, deltaRadians: Float) {
        startEngineIfNeeded()
        val deck = deckProc(deckId)
        val velocity = (deltaRadians * 4.2f).coerceIn(-4.5f, 4.5f)
        deck.scratchRateOffset = velocity
        emitState()
    }

    fun toggleLoop(deckId: String, beats: Int) {
        val deck = deckProc(deckId)
        if (deck.loopBeats == beats) {
            deck.loopBeats = null
        } else {
            val secPerBeat = 60.0 / (deck.track.detectedBpm.coerceAtLeast(60f))
            deck.loopStartFrame = deck.playheadFrame
            deck.loopLengthFrames = beats * secPerBeat * deck.track.sampleRateHz
            deck.loopBeats = beats
        }
        emitState()
    }

    fun triggerHotCue(deckId: String, slotIndex: Int) {
        if (slotIndex !in 0..3) return
        startEngineIfNeeded()
        val deck = deckProc(deckId)
        val existing = deck.hotCueFrames[slotIndex]
        if (existing == null) {
            deck.hotCueFrames[slotIndex] = deck.playheadFrame
        } else {
            deck.playheadFrame = existing
            deck.isPlaying = true
        }
        emitState()
    }

    fun clearHotCue(deckId: String, slotIndex: Int) {
        if (slotIndex !in 0..3) return
        deckProc(deckId).hotCueFrames[slotIndex] = null
        emitState()
    }

    fun setTempo(deckId: String, multiplier: Float) {
        deckProc(deckId).tempoMultiplier = multiplier.coerceIn(0.75f, 1.25f)
        emitState()
    }

    fun syncDeckToOther(deckId: String) {
        val target = deckProc(deckId)
        val master = if (deckId == "A") deckBProc else deckAProc
        val masterEffectiveBpm = master.track.detectedBpm * master.tempoMultiplier
        val newMultiplier = (masterEffectiveBpm / target.track.detectedBpm.coerceAtLeast(1f))
            .coerceIn(0.75f, 1.25f)
        target.tempoMultiplier = newMultiplier
        // Align beat phase offset smoothly
        val secPerBeat = 60.0 / masterEffectiveBpm.coerceAtLeast(60f)
        val framesPerBeat = secPerBeat * master.track.sampleRateHz
        val masterBeatPhase = master.playheadFrame % framesPerBeat
        val targetSecPerBeat = 60.0 / (target.track.detectedBpm * newMultiplier).coerceAtLeast(60f)
        val targetFramesPerBeat = targetSecPerBeat * target.track.sampleRateHz
        val phaseRatio = masterBeatPhase / framesPerBeat
        val currentBarStart = target.playheadFrame - (target.playheadFrame % targetFramesPerBeat)
        target.playheadFrame = (currentBarStart + phaseRatio * targetFramesPerBeat)
            .coerceIn(0.0, (target.track.totalFrames - 2).toDouble())
        emitState()
    }

    fun setDeckEq(deckId: String, lowDb: Float? = null, midDb: Float? = null, highDb: Float? = null, filter: Float? = null) {
        val deck = deckProc(deckId)
        lowDb?.let { deck.lowDb = it.coerceIn(-12f, 12f) }
        midDb?.let { deck.midDb = it.coerceIn(-12f, 12f) }
        highDb?.let { deck.highDb = it.coerceIn(-12f, 12f) }
        filter?.let { deck.filterSweep = it.coerceIn(-1f, 1f) }
        emitState()
    }

    fun setCrossfader(value: Float) {
        crossfader = value.coerceIn(-1f, 1f)
        emitState()
    }

    fun setEchoMix(value: Float) {
        echoMix = value.coerceIn(0f, 1f)
        emitState()
    }

    fun setReverbMix(value: Float) {
        reverbMix = value.coerceIn(0f, 1f)
        emitState()
    }

    fun triggerPerformancePad(padType: PerformancePadType) {
        startEngineIfNeeded()
        activePadVoices.add(PerformancePadVoice(padType))
        _state.value = snapshotState().copy(activePadFlashId = padType.padNumber)
    }

    fun loadBuiltInTrack(deckId: String, trackId: String) {
        val found = builtInTracks.firstOrNull { it.id == trackId } ?: return
        loadTrackIntoDeck(deckId, found)
    }

    fun loadAudioFile(deckId: String, uri: Uri, title: String): Boolean {
        val decoded = PcmTrackDecoder.decodeUri(context, uri, title) ?: return false
        loadTrackIntoDeck(deckId, decoded)
        return true
    }

    private fun loadTrackIntoDeck(deckId: String, track: DecodedAudioTrack) {
        val deck = deckProc(deckId)
        deck.isPlaying = false
        deck.track = track
        deck.playheadFrame = 0.0
        deck.cueFrame = 0.0
        deck.loopBeats = null
        for (i in 0..3) deck.hotCueFrames[i] = null
        emitState()
    }

    private fun snapshotState(): DjConsoleState {
        return DjConsoleState(
            deckA = deckAProc.toUiState(),
            deckB = deckBProc.toUiState(),
            crossfader = crossfader,
            echoMix = echoMix,
            reverbMix = reverbMix,
            masterDjGain = masterGain,
            builtInLibrary = builtInTracks,
            engineRunning = running
        )
    }

    private fun emitState() {
        _state.value = snapshotState()
    }
}
