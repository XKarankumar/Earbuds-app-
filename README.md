# Simple SMS Calculator

**Owner:** Hardik Kumar  
**Copyright:** © 2026 Hardik Kumar. All Rights Reserved.  
**Application ID:** `com.aistudio.smscalc.hkmpzq`  
**Platform:** Android Phones & Tablets (Jetpack Compose, Material Design 3, Adaptive Window Layouts)

---

## Offline-First & Zero-Account Architecture

Simple SMS Calculator operates **100% offline** with **no Google account, no Gmail login, no cloud sync requirement, and no analytics/tracking**. All customer ledgers, permanent transactions, audit trails, calculator history, and owner profile details are stored locally inside an **AES-256-GCM encrypted Room SQLite database** backed by Android Keystore.

### Package Structure

- `com.example.MainActivity` — Edge-to-edge `FragmentActivity` launching directly into the offline workspace (gated only if the user enables the optional 4-digit PIN lock), with adaptive `NavigationBar` (phones) / `NavigationRail` (tablets).
- `com.example.model`
  - `CalculatorModels.kt` — Recursive-descent arithmetic expression evaluator (`+`, `-`, `×`, `÷`, `%`, `()`, `√`, `x²`), unit conversion engine (`KL`, `Litre`, `mL`, `Gallon`, `m³`, `MT`, `Quintal`, `KG`, `GM`, `mg`, `lb`, `oz`, `KM`, `m`, `cm`, `mm`, `ft`, `in`, `yd`, `mi`, `sq.ft`, `sq.m`, `Gaj`, `Acre`, `Hectare`, `Bigha`, `°C`, `°F`, `K`), and financial calculators (`GST`, `Simple & Compound Interest`, `Discount`, `EMI`).
  - `LedgerModels.kt` — Offline domain models (`UserProfile`, `Customer`, `LedgerTransaction`, `AuditLog`, `CalcHistoryEntry`, `LocalBackupSnapshot`) and `AppThemeId` enum.
- `com.example.data.local` & `com.example.data`
  - `AppDatabase.kt`, `LedgerDao.kt`, `EncryptedLedgerEntities.kt` — Room SQLite database with KSP codegen storing AES-256-GCM encrypted payloads and SHA-256 integrity digests.
  - `LedgerRepository.kt` — Reactive `Flow` queries, atomic Udhaar/Jama balance updates, immutable audit log writes, and local encrypted backup restore.
- `com.example.security`
  - `CryptoVault.kt` — Android Keystore `AES/GCM/NoPadding` encryption for database records and PIN secrets, salted `PBKDF2WithHmacSHA256` PIN verification, SHA-256 integrity digests, and portable `.sscbak` backup encryption/decryption.
  - `AppPreferencesRepository.kt` — DataStore preferences for themes, glassmorphism opacity, optional PIN lock, and optional feature toggles.
  - `BiometricHelper.kt` — `BiometricManager` availability checks and optional `BiometricPrompt` fingerprint/face authentication.
- `com.example.permissions`
  - `PermissionManager.kt` — Evaluates `Granted`, `Not Granted`, and `Not Required` states across SIM SMS, Contacts, Camera, Notifications, Local Scoped Storage / Photo Picker, Biometrics, Internet (`Not Required`), and Bluetooth (`Not Required`).
- `com.example.sms`, `com.example.contacts`, `com.example.notifications`
  - `SmsReceiptHelper.kt` — Formats payment receipts and dispatches via the phone's SIM `SmsManager` (when enabled and permitted) or Android's native `ACTION_SENDTO` SMS interface.
  - `ContactsHelper.kt` — Queries `ContactsContract.CommonDataKinds.Phone` only when `READ_CONTACTS` is granted.
  - `PaymentReminderReceiver.kt` — Schedules and displays local offline payment reminder notifications.
- `com.example.export`
  - `ReportExporter.kt` — Generates multi-page PDF statements (`PdfDocument`), UTF-8 BOM CSV files, native OOXML Excel `.xlsx` workbooks (`ZipOutputStream`), and AES-256-GCM encrypted `.sscbak` backups saved to local device storage via Storage Access Framework (`CreateDocument` / `OpenDocument`) or app-private snapshots.
- `com.example.ui`
  - `theme/` — 5 selectable themes (`Obsidian Emerald`, `Alabaster Ivory`, `Royal Indigo`, `Saffron Ledger`, `Cyber Slate`) with glassmorphism tokens.
  - `components/` — Glassmorphic containers, on-device CameraX + ML Kit QR/Bill Scanner + Photo Picker, and Contacts Picker sheet.
  - `screens/` — `AppLockScreen` (optional PIN), `CalculatorScreen`, `LedgerScreen`, `ToolsScreen`, `HistoryAuditScreen`, and `SettingsScreen`.

---

## Security & Privacy Highlights

- **Zero Google Account / Zero Network Permission:** `INTERNET` and `ACCESS_NETWORK_STATE` are not requested in `AndroidManifest.xml`; no Google Sign-In, Firebase, or analytics SDKs are included.
- **Encrypted Local Storage:** All Room database records and local `.sscbak` backup archives use AES-256-GCM encryption.
- **Scoped Storage & Zero Media Permissions:** Uses Android Photo Picker (`PickVisualMedia`) and Storage Access Framework (`CreateDocument` / `OpenDocument`) with zero broad storage permissions.
