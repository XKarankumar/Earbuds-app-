package com.example.hometheatre.dolby

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaCodecList
import android.os.Build
import com.example.hometheatre.core.DolbyCapabilityStatus

/**
 * Inspects platform MediaCodec decoders, AudioDeviceInfo sink encodings, HDMI ARC/eARC routes,
 * and AudioTrack direct passthrough support to report real, certified Dolby Atmos / AC-3 / E-AC-3 / AC-4
 * availability without ever faking or simulating Dolby certification.
 */
class DolbyCapabilityDetector(context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun inspectDolbyCapabilities(): DolbyCapabilityStatus {
        val detectedCodecs = mutableSetOf<String>()
        var ac3Decoder = false
        var eac3Decoder = false
        var atmosJocDecoder = false
        var ac4Decoder = false

        try {
            val codecInfos = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
            for (info in codecInfos) {
                if (info.isEncoder) continue
                for (mime in info.supportedTypes) {
                    when (mime.lowercase()) {
                        "audio/ac3" -> {
                            ac3Decoder = true
                            detectedCodecs.add("Dolby Digital (AC-3) [${info.name}]")
                        }
                        "audio/eac3" -> {
                            eac3Decoder = true
                            detectedCodecs.add("Dolby Digital Plus (E-AC-3) [${info.name}]")
                        }
                        "audio/eac3-joc" -> {
                            atmosJocDecoder = true
                            detectedCodecs.add("Dolby Atmos (E-AC-3 JOC) [${info.name}]")
                        }
                        "audio/ac4" -> {
                            ac4Decoder = true
                            detectedCodecs.add("Dolby AC-4 [${info.name}]")
                        }
                        "audio/true-hd" -> {
                            detectedCodecs.add("Dolby TrueHD [${info.name}]")
                        }
                    }
                }
            }
        } catch (_: Throwable) {
            // Codec inspection restricted on some environments
        }

        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        var hdmiArcOrEarc = false
        var sinkAtmosPassthrough = false

        for (device in outputs) {
            val type = device.type
            if (type == AudioDeviceInfo.TYPE_HDMI ||
                type == AudioDeviceInfo.TYPE_HDMI_ARC ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && type == AudioDeviceInfo.TYPE_HDMI_EARC)
            ) {
                hdmiArcOrEarc = true
            }

            for (enc in device.encodings) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    when (enc) {
                        AudioFormat.ENCODING_E_AC3_JOC,
                        AudioFormat.ENCODING_AC4,
                        AudioFormat.ENCODING_DOLBY_TRUEHD -> {
                            sinkAtmosPassthrough = true
                        }
                    }
                }
            }
        }

        val directAtmosSupported = checkDirectAtmosPlayback()

        val banner = when {
            (atmosJocDecoder || ac4Decoder) && (sinkAtmosPassthrough || directAtmosSupported) ->
                "Licensed Dolby Atmos decoder & direct passthrough route detected."
            atmosJocDecoder || ac4Decoder ->
                "Platform Dolby decoder present. Connect an HDMI ARC/eARC or certified Atmos sink for passthrough."
            ac3Decoder || eac3Decoder ->
                "Standard AC-3 / E-AC-3 decoder found, but Dolby Atmos (E-AC-3 JOC / AC-4) is unavailable on this device."
            else ->
                "No licensed Dolby Atmos decoder or passthrough sink detected on this device. Custom DSP will not be labeled as Dolby Atmos."
        }

        return DolbyCapabilityStatus(
            dolbyAtmosDecoderPresent = atmosJocDecoder || ac4Decoder,
            dolbyAc3DecoderPresent = ac3Decoder,
            dolbyEac3DecoderPresent = eac3Decoder,
            dolbyAc4DecoderPresent = ac4Decoder,
            sinkSupportsAtmosPassthrough = sinkAtmosPassthrough,
            hdmiArcOrEarcDetected = hdmiArcOrEarc,
            activeRouteSupportsDirectAtmos = directAtmosSupported,
            detectedCodecs = detectedCodecs.toList().sorted(),
            honestyStatusBanner = banner
        )
    }

    private fun checkDirectAtmosPlayback(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                .build()
            val jocFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_E_AC3_JOC)
                .setSampleRate(48000)
                .setChannelMask(AudioFormat.CHANNEL_OUT_5POINT1)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                AudioManager.getDirectPlaybackSupport(jocFormat, attrs) != AudioManager.DIRECT_PLAYBACK_NOT_SUPPORTED
            } else {
                @Suppress("DEPRECATION")
                AudioTrack.isDirectPlaybackSupported(jocFormat, attrs)
            }
        } catch (_: Throwable) {
            false
        }
    }
}
