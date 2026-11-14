# FITNESS HUB — Android Build Instructions

**Developer:** Hardik Kumar  
**Version:** 1.0 (`versionCode = 1`, `versionName = "1.0"`)  
**Package / Application ID:** `com.aistudio.fitnesshub.hkprod`  
**SDK:** `minSdk = 24`, `compileSdk = 36`, `targetSdk = 36`

## 1. Prerequisites
- Android Studio Ladybug / Meerkat or Gradle 9.3.1+
- JDK 17+ (`JAVA_HOME` configured)
- Android SDK 36

## 2. Build Debug APK
Using Gradle Wrapper (in Android Studio / local machine):
```bash
./gradlew assembleDebug
```
Or using pre-installed `gradle` in CI/container environments:
```bash
gradle :app:assembleDebug
```
**Output:** `app/build/outputs/apk/debug/app-debug.apk`

## 3. Build Signed Release APK
Set the upload keystore environment variables (never hardcode passwords in `build.gradle.kts`):
```bash
export KEYSTORE_PATH="/path/to/my-upload-key.jks"
export STORE_PASSWORD="your-store-password"
export KEY_PASSWORD="your-key-password"
```
Then build the signed Release APK:
```bash
./gradlew assembleRelease
```
(Or `gradle :app:assembleRelease` in container environments.)  
**Output:** `app/build/outputs/apk/release/app-release.apk`

## 4. Run Unit & Robolectric Tests
```bash
./gradlew testDebugUnitTest
```
(Or `gradle :app:testDebugUnitTest`.)
