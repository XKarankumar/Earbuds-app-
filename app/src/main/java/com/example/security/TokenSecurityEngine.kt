package com.example.security

import com.example.data.local.AccessTokenEntity
import com.example.data.local.TokenAuditEntity
import com.example.data.local.TokenDao
import com.example.model.TokenSessionState
import com.example.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class TokenVerificationResult {
    data class Verified(
        val role: UserRole,
        val maskedToken: String,
        val label: String,
        val expiresAtMillis: Long,
        val offlineLeaseUntilMillis: Long,
        val deviceBound: Boolean,
        val viaBackend: Boolean
    ) : TokenVerificationResult()

    data class Denied(val reason: String, val lockedOutUntilMillis: Long = 0L) : TokenVerificationResult()
}

data class GeneratedTokenPair(
    val plainToken: String,
    val entity: AccessTokenEntity
)

/**
 * Cryptographic Token-Based App Access engine.
 * - Zero hardcoded master tokens anywhere in the APK.
 * - Optional HTTPS backend verification when configured.
 * - One-device binding using SHA-256 device fingerprint.
 * - Time-limited offline access lease encrypted via AndroidKeyStore AES-GCM.
 * - Brute-force invalid token protection with lockout.
 */
class TokenSecurityEngine(
    private val crypto: KeystoreCryptoManager,
    private val tokenDao: TokenDao,
    private val prefsRepo: AppPreferencesRepository
) {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    fun generateUniqueToken(
        role: UserRole,
        label: String,
        validityDays: Int,
        bindToCurrentDevice: Boolean,
        assignedMemberId: Long? = null
    ): GeneratedTokenPair {
        val entropy = crypto.generateRandomAlphanumeric(12)
        val body = "FH-${role.code}-${entropy.substring(0, 4)}-${entropy.substring(4, 8)}-${entropy.substring(8, 12)}"
        val chk = crypto.sha256(body).take(4).uppercase()
        val plain = "$body-$chk"
        val masked = "FH-${role.code}-••••-$chk"
        val now = System.currentTimeMillis()
        val expiresAt = now + validityDays.coerceIn(1, 730) * 86_400_000L
        val entity = AccessTokenEntity(
            tokenHash = crypto.sha256(plain),
            maskedToken = masked,
            role = role.name,
            label = label.trim().ifBlank { "${role.label} Access" },
            assignedMemberId = assignedMemberId,
            deviceBindingHash = if (bindToCurrentDevice) crypto.deviceBindingHash() else null,
            expiresAtMillis = expiresAt,
            isRevoked = false,
            lastVerifiedMillis = 0L,
            createdAtMillis = now
        )
        return GeneratedTokenPair(plainToken = plain, entity = entity)
    }

    suspend fun initializeFirstOwnerConsole(): GeneratedTokenPair {
        val pair = generateUniqueToken(
            role = UserRole.ADMIN,
            label = "Owner Primary Console",
            validityDays = 365,
            bindToCurrentDevice = true
        )
        tokenDao.insertToken(pair.entity.copy(lastVerifiedMillis = System.currentTimeMillis()))
        val deviceShort = crypto.deviceBindingHash().take(10)
        tokenDao.insertAudit(
            TokenAuditEntity(
                maskedToken = pair.entity.maskedToken,
                action = "INIT_OWNER",
                role = UserRole.ADMIN.name,
                deviceHashShort = deviceShort,
                success = true,
                detail = "Initialized unique AndroidKeyStore-protected Owner Admin token"
            )
        )
        cacheOfflineLease(
            tokenHash = pair.entity.tokenHash,
            role = UserRole.ADMIN,
            maskedToken = pair.entity.maskedToken,
            label = pair.entity.label,
            expiresAtMillis = pair.entity.expiresAtMillis,
            deviceBound = true
        )
        return pair
    }

    suspend fun verifyToken(rawInput: String): TokenVerificationResult {
        val token = rawInput.trim().uppercase()
        val now = System.currentTimeMillis()
        val prefs = prefsRepo.current()
        val deviceHash = crypto.deviceBindingHash()
        val deviceShort = deviceHash.take(10)

        if (prefs.tokenLockoutUntilMillis > now) {
            val secs = ((prefs.tokenLockoutUntilMillis - now) / 1000L).coerceAtLeast(1)
            return TokenVerificationResult.Denied(
                reason = "Invalid token protection active. Retry in ${secs}s.",
                lockedOutUntilMillis = prefs.tokenLockoutUntilMillis
            )
        }

        if (token.length < 12) {
            return recordFailure(
                masked = "INVALID",
                deviceShort = deviceShort,
                reason = "Malformed token format.",
                currentFailures = prefs.failedTokenAttempts
            )
        }

        val tokenHash = crypto.sha256(token)

        // 1. Optional HTTPS Backend Verification if configured
        val backendUrl = prefs.tokenBackendUrl.trim()
        if (backendUrl.startsWith("https://", ignoreCase = true)) {
            val remote = verifyViaHttpsBackend(backendUrl, token, deviceHash)
            if (remote != null) {
                if (!remote.first) {
                    return recordFailure(
                        masked = maskRaw(token),
                        deviceShort = deviceShort,
                        reason = remote.second,
                        currentFailures = prefs.failedTokenAttempts
                    )
                }
            }
        }

        // 2. Local Registry & Integrity Check
        val stored = tokenDao.findByHash(tokenHash)
            ?: return recordFailure(
                masked = maskRaw(token),
                deviceShort = deviceShort,
                reason = "Token not recognized or invalid.",
                currentFailures = prefs.failedTokenAttempts
            )

        if (stored.isRevoked) {
            return recordFailure(
                masked = stored.maskedToken,
                deviceShort = deviceShort,
                reason = "This token has been revoked as compromised.",
                currentFailures = prefs.failedTokenAttempts
            )
        }

        if (stored.expiresAtMillis <= now) {
            return recordFailure(
                masked = stored.maskedToken,
                deviceShort = deviceShort,
                reason = "Token expired. Request renewal from Admin.",
                currentFailures = prefs.failedTokenAttempts
            )
        }

        if (!stored.deviceBindingHash.isNullOrBlank() && stored.deviceBindingHash != deviceHash) {
            return recordFailure(
                masked = stored.maskedToken,
                deviceShort = deviceShort,
                reason = "One-device binding mismatch. Token locked to another device.",
                currentFailures = prefs.failedTokenAttempts
            )
        }

        // Valid! Clear failed attempts, update lastVerified, cache time-limited offline lease
        prefsRepo.setTokenLockout(0, 0L)
        tokenDao.updateToken(stored.copy(lastVerifiedMillis = now))
        val role = UserRole.fromKey(stored.role)
        val leaseUntil = cacheOfflineLease(
            tokenHash = stored.tokenHash,
            role = role,
            maskedToken = stored.maskedToken,
            label = stored.label,
            expiresAtMillis = stored.expiresAtMillis,
            deviceBound = !stored.deviceBindingHash.isNullOrBlank()
        )
        tokenDao.insertAudit(
            TokenAuditEntity(
                maskedToken = stored.maskedToken,
                action = "VERIFY_OK",
                role = role.name,
                deviceHashShort = deviceShort,
                success = true,
                detail = if (backendUrl.startsWith("https://")) "Verified via HTTPS + local vault" else "Verified against Keystore vault"
            )
        )
        return TokenVerificationResult.Verified(
            role = role,
            maskedToken = stored.maskedToken,
            label = stored.label,
            expiresAtMillis = stored.expiresAtMillis,
            offlineLeaseUntilMillis = leaseUntil,
            deviceBound = !stored.deviceBindingHash.isNullOrBlank(),
            viaBackend = backendUrl.startsWith("https://")
        )
    }

    suspend fun restoreCachedOfflineSession(): TokenSessionState {
        val prefs = prefsRepo.current()
        val hasAdmin = tokenDao.activeAdminTokenCount() > 0
        val now = System.currentTimeMillis()
        if (prefs.encryptedCachedAuthLease.isBlank()) {
            return TokenSessionState(
                isAuthenticated = false,
                hasAnyAdminToken = hasAdmin,
                lockedOutUntilMillis = prefs.tokenLockoutUntilMillis,
                failedAttempts = prefs.failedTokenAttempts
            )
        }
        val plain = crypto.decrypt(prefs.encryptedCachedAuthLease)
        if (plain.isBlank()) {
            return TokenSessionState(isAuthenticated = false, hasAnyAdminToken = hasAdmin)
        }
        return try {
            val json = JSONObject(plain)
            val tokenHash = json.getString("tokenHash")
            val leaseUntil = json.getLong("leaseUntil")
            val tokenExpiry = json.getLong("expiresAt")
            val deviceBound = json.optBoolean("deviceBound", false)
            val boundDevice = json.optString("deviceHash", "")

            if (leaseUntil <= now || tokenExpiry <= now) {
                prefsRepo.setCachedAuthLease("")
                return TokenSessionState(isAuthenticated = false, hasAnyAdminToken = hasAdmin)
            }
            if (deviceBound && boundDevice != crypto.deviceBindingHash()) {
                prefsRepo.setCachedAuthLease("")
                return TokenSessionState(isAuthenticated = false, hasAnyAdminToken = hasAdmin)
            }
            val stored = tokenDao.findByHash(tokenHash)
            if (stored == null || stored.isRevoked || stored.expiresAtMillis <= now) {
                prefsRepo.setCachedAuthLease("")
                return TokenSessionState(isAuthenticated = false, hasAnyAdminToken = hasAdmin)
            }
            TokenSessionState(
                isAuthenticated = true,
                role = UserRole.fromKey(stored.role),
                maskedToken = stored.maskedToken,
                tokenLabel = stored.label,
                expiresAtMillis = stored.expiresAtMillis,
                offlineLeaseUntilMillis = leaseUntil,
                isOfflineCached = true,
                deviceBound = deviceBound,
                hasAnyAdminToken = hasAdmin
            )
        } catch (_: Throwable) {
            TokenSessionState(isAuthenticated = false, hasAnyAdminToken = hasAdmin)
        }
    }

    suspend fun signOut() {
        prefsRepo.setCachedAuthLease("")
    }

    private suspend fun cacheOfflineLease(
        tokenHash: String,
        role: UserRole,
        maskedToken: String,
        label: String,
        expiresAtMillis: Long,
        deviceBound: Boolean
    ): Long {
        val prefs = prefsRepo.current()
        val now = System.currentTimeMillis()
        val leaseUntil = minOf(expiresAtMillis, now + prefs.offlineLeaseHours * 3_600_000L)
        val payload = JSONObject().apply {
            put("tokenHash", tokenHash)
            put("role", role.name)
            put("maskedToken", maskedToken)
            put("label", label)
            put("expiresAt", expiresAtMillis)
            put("leaseUntil", leaseUntil)
            put("deviceBound", deviceBound)
            put("deviceHash", crypto.deviceBindingHash())
        }.toString()
        prefsRepo.setCachedAuthLease(crypto.encrypt(payload))
        return leaseUntil
    }

    private suspend fun recordFailure(
        masked: String,
        deviceShort: String,
        reason: String,
        currentFailures: Int
    ): TokenVerificationResult.Denied {
        val nextFailures = currentFailures + 1
        val lockUntil = if (nextFailures >= 5) {
            System.currentTimeMillis() + (30_000L * (nextFailures - 4).coerceAtMost(6))
        } else {
            0L
        }
        prefsRepo.setTokenLockout(nextFailures, lockUntil)
        tokenDao.insertAudit(
            TokenAuditEntity(
                maskedToken = masked,
                action = "VERIFY_DENIED",
                role = "NONE",
                deviceHashShort = deviceShort,
                success = false,
                detail = reason
            )
        )
        return TokenVerificationResult.Denied(reason, lockUntil)
    }

    private suspend fun verifyViaHttpsBackend(
        url: String,
        token: String,
        deviceHash: String
    ): Pair<Boolean, String>? = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("tokenHash", crypto.sha256(token))
                put("deviceHash", deviceHash)
            }.toString()
            val req = Request.Builder()
                .url(url)
                .post(json.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    false to "Backend rejected token (HTTP ${resp.code})"
                } else {
                    val bodyStr = resp.body?.string().orEmpty()
                    val parsed = runCatching { JSONObject(bodyStr) }.getOrNull()
                    val valid = parsed?.optBoolean("valid", true) ?: true
                    val msg = parsed?.optString("message").orEmpty().ifBlank { "Backend verification failed" }
                    valid to msg
                }
            }
        } catch (_: Throwable) {
            // Fall back cleanly to time-limited local Keystore verification when offline
            null
        }
    }

    private fun maskRaw(raw: String): String {
        val clean = raw.trim()
        return if (clean.length <= 6) "INVALID" else "${clean.take(4)}••••${clean.takeLast(4)}"
    }
}
