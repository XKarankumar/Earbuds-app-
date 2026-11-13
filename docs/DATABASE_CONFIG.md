# Database & Encrypted Vault Configuration

## 1. Android Local Encrypted Database (Room + Android Keystore AES-256-GCM)
- **Database Name:** `karan_finder_vault.db` (`AppDatabase`, version `2`)
- **DAO:** `SearchHistoryDao` (`Flow<List<SearchHistoryEntity>>`, suspend `insert`, `deleteById`, `deleteAll`)
- **Entity:** `SearchHistoryEntity(id, recordType, encryptedPayload, riskScore, createdAtMillis)`
- **Encryption Layer (`CryptoVault.kt`):**
  - Uses `AndroidKeyStore` hardware-backed 256-bit `AES/GCM/NoPadding` key (`karan_finder_pro_vault_v1`).
  - Every `SearchHistoryRecord` JSON payload (`title`, `subtitle`, `riskLevel`, `summaryText`, `sourceAttribution`) is encrypted with a fresh 12-byte GCM IV before persisting to SQLite.
  - Optional 4-digit Vault PIN is hashed using `PBKDF2WithHmacSHA256` (65,536 iterations, 16-byte random salt) and stored encrypted in Jetpack DataStore (`karan_finder_pro_prefs`).

## 2. iOS Local Encrypted Vault (Keychain + CryptoKit AES-GCM)
- Implemented in `ios/KaranNumberFinderPro/Services/EncryptedVaultService.swift`.
- Generates a 256-bit `SymmetricKey` stored in iOS Keychain (`kSecClassGenericPassword`) and seals serialized history using `AES.GCM.seal`.

## 3. Guest Mode & User Control
- No mandatory Google/Apple sign-in is required.
- Users can toggle **Save Local Search History** in Security Center, delete individual records, or clear all encrypted records with one tap.
