# Android Permissions & Privacy Documentation

**Desi Calculator AI PRO V2.0** follows least-privilege and Google Play Developer Program policies:

| Permission / API | Type | Purpose & User Control |
|---|---|---|
| `INTERNET` | Install-time (Normal) | Used solely by Firebase AI Logic (`Firebase.ai`) and App Check for Online Gemini AI queries. Offline Smart Engine and Room Ledger work 100% offline. |
| `POST_NOTIFICATIONS` | Runtime (Android 13+ / API 33+) | Requested only when the user opts in via Profile -> Notifications. Powers transaction alerts, payment reminder notifications, and daily summaries across 3 Android `NotificationChannel`s. |
| `SEND_SMS` | Runtime (Dangerous, Optional) | Strictly optional. Requested only when the user explicitly grants SMS consent to send direct or scheduled payment reminders via `SmsManager`. A zero-permission **Manual SMS Composer** (`Intent.ACTION_SENDTO` `smsto:`) fallback is always available. Never sends messages silently without consent. |
| Photo Picking (`PickVisualMedia`) | Zero-Permission System Picker | Customer & business profile avatars use Android Photo Picker (`ActivityResultContracts.PickVisualMedia`). `READ_EXTERNAL_STORAGE` and `READ_MEDIA_IMAGES` are **not** requested. |
| Backup & CSV Export (`OpenDocument` / `CreateDocument`) | Zero-Permission SAF | JSON backups and CSV ledger exports use Storage Access Framework document pickers. |
