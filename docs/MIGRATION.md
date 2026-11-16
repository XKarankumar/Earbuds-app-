# Database & Legacy LocalStorage Migration Guide

**Desi Calculator AI PRO V2.0 — Non-Destructive Upgrade Policy**

## 1. Android Room Database Migration (`v1 -> v2`)
`DesiCalculatorDatabase` enforces non-destructive schema upgrades via `MIGRATION_1_2` (`fallbackToDestructiveMigration()` is intentionally **never** used):
- Preserves all existing `customers`, `transactions`, `calculation_history`, `sms_logs`, and `ai_chat_messages` rows.
- Adds `smsConsentGranted`, `reminderIntervalDays`, `lastReminderSentAt` to `customers` and `isPartialPayment` to `transactions` with safe `DEFAULT 0` values.

## 2. Migrating Data from Legacy `custom_hisab_calculator.html`
Both the Android app (`Profile -> Backup & Migration`) and the Web Edition (`Profile -> Backup, Restore & Legacy Migration`) support non-destructive merging:

### Web Edition Automatic Migration
On first load, `web/js/storage.js` scans legacy browser `localStorage` keys (`hisab_customers`, `customers`, `udhar_jama_entries`, `transactions`, `hisabData`, `custom_hisab_data`) and merges all valid customers and transactions into `desi_calculator_ai_pro_v2` without deleting or overwriting existing records.

### Moving Data from HTML Browser to Native Android APK
1. In the HTML Web App, open **SMS & Reports** (or **Profile**) and click **Export JSON Backup** (or export your existing legacy `localStorage` JSON).
2. In the Android app, open **Profile -> Backup, Restore & HTML Data Migration**.
3. Tap **Import JSON File** (uses zero-permission Storage Access Framework) or **Paste Legacy JSON**.
4. `BackupAndMigrationManager.importJsonNonDestructive()` matches existing customers by `legacyExternalId`, mobile number, or exact name and skips already-imported transaction IDs/timestamps so **no old records are ever overwritten or duplicated**.
