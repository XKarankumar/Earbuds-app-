# Database & AndroidKeyStore Encryption Configuration

## 1. Room Database (`fitness_hub_v1.db`)
- **Tables (`13`):** `members`, `trainers`, `attendance`, `payments`, `workout_plan_items`, `custom_routines`, `workout_history`, `diet_meals`, `weight_logs`, `water_logs`, `access_tokens`, `token_audit`, `sms_logs`.
- **Reactive Access:** All DAOs expose `Flow<List<...>>` for main-thread-safe reactive Compose updates and `suspend` functions for writes.

## 2. AndroidKeyStore AES-256-GCM Encryption (`KeystoreCryptoManager`)
- **Key Alias:** `FitnessHubMasterAesKey_v1` in `AndroidKeyStore` (`AES/GCM/NoPadding`, 256-bit, random 12-byte IV per ciphertext).
- **Encrypted at Rest:**
  - Member & Trainer mobile phone numbers (`encryptedPhone`)
  - Member emergency contact names & numbers (`encryptedEmergencyName`, `encryptedEmergencyPhone`)
  - SMS Gateway API Key / Bearer token (`sms_api_key_enc` in DataStore)
  - Time-limited Offline Token Authorization Lease (`cached_auth_lease_enc` in DataStore)
- **Token Hashing:** Access tokens are never stored in plaintext or hardcoded in the APK; only SHA-256 digests (`tokenHash`) and masked previews (`FH-ADM-••••-CHK`) are stored.
