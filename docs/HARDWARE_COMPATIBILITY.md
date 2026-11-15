# Hardware Compatibility & Capability Honesty Guide

**ULTIMATE HOME THEATRE PRO** enforces strict hardware honesty across Android 8.0 Oreo (API 26) through Android 16+ (API 36).

## 1. Android API Level Feature Matrix

| Feature | API 26–27 (Android 8.0–8.1) | API 28–30 (Android 9–11) | API 31–32 (Android 12/12L) | API 33+ (Android 13+) |
|---|---|---|---|---|
| 10-Band Biquad DSP & Soft-Clip Limiter | Full Software DSP + Legacy `Equalizer`/`BassBoost` | + `DynamicsProcessing` API | + `DynamicsProcessing` | + `DynamicsProcessing` |
| Dual-Deck DJ Audio Mixer (44.1 kHz) | `AudioTrack` Low-Latency | `AudioTrack` Low-Latency | `AudioTrack` Low-Latency | `AudioTrack` Low-Latency |
| Android Spatializer API | Software Virtual Surround DSP Fallback | Software Virtual Surround DSP Fallback | `AudioManager.getSpatializer()` (API 32+) | Spatializer + Head Tracking |
| Dolby Atmos Passthrough Detection | `MediaCodecList` + `AudioDeviceInfo` | + `AudioTrack.isDirectPlaybackSupported` (API 29+) | + HDMI eARC `TYPE_HDMI_EARC` | Full Codec + Sink Inspection |
| Bluetooth Headphone / BLE RGB Scan | `BLUETOOTH` / `BLUETOOTH_ADMIN` | `BLUETOOTH` / `BLUETOOTH_ADMIN` | `BLUETOOTH_SCAN` + `BLUETOOTH_CONNECT` | Scoped Runtime Permissions |

## 2. Active Noise Cancellation (ANC)

Android's public framework does not provide a universal cross-vendor ANC toggle. The `:device-compatibility` module inspects connected Bluetooth A2DP/LE Audio, USB-C, and wired headsets and clearly states whether proprietary OEM SDK integration is unavailable. Software EQ is never disguised as hardware ANC.

## 3. Dolby Atmos & HDMI ARC/eARC

The `:dolby-integration` module queries the platform `MediaCodecList` for `audio/ac3`, `audio/eac3`, `audio/eac3-joc`, `audio/ac4`, and `audio/true-hd`, and checks connected `AudioDeviceInfo` sinks (`TYPE_HDMI`, `TYPE_HDMI_ARC`, `TYPE_HDMI_EARC`) and `AudioTrack.isDirectPlaybackSupported()`. If the device or sink lacks licensed Dolby support, the UI reports it explicitly.

## 4. RGB Hardware Controllers

Physical LED output requires one of:
- **WLED** controller on local Wi-Fi (DRGB UDP port `21324`)
- **ESP32** firmware listening for binary RGB frames on UDP port `4210`
- **Bluetooth LE** RGB controller exposing a writable GATT characteristic

Without a connected controller, RGB Studio runs strictly in **Preview Only** mode.
