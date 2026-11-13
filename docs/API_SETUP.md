# API & Backend Integration Guide — Karan Number Finder Pro AI

**App Name:** Karan Number Finder Pro AI  
**Developer:** Hardik Kumar  
**Version:** 1.0  

## 1. Provisioned Cloud AI Backend (Firebase AI Logic + App Check)
All AI capabilities (Phone Risk & Caller Verification, Multimodal Photo Analysis, Spam/Phishing Detection, and Bilingual Hindi/English AI Assistant) run securely over HTTPS via **Firebase AI Logic** (`gemini-3.8-flash`) attested by **Firebase App Check**.

- **Firebase Project ID:** `gen-lang-client-0047373257`
- **Android Configuration:** `app/google-services.json` and `app/src/main/res/values/firebase_applet_config.xml`
- **App Check Wiring:** Configured in `MainActivity.onCreate()` via `configureAppCheck(this, intent)`:
  - **Debug Builds:** Uses `DebugAppCheckProviderFactory` with synchronous `.commit()` persistence of `FIREBASE_APPCHECK_DEBUG_TOKEN`.
  - **Release Builds:** Uses `RecaptchaAppCheckProviderFactory` with `recaptcha_site_key`.
- **No Client-Side API Key Exposure:** Gemini requests do not embed a raw `GEMINI_API_KEY` inside the APK.

## 2. Offline / On-Device Engines (Zero API Key Required)
- **Google `libphonenumber` (`8.13.52`):** International phone number validation, E.164 / National / International formatting, country & calling code detection, number type (`MOBILE`, `FIXED_LINE`, `TOLL_FREE`, `VOIP`, `PREMIUM_RATE`), and text extraction.
- **Google ML Kit Text Recognition (`16.0.1`):** On-device OCR extraction of text and phone numbers from selected images before optional AI multimodal description.

## 3. Features Requiring Paid / External Telecom APIs
For legal and technical accuracy, **live subscriber HLR/MNP carrier lookups and licensed commercial Caller ID directories** are **never fabricated**. To enable live telecom carrier/CNAM queries in production, deploy an HTTPS backend proxy (e.g., Cloud Run / Cloud Functions) connected to an authorized provider:
- **Twilio Lookup v2** (Carrier & Line Type Intelligence — paid subscription)
- **Numverify / AbstractAPI** (Telecom carrier metadata — paid tier)
- **Truecaller Enterprise Business API** (Authorized commercial caller verification — enterprise license)

Store provider API keys strictly on your backend proxy server (never in mobile client binaries).
