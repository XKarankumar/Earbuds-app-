# Testing Checklist — Desi Calculator AI PRO V2.0

## Automated Tests (JVM + Robolectric + Web ESM)
Executed `gradle :app:testDebugUnitTest` — **11/11 tests PASSED**:
- [x] `ExampleUnitTest`: Baseline arithmetic sanity check (`addition_isCorrect` PASSED).
- [x] `CalculatorEngineTest` (5/5 PASSED): Expression precedence, brackets, `%`, division-by-zero handling, GST Inclusive/Exclusive + CGST/SGST split, stacked Discounts, Profit/Loss, and Indian unit conversions (*Quintal*, *Gaj*).
- [x] `DesiLedgerRobolectricTest` (5/5 PASSED):
  - [x] Strict separation of `UDHAR_DIYA` / `JAMA_RECEIVED` vs `UDHAR_LIYA` / `PAYMENT_MADE` balances.
  - [x] Duplicate transaction detection within 15-minute window.
  - [x] Non-destructive legacy `custom_hisab_calculator.html` JSON import preserving existing records.
  - [x] Bilingual Hindi/English Offline Smart Engine command parsing (`"Rahul ko 500 rupaye udhar diye"`, `"Kis customer ka sabse zyada baki hai?"`, `"Aaj ka total hisab batao"`) without silent writes.
  - [x] App metadata & identity verification (`app_name`, version `2.0.0`).
- [x] Web Edition (`web/js/*.js`): ES module syntax & runtime module logic verified across all 6 JS modules.

## Manual Verification Checklist (Android & Web)
- [ ] Animated splash screen logo reveal transitions cleanly into Dashboard.
- [ ] Basic, GST, Discount, Percentage, Profit/Loss, and Unit calculators compute accurately and persist to History.
- [ ] Adding/editing/deleting customers, Photo Picker avatar upload, search, and balance filters (`Lena Hai`, `Dena Hai`, `Settled`).
- [ ] Partial payments and edit/delete transactions recalculate customer & dashboard totals immediately.
- [ ] AI Smart Assistant proposes transaction confirmation card and warns on duplicates before saving.
- [ ] Manual SMS Composer opens pre-filled `smsto:` / `sms:`; Direct SMS logs `SENT`/`DELIVERED`/`FAILED` status.
- [ ] All 7 theme presets + custom accent/background persist across restarts.
- [ ] Language switcher (`English`, `हिन्दी`, `Hinglish`) updates UI labels dynamically.
- [ ] JSON Backup, JSON Restore, and CSV Export produce valid files.
