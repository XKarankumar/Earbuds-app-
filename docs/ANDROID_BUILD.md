# Android APK & App Bundle Build Instructions

## Prerequisites
- Android Studio Ladybug / Meerkat or Gradle 8.13+
- JDK 11+ (`JAVA_HOME` configured)
- Android SDK 36 (`compileSdk = 36`, `minSdk = 24`, `targetSdk = 36`)
- `app/google-services.json` (already provisioned for project `gen-lang-client-0047373257`)

## Build Commands
1. **Debug APK:**
   ```bash
   gradle :app:assembleDebug
   ```
   Output: `app/build/outputs/apk/debug/app-debug.apk`

2. **Run Unit & Robolectric Tests:**
   ```bash
   gradle :app:testDebugUnitTest
   ```

3. **Release AAB (Google Play):**
   Set `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` environment variables, then run:
   ```bash
   gradle :app:bundleRelease
   ```
   Output: `app/build/outputs/bundle/release/app-release.aab`
