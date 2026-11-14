package com.example.security

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Hardware/AndroidKeyStore-backed AES-256-GCM cryptography helper.
 * Encrypts sensitive PII (mobile numbers, emergency contacts, gateway API keys, cached offline auth).
 * Includes a safe deterministic fallback cipher for JVM Robolectric unit tests where AndroidKeyStore is absent.
 */
class KeystoreCryptoManager(private val context: Context) {

    private val keyAlias = "FitnessHubMasterAesKey_v1"
    private val transformation = "AES/GCM/NoPadding"
    private val gcmTagBits = 128
    private val secureRandom = SecureRandom()

    private fun getOrCreateKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val existing = keyStore.getKey(keyAlias, null) as? SecretKey
            if (existing != null) return existing

            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val spec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGen.init(spec)
            keyGen.generateKey()
        } catch (_: Throwable) {
            // Robolectric / JVM fallback key stored only inside app-private filesDir
            getOrCreateAppPrivateFallbackKey()
        }
    }

    private fun getOrCreateAppPrivateFallbackKey(): SecretKey {
        val file = java.io.File(context.filesDir, ".fh_local_vault.key")
        val rawBytes = if (file.exists() && file.length() == 32L) {
            file.readBytes()
        } else {
            ByteArray(32).also {
                secureRandom.nextBytes(it)
                file.writeBytes(it)
            }
        }
        return SecretKeySpec(rawBytes, "AES")
    }

    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        val combined = ByteArray(iv.size + cipherBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)
        return "ENC:" + Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(cipherText: String): String {
        if (cipherText.isBlank()) return ""
        if (!cipherText.startsWith("ENC:")) return cipherText
        return try {
            val combined = Base64.decode(cipherText.removePrefix("ENC:"), Base64.NO_WRAP)
            if (combined.size <= 12) return ""
            val iv = combined.copyOfRange(0, 12)
            val data = combined.copyOfRange(12, combined.size)
            val key = getOrCreateKey()
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(gcmTagBits, iv))
            String(cipher.doFinal(data), StandardCharsets.UTF_8)
        } catch (_: Throwable) {
            ""
        }
    }

    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    @SuppressLint("HardwareIds")
    fun deviceBindingHash(): String {
        val androidId = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull().orEmpty().ifBlank { "fh-device-local" }
        return sha256("${context.packageName}::$androidId")
    }

    fun generateRandomAlphanumeric(length: Int): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString(length) {
            repeat(length) {
                append(alphabet[secureRandom.nextInt(alphabet.length)])
            }
        }
    }

    fun generateNumericOtp(digits: Int = 6): String {
        return buildString(digits) {
            repeat(digits) {
                append(secureRandom.nextInt(10))
            }
        }
    }
}
