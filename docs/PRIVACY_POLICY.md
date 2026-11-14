# Privacy Policy — FITNESS HUB

**Effective Date:** November 14, 2026  
**Developer:** Hardik Kumar  
**Application:** FITNESS HUB (Version 1.0)

## 1. Data Minimization & Security
- **HTTPS Only:** `network_security_config.xml` sets `cleartextTrafficPermitted="false"`.
- **No Tracking or Spyware:** Zero analytics SDKs, zero background location tracking, zero contact harvesting.
- **No Hardcoded Secrets:** Zero master tokens, API keys, or keystore passwords are embedded in the APK.

## 2. Permissions Used Only When Needed
- **Photo Selection:** Uses the zero-permission Android Photo Picker (`ActivityResultContracts.PickVisualMedia`). No `READ_EXTERNAL_STORAGE` or `READ_MEDIA_IMAGES` permissions are declared.
- **Camera (`CAMERA`):** Requested at runtime only when the user opens the QR Check-In Scanner.
- **Notifications (`POST_NOTIFICATIONS`):** Requested on Android 13+ only when the user enables daily workout reminders.
- **SMS Dispatch:** Uses configured Authorized HTTPS SMS Gateway or zero-permission Android SMS Intent handoff (`ACTION_SENDTO smsto:`). `SEND_SMS` / `READ_SMS` permissions are not requested.

## 3. Data Encryption & User Deletion Rights
- Member phone numbers, emergency contacts, gateway credentials, and cached authorization leases are encrypted using **AndroidKeyStore AES-256-GCM**.
- Users can erase all local Room and DataStore records immediately via **Settings → Privacy Policy & Data Deletion → Delete All Data**.
