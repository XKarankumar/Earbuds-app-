package com.example.hometheatre.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.example.hometheatre.core.AudioTelemetrySnapshot
import java.nio.ByteBuffer

/**
 * Custom Media3 AudioProcessor injecting TheatreDspChain (10-band EQ, Sub/Bass, Virtualizer,
 * Soft-Clip Limiter) and RealTimeAudioAnalyzer directly into ExoPlayer's audio pipeline.
 */
@UnstableApi
class DspMedia3AudioProcessor(
    private val dspChain: TheatreDspChain,
    private val analyzer: RealTimeAudioAnalyzer,
    private val onTelemetry: (AudioTelemetrySnapshot) -> Unit
) : BaseAudioProcessor() {

    private var scratchStereo = FloatArray(2048)

    @Volatile
    var currentSourceLabel: String = "Media3 Stream"

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        if (inputAudioFormat.channelCount != 2 && inputAudioFormat.channelCount != 1) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        return AudioProcessor.AudioFormat(
            inputAudioFormat.sampleRate,
            2,
            C.ENCODING_PCM_16BIT
        )
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val inChannels = inputAudioFormat.channelCount.coerceAtLeast(1)
        val frameCount = remaining / (2 * inChannels)
        if (frameCount <= 0) {
            inputBuffer.position(inputBuffer.limit())
            return
        }

        if (scratchStereo.size < frameCount * 2) {
            scratchStereo = FloatArray(frameCount * 2)
        }

        for (i in 0 until frameCount) {
            val l = inputBuffer.short / 32768f
            val r = if (inChannels >= 2) inputBuffer.short / 32768f else l
            scratchStereo[i * 2] = l
            scratchStereo[i * 2 + 1] = r
        }

        dspChain.processStereoInPlace(scratchStereo, frameCount)

        val outBytes = frameCount * 4
        val outBuf = replaceOutputBuffer(outBytes)
        for (i in 0 until frameCount * 2) {
            val clamped = (scratchStereo[i] * 32767f).coerceIn(-32768f, 32767f).toInt().toShort()
            outBuf.putShort(clamped)
        }
        outBuf.flip()

        val telemetry = analyzer.analyzeInterleavedStereo(
            interleaved = scratchStereo,
            frameCount = frameCount,
            sampleRateHz = inputAudioFormat.sampleRate,
            limiterGainReductionDb = dspChain.limiter.lastGainReductionDb,
            clippingProtected = dspChain.limiter.protectionEngaged,
            isPlaying = true,
            sourceLabel = currentSourceLabel
        )
        onTelemetry(telemetry)
    }
}
