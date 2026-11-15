# ULTIMATE HOME THEATRE PRO

A modular Kotlin + Jetpack Compose Android audio engineering suite built for Android 8.0 Oreo (API 26) through Android 16+ (API 36), featuring real-time DSP processing, dual-deck DJ mixing, WLED/ESP32/BLE RGB lighting studio, honest ANC & Dolby Atmos capability detection, and Android Spatializer 3D surround integration.

## Key Features

- **5 Console Themes + Custom Theme Editor**: Premium Dark, Monochrome B&W, Silver Glass, RGB Neon (synced to live LED preview), and Custom Theme Editor with DataStore persistence.
- **Extra Bass, Subwoofer & 10-Band EQ (`:audio-engine`)**: Real-time 10-band biquad EQ (`31 Hz – 16 kHz`), Bass Booster, Subwoofer LFE shelf, Deep Bass (40 Hz), Cinema Bass (58 Hz), Mid/Treble shelves, built-in + user-saved EQ presets, Media3 `ExoPlayer` `AudioProcessor` pipeline, and soft-knee `-0.3 dBFS` anti-clipping limiter.
- **Extra Volume & Safe Listening**: Synchronized Android `STREAM_MUSIC` hardware volume control, digital loudness gain (`0–12 dB`) with clipping guard, and configurable Safe Volume Warning (`>= 80%`).
- **Dual-Deck DJ Console (`:dj-console`)**: Two independent 44.1 kHz live audio decks (built-in studio stems + local MP3/WAV/AAC/FLAC decoding via `MediaExtractor`/`MediaCodec`), Play/Pause, Cue, interactive vinyl Scratch wheel, 1/2/4/8-beat Loop, 4 Hot Cues per deck, onset autocorrelation BPM detection, Tempo & Beat Sync, 3-band per-deck EQ, LPF/HPF resonant filter, equal-power Crossfader, Echo, Reverb, and **16 live synthesized Performance Pads**.
- **RGB Lighting Studio (`:rgb-controller`)**: **108 procedural RGB effects** across 9 categories, Music Reactive & Bass Beat Sync driven by real-time FFT/RMS telemetry, custom RGB colors, saved presets, **WLED DRGB UDP (port 21324)**, **ESP32 UDP (port 4210)**, and **Bluetooth LE GATT** controller discovery. Explicitly reports Preview-Only mode whenever no physical RGB controller is connected.
- **Honest ANC, Spatial Audio & Dolby Atmos (`:device-compatibility`, `:spatial-audio`, `:dolby-integration`)**:
  - **ANC**: Detects Bluetooth/USB/wired headsets and reports OEM SDK availability honestly; never simulates hardware ANC.
  - **Spatial Audio**: Integrates `AudioManager.getSpatializer()` (API 32+) and Head Tracking (API 33+), with Cinema/Music/Gaming modes and a transparent Virtual Surround DSP fallback on Android 8.0–12.
  - **Dolby Atmos**: Inspects `MediaCodecList`, `AudioDeviceInfo` encodings, HDMI ARC/eARC routes, and `AudioTrack.isDirectPlaybackSupported()` (API 29+) without ever faking Dolby certification.

## Project Module Architecture

```
ultimate-home-theatre-pro/
├── app/                   # Jetpack Compose UI, Navigation, Media3 MediaSessionService, DataStore
├── core/                  # Shared domain models, themes, EQ catalog, future expansion contracts
├── audio-engine/          # Media3 AudioProcessor, 10-band biquad DSP, soft-clip limiter, FFT analyzer
├── dj-console/            # Dual-deck live PCM mixer, MediaCodec decoder, BPM detector, 16 pads
├── rgb-controller/        # 108 RGB effects, WLED DRGB UDP, ESP32 UDP, BLE GATT controller
├── device-compatibility/  # AudioDeviceCallback, Bluetooth/USB/AUX/HDMI/ANC inspection
├── spatial-audio/         # Android Spatializer API 32+ & Head Tracking API 33+ integration
├── dolby-integration/     # Licensed Dolby AC-3 / E-AC-3 JOC / AC-4 / HDMI ARC/eARC detector
├── gradle/                # Version catalog (libs.versions.toml)
└── .github/workflows/     # Automated CI test + Debug APK workflow (android-build.yml)
```

## Installation & Building Debug APK

### Prerequisites
- Android Studio Ladybug / Meerkat or newer
- JDK 17+
- Android SDK Platform 36 (`minSdk = 26`, `targetSdk = 36`)

### Build via Command Line

```bash
# Run all unit and Robolectric tests across modules
gradle testDebugUnitTest

# Assemble the Debug APK
gradle :app:assembleDebug
```

The output APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`

Install on a connected device:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Additional Documentation

- [Hardware Compatibility Matrix](docs/HARDWARE_COMPATIBILITY.md)
- [Future Expansion & Module Guide](docs/FUTURE_UPDATES.md)
