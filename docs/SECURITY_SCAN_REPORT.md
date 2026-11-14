# FITNESS HUB — Security & Policy Scan Report

**App Name:** FITNESS HUB (v1.0)  
**Application ID:** `com.aistudio.fitnesshub.hkprod`  
**Developer:** Hardik Kumar  
**Date:** November 14, 2026  

---

## 1. Static Code & Android Lint Verification
- **Debug Lint (`lint_applet` / `:app:lintDebug`):** **PASSED** (`BUILD SUCCESSFUL`, **0 errors**)
- **Release Lint (`:app:lintVitalRelease`):** **PASSED** (`BUILD SUCCESSFUL`, **0 errors**)
- **Grep Audit (`app/src/main/`):**
  - Cleartext `http://` endpoints in source/config: **0 found** (`network_security_config.xml` enforces `cleartextTrafficPermitted="false"`).
  - Dynamic Code Loading (`DexClassLoader`, `PathClassLoader`, `System.load`): **0 found**.
  - Prohibited broad storage/SMS permissions (`READ_EXTERNAL_STORAGE`, `READ_MEDIA_IMAGES`, `SEND_SMS`, `READ_SMS`, `QUERY_ALL_PACKAGES`, `SYSTEM_ALERT_WINDOW`, `REQUEST_INSTALL_PACKAGES`): **0 found**.
  - Hardcoded master tokens, API keys, or keystore passwords in APK source: **0 found**.

## 2. Cryptographic & Access Controls Verified
1. **AndroidKeyStore AES-256-GCM (`KeystoreCryptoManager`):**
   - Key alias `FitnessHubMasterAesKey_v1` protects member phone numbers, emergency contact details, SMS Gateway API keys, and offline token authorization leases.
2. **Token-Based App Access (`TokenSecurityEngine`):**
   - Unique tokens generated dynamically via `SecureRandom` + SHA-256 checksum; only SHA-256 token digests stored in Room.
   - Supports optional HTTPS backend verification, SHA-256 one-device binding, revocation of compromised tokens, 5-attempt brute-force lockout, and time-limited (1–72h) AES-GCM cached offline leases.
3. **Cloud Backup Hardening:**
   - `android:allowBackup="false"` in `AndroidManifest.xml`, plus explicit `<exclude>` rules for `database`, `sharedpref`, and `file` domains in `backup_rules.xml` and `data_extraction_rules.xml`.
4. **Least-Privilege Runtime Permissions:**
   - Photo selection uses zero-permission Android Photo Picker (`PickVisualMedia`).
   - `CAMERA` requested strictly when opening QR Check-In Scanner.
   - `POST_NOTIFICATIONS` requested on Android 13+ strictly when enabling workout reminders.
   - SMS dispatch uses Authorized HTTPS Gateway or zero-permission `ACTION_SENDTO` (`smsto:`).
