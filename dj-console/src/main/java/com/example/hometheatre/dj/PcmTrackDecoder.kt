package com.example.hometheatre.dj

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class DecodedAudioTrack(
    val id: String,
    val title: String,
    val artist: String,
    val sampleRateHz: Int,
    val stereoInterleaved: FloatArray,
    val totalFrames: Int,
    val detectedBpm: Float,
    val waveformPeaks: List<Float>,
    val isBuiltIn: Boolean
)

/**
 * Decodes local audio files (MP3/WAV/AAC/FLAC/OGG) via MediaExtractor + MediaCodec into
 * 44.1kHz stereo PCM float buffers, generates waveform peaks, estimates BPM using onset-energy
 * autocorrelation, and synthesizes 4 built-in studio tracks so both DJ decks mix real audio immediately.
 */
object PcmTrackDecoder {
    const val TARGET_SAMPLE_RATE = 44100
    private const val MAX_DECODE_FRAMES = 44100 * 90 // Cap memory at 90s stereo (~31MB float)

    fun buildBuiltInTracks(): List<DecodedAudioTrack> = listOf(
        synthesizeBuiltInTrack(
            id = "builtin_cyber_pulse",
            title = "Neon Horizon (Club Mix)",
            artist = "Theatre Studio Stem",
            bpm = 126f,
            bars = 8,
            style = 0
        ),
        synthesizeBuiltInTrack(
            id = "builtin_sub_cinema",
            title = "Obsidian Sub Drop",
            artist = "Theatre Studio Stem",
            bpm = 128f,
            bars = 8,
            style = 1
        ),
        synthesizeBuiltInTrack(
            id = "builtin_trap_bass",
            title = "Midnight 808 Groove",
            artist = "Theatre Studio Stem",
            bpm = 96f,
            bars = 8,
            style = 2
        ),
        synthesizeBuiltInTrack(
            id = "builtin_techno_drive",
            title = "Prism Techno Drive",
            artist = "Theatre Studio Stem",
            bpm = 136f,
            bars = 8,
            style = 3
        )
    )

    fun decodeUri(context: Context, uri: Uri, title: String): DecodedAudioTrack? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            var trackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    trackIndex = i
                    format = f
                    break
                }
            }
            if (trackIndex < 0 || format == null) return null
            extractor.selectTrack(trackIndex)

            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            var srcSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var srcChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)

            val pcmChunks = mutableListOf<Float>()
            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false

            while (!outputDone && pcmChunks.size / 2 < MAX_DECODE_FRAMES) {
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(5000)
                    if (inIdx >= 0) {
                        val inBuf = codec.getInputBuffer(inIdx)
                        if (inBuf != null) {
                            val sampleSize = extractor.readSampleData(inBuf, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                codec.queueInputBuffer(inIdx, 0, sampleSize, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                val outIdx = codec.dequeueOutputBuffer(bufferInfo, 5000)
                when {
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val newFmt = codec.outputFormat
                        if (newFmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            srcSampleRate = newFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        }
                        if (newFmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                            srcChannels = newFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
                        }
                    }
                    outIdx >= 0 -> {
                        val outBuf = codec.getOutputBuffer(outIdx)
                        if (outBuf != null && bufferInfo.size > 0) {
                            outBuf.order(ByteOrder.LITTLE_ENDIAN)
                            val shortBuf = outBuf.asShortBuffer()
                            val sampleCount = bufferInfo.size / 2
                            val frames = sampleCount / srcChannels
                            for (f in 0 until frames) {
                                val l = shortBuf.get(f * srcChannels) / 32768f
                                val r = if (srcChannels >= 2) shortBuf.get(f * srcChannels + 1) / 32768f else l
                                pcmChunks.add(l)
                                pcmChunks.add(r)
                            }
                        }
                        codec.releaseOutputBuffer(outIdx, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outputDone = true
                        }
                    }
                }
            }

            if (pcmChunks.isEmpty()) return null
            val rawStereo = pcmChunks.toFloatArray()
            val resampled = resampleStereo(rawStereo, srcSampleRate, TARGET_SAMPLE_RATE)
            val totalFrames = resampled.size / 2
            val bpm = estimateBpm(resampled, TARGET_SAMPLE_RATE)
            val peaks = extractWaveformPeaks(resampled, 96)

            return DecodedAudioTrack(
                id = uri.toString(),
                title = title,
                artist = "Local Audio File",
                sampleRateHz = TARGET_SAMPLE_RATE,
                stereoInterleaved = resampled,
                totalFrames = totalFrames,
                detectedBpm = bpm,
                waveformPeaks = peaks,
                isBuiltIn = false
            )
        } catch (_: Throwable) {
            return null
        } finally {
            try { codec?.stop(); codec?.release() } catch (_: Throwable) {}
            try { extractor.release() } catch (_: Throwable) {}
        }
    }

    fun resampleStereo(input: FloatArray, srcRate: Int, dstRate: Int): FloatArray {
        if (srcRate == dstRate || srcRate <= 0) return input
        val srcFrames = input.size / 2
        val ratio = srcRate.toDouble() / dstRate.toDouble()
        val dstFrames = (srcFrames / ratio).toInt().coerceAtLeast(1)
        val out = FloatArray(dstFrames * 2)
        for (i in 0 until dstFrames) {
            val srcPos = i * ratio
            val idx0 = srcPos.toInt().coerceIn(0, srcFrames - 1)
            val idx1 = min(idx0 + 1, srcFrames - 1)
            val frac = (srcPos - idx0).toFloat()
            out[i * 2] = input[idx0 * 2] * (1f - frac) + input[idx1 * 2] * frac
            out[i * 2 + 1] = input[idx0 * 2 + 1] * (1f - frac) + input[idx1 * 2 + 1] * frac
        }
        return out
    }

    fun extractWaveformPeaks(stereo: FloatArray, bins: Int = 96): List<Float> {
        val totalFrames = stereo.size / 2
        if (totalFrames == 0) return List(bins) { 0.1f }
        val step = max(1, totalFrames / bins)
        val peaks = ArrayList<Float>(bins)
        for (b in 0 until bins) {
            val start = b * step
            val end = min(totalFrames, start + step)
            var peak = 0f
            var i = start
            while (i < end) {
                val amp = max(abs(stereo[i * 2]), abs(stereo[i * 2 + 1]))
                if (amp > peak) peak = amp
                i += max(1, step / 24)
            }
            peaks.add(peak.coerceIn(0.05f, 1f))
        }
        return peaks
    }

    /**
     * Real onset-envelope autocorrelation BPM estimator (range 75..165 BPM).
     */
    fun estimateBpm(stereo: FloatArray, sampleRate: Int): Float {
        val totalFrames = stereo.size / 2
        if (totalFrames < sampleRate * 2) return 120f

        val hopSize = 512
        val envLen = min(totalFrames / hopSize, 1500)
        if (envLen < 64) return 120f

        val energy = FloatArray(envLen)
        for (i in 0 until envLen) {
            var sum = 0f
            val base = i * hopSize
            for (j in 0 until hopSize) {
                val mono = 0.5f * (stereo[(base + j) * 2] + stereo[(base + j) * 2 + 1])
                sum += mono * mono
            }
            energy[i] = sqrt(sum / hopSize)
        }

        val onset = FloatArray(envLen)
        for (i in 1 until envLen) {
            onset[i] = max(0f, energy[i] - energy[i - 1])
        }

        val envRate = sampleRate.toFloat() / hopSize
        val minLag = (envRate * 60f / 165f).toInt().coerceAtLeast(4)
        val maxLag = (envRate * 60f / 75f).toInt().coerceAtMost(envLen / 2)
        if (maxLag <= minLag) return 120f

        var bestLag = minLag
        var bestScore = -1.0
        for (lag in minLag..maxLag) {
            var corr = 0.0
            for (i in 0 until envLen - lag) {
                corr += onset[i] * onset[i + lag]
            }
            if (corr > bestScore) {
                bestScore = corr
                bestLag = lag
            }
        }
        val bpm = (60f * envRate) / bestLag.toFloat()
        return ((bpm * 10f).toInt() / 10f).coerceIn(75f, 165f)
    }

    private fun synthesizeBuiltInTrack(
        id: String,
        title: String,
        artist: String,
        bpm: Float,
        bars: Int,
        style: Int
    ): DecodedAudioTrack {
        val sr = TARGET_SAMPLE_RATE
        val beatsPerSec = bpm / 60.0
        val totalBeats = bars * 4
        val durationSec = totalBeats / beatsPerSec
        val totalFrames = (durationSec * sr).toInt()
        val stereo = FloatArray(totalFrames * 2)

        val bassNotes = when (style) {
            0 -> doubleArrayOf(55.0, 55.0, 65.41, 49.0)
            1 -> doubleArrayOf(43.65, 43.65, 51.91, 38.89)
            2 -> doubleArrayOf(49.0, 55.0, 41.2, 43.65)
            else -> doubleArrayOf(61.74, 61.74, 69.3, 55.0)
        }
        val leadNotes = when (style) {
            0 -> doubleArrayOf(220.0, 261.63, 329.63, 392.0)
            1 -> doubleArrayOf(174.61, 207.65, 261.63, 311.13)
            2 -> doubleArrayOf(196.0, 246.94, 293.66, 392.0)
            else -> doubleArrayOf(246.94, 293.66, 369.99, 440.0)
        }

        var kickPhase = 0.0
        var bassPhase = 0.0
        var arpPhase = 0.0
        var seed = 1337L + style * 97L

        for (i in 0 until totalFrames) {
            val t = i.toDouble() / sr
            val beatTotal = t * beatsPerSec
            val beatInBar = beatTotal % 4.0
            val beatFrac = beatTotal % 1.0
            val barIdx = (beatTotal / 4.0).toInt() % bassNotes.size
            val step16 = ((beatTotal * 4.0).toInt()) % 16

            // Kick drum on quarter notes (or trap pattern for style 2)
            val kickTrigger = if (style == 2) {
                if (step16 == 0 || step16 == 6 || step16 == 10) (beatTotal * 4.0) % 1.0 else 1.0
            } else {
                beatFrac
            }
            val kickEnv = exp(-kickTrigger * 12.0)
            val kickFreq = 42.0 + 96.0 * exp(-kickTrigger * 26.0)
            kickPhase += 2.0 * PI * kickFreq / sr
            val kick = sin(kickPhase) * kickEnv * 0.62

            // Rolling sub-bassline
            val bassFreq = bassNotes[barIdx]
            bassPhase += 2.0 * PI * bassFreq / sr
            val sidechain = 1.0 - 0.55 * exp(-beatFrac * 8.0)
            val bass = (sin(bassPhase) + 0.35 * sin(bassPhase * 2.0)) * 0.32 * sidechain

            // Arpeggiated stereo synth chord
            val arpFreq = leadNotes[step16 % leadNotes.size] * if ((step16 / 4) % 2 == 1) 1.5 else 1.0
            arpPhase += 2.0 * PI * arpFreq / sr
            val arpEnv = exp(-((beatTotal * 4.0) % 1.0) * 6.0)
            val arp = (sin(arpPhase) + 0.4 * sin(arpPhase * 2.0)) * arpEnv * 0.22

            // Crisp hi-hat offbeat
            seed = (seed * 1103515245L + 12345L) and 0x7FFFFFFFL
            val white = (seed / 1073741824.0) - 1.0
            val hatFrac = (beatTotal + 0.5) % 1.0
            val hatEnv = if (style == 3) exp(-((beatTotal * 4.0) % 1.0) * 18.0) else exp(-hatFrac * 22.0)
            val hat = white * hatEnv * 0.14

            // Snare on beats 2 and 4
            val snareBeat = if (beatInBar >= 1.0 && beatInBar < 2.0) beatInBar - 1.0
            else if (beatInBar >= 3.0) beatInBar - 3.0
            else 1.0
            val snare = (white * 0.22 + sin(2.0 * PI * 185.0 * t) * 0.14) * exp(-snareBeat * 15.0)

            val pan = 0.5 + 0.35 * sin(2.0 * PI * 0.5 * t)
            val left = (kick + bass + snare + arp * (1.1 - pan) + hat * 0.7).toFloat().coerceIn(-0.95f, 0.95f)
            val right = (kick + bass + snare + arp * (0.4 + pan) + hat * 1.1).toFloat().coerceIn(-0.95f, 0.95f)

            stereo[i * 2] = left
            stereo[i * 2 + 1] = right
        }

        return DecodedAudioTrack(
            id = id,
            title = title,
            artist = artist,
            sampleRateHz = sr,
            stereoInterleaved = stereo,
            totalFrames = totalFrames,
            detectedBpm = bpm,
            waveformPeaks = extractWaveformPeaks(stereo, 96),
            isBuiltIn = true
        )
    }
}
