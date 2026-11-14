# HTTPS SMS Gateway & Token Backend Setup

## 1. Authorized SMS Gateway
Configure in **Settings → Authorized HTTPS SMS Gateway**:
- **Endpoint URL:** Any `https://` REST endpoint accepting JSON:
  ```json
  {
    "to": "+919876543210",
    "senderId": "FITHUB",
    "message": "Your gym reminder message"
  }
  ```
- **API Key:** Stored encrypted with AndroidKeyStore AES-256-GCM and sent via `Authorization: Bearer <API_KEY>`.
- **Fallback:** When no HTTPS gateway is enabled, automated reminders queue cleanly and individual alerts can open the device SMS app via zero-permission `ACTION_SENDTO` (`smsto:`).

## 2. Token Verification Backend (Optional)
Configure in **Settings → Token Backend & Offline Lease**:
- **Endpoint URL:** Any `https://` endpoint accepting `{ "tokenHash": "<sha256>", "deviceHash": "<sha256>" }` and returning `{ "valid": true, "message": "ok" }`.
- **Offline Cache Lease:** Verified tokens receive an AES-256-GCM encrypted offline authorization lease bounded to 1–72 hours (default 24 hours).
