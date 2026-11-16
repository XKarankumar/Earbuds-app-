# Desi Calculator AI PRO V2.0

**Developed by Hardik Kumar**  
© 2026 Hardik Kumar. All Rights Reserved.  
Repository: `desi-calculator-ai-pro`

Desi Calculator AI PRO V2.0 is a dual-platform smart Udhar–Jama Khata ledger, advanced Indian business calculator, bilingual Hindi/English AI assistant, and SMS reminder suite delivered in two complete editions:

1. **Native Android Application** (`app/`, Kotlin, Jetpack Compose Material 3, Room Database, DataStore, WorkManager, Android Notifications, SmsManager/SMS Composer, Firebase AI Logic + App Check).
2. **Modular Responsive HTML Web App** (`web/`, split cleanly into `index.html`, `css/styles.css`, and ES module files `js/storage.js`, `js/calculator.js`, `js/ledger.js`, `js/ai_assistant.js`, `js/sms.js`, `js/app.js`).

---

## Key Features

- **Dashboard**: Total Customers, Total Udhar, Total Jama, Remaining Balance (`Lena Hai` / `Dena Hai`), Today's Transactions, Pending Payments, Recent History, and inline Quick Calculator.
- **Advanced Calculator**: Safe expression parser (`+`, `-`, `×`, `÷`, `%`, `()`), GST (Inclusive/Exclusive + CGST/SGST split), Discount (single & stacked), Percentage, Profit & Loss, Indian & standard Unit Converter (*Gaj*, *Bigha*, *Tola*, *Quintal*, etc.), persisted Calculation History, and Hindi/English NL commands.
- **Customer & Udhar/Jama Ledger**: Unlimited customers with Photo Picker avatars, search/filter, strictly separated receivable (`UDHAR_DIYA` / `JAMA_RECEIVED`) and payable (`UDHAR_LIYA` / `PAYMENT_MADE`) balances, partial payments, edit/delete safeguards, and Daily/Weekly/Monthly Canvas reports.
- **Bilingual AI Smart System (Hindi + English)**:
  - **Offline Smart Engine**: Parses Hindi, Hinglish, and English commands (`"Rahul ko 500 rupaye udhar diye"`, `"Rahul se 200 rupaye jama mile"`, `"Aaj ka total hisab batao"`, `"Kis customer ka sabse zyada baki hai?"`, `"Pichhle mahine ki report dikhao"`), detects possible duplicate transactions within 15 minutes, and always asks explicit **Confirm & Save** approval.
  - **Online AI Engine**: Powered by **Firebase AI Logic (`gemini-3.8-flash`)** with structured JSON output and App Check on Android (and optional user-entered API key in the Web Edition).
- **SMS System & Notifications**:
  - Zero-permission **Manual SMS Composer** fallback (`ACTION_SENDTO smsto:` on Android / `sms:` on Web).
  - User-consented **Direct & Scheduled SMS** (`SEND_SMS` runtime permission + `SmsManager` Sent/Delivered tracking + `WorkManager` background checks respecting Android restrictions).
  - Custom templates (`REMINDER`, `UDHAR_CONFIRM`, `JAMA_CONFIRM`, `BALANCE_SUMMARY`), SMS history log, and Android `NotificationChannel`s.
- **Themes, Profile & Backup**:
  - 6 preset themes (`Premium Dark`, `Light`, `Blue`, `Green`, `Purple`, `Black AMOLED`) + Custom Accent & Custom Background Studio persisted in DataStore / LocalStorage.
  - Bilingual switcher (`English`, `हिन्दी`, `Hinglish`), INR `₹` formatting, non-destructive JSON Backup/Restore, CSV Export, and legacy `custom_hisab_calculator.html` LocalStorage migrator.

---

## Repository Structure

```text
desi-calculator-ai-pro/
├── README.md
├── LICENSE
├── .gitignore
├── app/                         # Native Android Studio Module (Kotlin + Jetpack Compose)
├── gradle/                      # Gradle Version Catalog & wrapper
├── web/                         # Modular Responsive Web Application
│   ├── index.html
│   ├── css/styles.css
│   ├── js/
│   │   ├── app.js
│   │   ├── storage.js
│   │   ├── calculator.js
│   │   ├── ledger.js
│   │   ├── ai_assistant.js
│   │   └── sms.js
│   └── assets/
├── docs/
│   ├── MIGRATION.md
│   ├── PERMISSIONS.md
│   └── TESTING_CHECKLIST.md
└── screenshots/
```

---

## Installation & Build Instructions

### 1. Native Android App (Android Studio & CLI)
**Prerequisites**: Android Studio Ladybug+, JDK 17+, Android SDK 36 (`minSdk = 26`, `targetSdk = 36`).

```bash
# Run unit & Robolectric tests
gradle :app:testDebugUnitTest

# Build Debug APK
gradle :app:assembleDebug

# Build Release APK (signing env vars required)
gradle :app:assembleRelease
```
- Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`
- Firebase AI Logic uses App Check (`configureAppCheck(this, intent)` in `MainActivity.onCreate()`) with `app/google-services.json`. No client-side `GEMINI_API_KEY` is hardcoded.

### 2. Responsive HTML Web App
Serve `web/` using any static web server (ES modules require HTTP/HTTPS, not `file://`):

```bash
python3 -m http.server 8080 --directory web
# Open http://localhost:8080
```

See `docs/MIGRATION.md`, `docs/PERMISSIONS.md`, and `docs/TESTING_CHECKLIST.md` for full details.
