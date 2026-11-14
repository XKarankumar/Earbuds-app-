# FITNESS HUB (v1.0)

**Developer / Copyright:** © 2026 Hardik Kumar. All Rights Reserved.  
**Platform:** Native Android (Kotlin, Jetpack Compose, Room, AndroidKeyStore AES-256-GCM)  
**Design:** Premium AMOLED Black (`#000000`) iOS-Inspired White Glassmorphism + Light Glass Theme

---

## Core Modules & Features

1. **Home:** Daily biometric rings (Calories, Protein, Water), 3D hero banner, active member & revenue KPIs, and 1-tap access to all 10 screens.
2. **Complete 3D Exercise Library:** 33 exercises across all **10 muscle groups** (*Chest, Back, Shoulders, Biceps, Triceps, Legs, Abs, Forearms, Calves, Cardio*). Every exercise includes an interactive 360° 3D perspective-projected biomechanical animation (`Exercise3DCanvas`), target muscle glowing highlights, equipment info, difficulty level, posture guide, step-by-step instructions, sets/reps, live countdown **Rest Timer**, common mistakes, and safety instructions.
3. **Personal Workout Planner:** Daily planner (Mon–Sun), custom split routine builder, session completion & history log, and WorkManager workout reminders (`POST_NOTIFICATIONS` gated on Android 13+).
4. **Diet, Hydration & Biometrics:** Daily meal planner, Mifflin-St Jeor Calories & Protein calculator, BMI calculator with healthy weight range, Weight progress line chart, Workout volume bar chart, and Water tracker.
5. **Members & Mobile Number System:** Member registration with international country code support, 6-digit OTP verification flow, Unique Member ID (`FH-MEM-XXXX`), emergency contact, zero-permission Android Photo Picker (`PickVisualMedia`), Member QR Pass, and encrypted PII storage.
6. **Attendance & QR Check-In:** CameraX + ML Kit Barcode Scanning QR check-in, manual attendance marking, and **Missed Gym Detection** comparing scheduled gym days against attendance after the configured deadline.
7. **Gym Management:** Admin dashboard, monthly/quarterly/yearly fee collection, pending fee & membership expiry alerts, trainer management, monthly revenue charts, **PDF Fee Receipts** (with QR verification) & **Monthly Revenue PDF Reports**.
8. **Token-Based App Access & Token Management:** Zero hardcoded master tokens; dynamic `SecureRandom` token generation, optional HTTPS backend verification, one-device binding (`SHA-256` device hash), renewal, revocation of compromised tokens, brute-force invalid token lockout, audit history, and AndroidKeyStore-encrypted time-limited offline leases.
9. **Authorized SMS Gateway System:** Automated event notifications (Attendance Missing, Workout Missed, Membership Expiry, Fee Pending, Payment Confirmation, New Member Registration, Token Expiry) over HTTPS Gateway or zero-permission SMS Intent handoff, with per-member opt-in preferences, duplicate suppression (`dedupeKey`), and delivery status tracking.
10. **Profile & Settings:** AMOLED Black / Light Glass theme switcher, gym & deadline configuration, HTTPS gateway setup, Privacy Policy, and one-tap complete local data deletion.

## Documentation & Reports
- [Android Build Instructions (Debug & Signed Release APK)](docs/ANDROID_BUILD.md)
- [Database & AndroidKeyStore Encryption Config](docs/DATABASE_CONFIG.md)
- [SMS & Token HTTPS Gateway Setup](docs/API_SETUP.md)
- [Privacy Policy & Data Deletion](docs/PRIVACY_POLICY.md)
- [Security Scan Report](docs/SECURITY_SCAN_REPORT.md)
- [Testing Report](docs/TESTING_REPORT.md)
