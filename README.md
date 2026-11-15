# Smart Cool & Battery Saver Pro

A native Kotlin + Jetpack Compose thermal, battery, and performance companion built for **Vivo X300** and **Android 8.0 Oreo (API 26) through Android 16 (API 36)**, designed with a responsive **iOS-inspired Liquid Glass** interface.

## Core Capabilities

### 1. iOS-Inspired Liquid Glass UI & 7 Live Themes
- **7 Built-in Themes with Live Preview & Permanent DataStore Persistence**:
  1. **iOS Liquid Glass** (Default translucent white, brushed silver, and ice-blue glassmorphism)
  2. **Midnight AMOLED** (True `#000000` OLED black with frosted obsidian cards)
  3. **Pearl Light** (Porcelain white with cool platinum glass highlights)
  4. **Ice Blue** (Glacial frost canvas with crisp cyan highlights)
  5. **Silver Glass** (Anodized titanium-silver brushed surfaces)
  6. **Ocean Blue** (Deep sapphire abyssal glass with bioluminescent aqua)
  7. **Custom Theme** (Live preview, Light/Dark toggle, custom accent color swatches, glass opacity, and glow sliders)
- **Adaptive Layouts**: Floating translucent bottom glass bar on phones and `NavigationRail` + centered max-width panels on tablets and foldables (`>= 600dp`), full `enableEdgeToEdge()` and `WindowInsets.safeDrawing` support.

### 2. Honest Smart Cooling
- **Live Battery Thermistor**: Reads real temperature via `BatteryManager.EXTRA_TEMPERATURE` (`°C` / `°F` toggle).
- **Android Thermal API**: Uses `PowerManager.getCurrentThermalStatus()` + `OnThermalStatusChangedListener` (Android 10 / API 29+) and `getThermalHeadroom()` (Android 11 / API 30+), with graceful fallback on Android 8.0–9.0.
- **Temperature History Graph**: Plots real persisted readings recorded over time in a Compose `Canvas` area graph.
- **Charging Heat Warnings & High-Temp Notifications**: Configurable thresholds with real Android `NotificationChannel` alerts (`POST_NOTIFICATIONS` on Android 13+).
- **No Fake Claims**: Never fabricates CPU die temperatures or claims software can physically cool hardware.

### 3. Battery Management
- Live battery percentage, charging/discharging state, plug source (`AC`, `USB`, `Wireless`), voltage (`V` / `mV`), reported health, technology, and HAL-reported current (`mA`), charge counter (`mAh`), and cycle count (Android 14+).
- Persisted battery level history chart, dynamic lithium-ion charging guidance, low-battery alerts, and direct Android **Battery Saver** / **Battery Usage** shortcuts.

### 4. Performance Monitor
- Live **Available / Used / Total RAM** and low-memory threshold monitoring via `ActivityManager.getMemoryInfo()`.
- Hardware & SoC profile (`Build.MANUFACTURER`, `Build.MODEL`, CPU cores, Android API level).
- **UsageStatsManager Insights** (strictly user-consented via `Settings.ACTION_USAGE_ACCESS_SETTINGS`): displays daily foreground screen time, top foreground apps, foreground-service activity (API 29+), and quick shortcuts to System App Info.
- **Honest Memory Science**: Never claims that killing background apps or "cleaning RAM" improves battery life.

### 5. Smart Modes & Daily Reports
- **Normal Mode**: Balanced telemetry polling.
- **Cooling Mode**: Faster 2-second foreground thermal polling + heat-reduction settings shortcuts.
- **Extreme Battery Saver**: Direct Android Battery Saver controls, battery optimization shortcuts, and 1-tap `Midnight AMOLED` OLED black mode.
- **Daily Battery & Screen Report**: Real daily min/avg/peak temperature, battery range, peak OS thermal status, and screen time summary.

---

## Project Structure

```text
app/src/main/java/com/example/
├── MainActivity.kt                              # Navigation, edge-to-edge glass bar, lifecycle polling
└── smartcool/
    ├── model/SmartCoolModels.kt                 # Domain & telemetry models
    ├── data/SmartCoolPreferencesStore.kt        # Jetpack DataStore settings & history store
    ├── monitor/
    │   ├── BatteryMonitor.kt                    # BatteryManager & sticky broadcast telemetry
    │   ├── ThermalMonitor.kt                    # PowerManager Thermal API & safe cooling advice
    │   └── PerformanceMonitor.kt                # ActivityManager RAM & UsageStatsManager insights
    ├── notifications/AlertNotificationManager.kt# Thermal & battery NotificationChannels
    ├── util/SystemShortcuts.kt                  # Safe Android Settings & Battery Saver intents
    └── ui/
        ├── theme/LiquidTheme.kt                 # 7 Liquid Glass themes & animated tokens
        ├── components/LiquidGlassComponents.kt  # Glass cards, ring gauges, Canvas charts
        ├── viewmodel/SmartCoolViewModel.kt      # Lifecycle-aware state orchestration
        └── screens/
            ├── OverviewScreen.kt
            ├── CoolingScreen.kt
            ├── BatteryScreen.kt
            ├── PerformanceScreen.kt
            ├── ModesAndReportsScreen.kt
            └── SettingsAndThemesScreen.kt
```

---

## Building & Running (Android Studio & CLI)

### Prerequisites
- Android Studio Ladybug / Meerkat or newer
- JDK 17+
- Android SDK Platform 36 (`minSdk = 26`, `targetSdk = 36`)

### Build Commands

```bash
# Run unit and Robolectric UI tests
gradle :app:testDebugUnitTest

# Build Debug APK
gradle :app:assembleDebug

# Build Release APK (requires KEYSTORE_PATH, STORE_PASSWORD, KEY_PASSWORD env vars)
gradle :app:assembleRelease
```

Outputs:
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`

## GitHub Actions CI/CD

The included `.github/workflows/android-build.yml` workflow automatically:
1. Runs `:app:testDebugUnitTest` on every push and pull request to `main`.
2. Builds and uploads `app-debug.apk` as a downloadable artifact (`smart-cool-battery-saver-pro-debug-apk`).
3. Builds and uploads the Release APK (`smart-cool-battery-saver-pro-release-apk`). To sign releases with your production upload key, configure repository secrets `SIGNING_KEYSTORE_BASE64`, `STORE_PASSWORD`, and `KEY_PASSWORD`.

## Privacy & Permissions

- **Zero Ads & 100% Offline**: No network permission (`INTERNET`) is requested.
- **`POST_NOTIFICATIONS`** (Android 13+): Optional, used solely for user-configured high-temp, charging-heat, and low-battery alerts.
- **`PACKAGE_USAGE_STATS`**: Optional, explicitly granted by the user in Android Usage Access Settings to view foreground screen time and app insights locally.
