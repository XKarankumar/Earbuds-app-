package com.example.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoVault {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "simple_sms_calc_vault_v1"
    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val GCM_IV_BYTES = 12
    private const val PBKDF2_ITERATIONS = 65536
    private const val PBKDF2_KEY_BITS = 256

    @Volatile
    private var jvmFallbackKey: SecretKey? = null

    private fun getOrCreateKeystoreKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            if (existing != null) return existing

            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGen.init(spec)
            keyGen.generateKey()
        } catch (_: Exception) {
            // Fallback strictly for non-Android JVM unit/Robolectric test environments
            jvmFallbackKey ?: synchronized(this) {
                jvmFallbackKey ?: KeyGenerator.getInstance("AES").apply { init(256) }.generateKey().also {
                    jvmFallbackKey = it
                }
            }
        }
    }

    fun encryptLocalString(plaintext: String): String {
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKeystoreKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val ctB64 = Base64.encodeToString(encrypted, Base64.NO_WRAP)
        return "$ivB64:$ctB64"
    }

    fun decryptLocalString(payload: String): String? {
        return try {
            val parts = payload.split(":")
            if (parts.size != 2) return null
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val ct = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKeystoreKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(ct), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    fun encryptJson(json: JSONObject): String = encryptLocalString(json.toString())

    fun decryptJson(payload: String): JSONObject? {
        val plain = decryptLocalString(payload) ?: return null
        return try {
            JSONObject(plain)
        } catch (_: Exception) {
            null
        }
    }

    fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun hashPinWithSalt(pin: String): Pair<String, String> {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val hash = deriveKeyBytes(pin, salt)
        return Base64.encodeToString(hash, Base64.NO_WRAP) to
            Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    fun verifyPin(pin: String, storedHashB64: String, storedSaltB64: String): Boolean {
        return try {
            val salt = Base64.decode(storedSaltB64, Base64.NO_WRAP)
            val expected = Base64.decode(storedHashB64, Base64.NO_WRAP)
            val actual = deriveKeyBytes(pin, salt)
            constantTimeEquals(actual, expected)
        } catch (_: Exception) {
            false
        }
    }

    fun encryptPortableBackup(jsonContent: String, passphrase: String): String {
        val salt = ByteArray(16)
        val iv = ByteArray(GCM_IV_BYTES)
        SecureRandom().apply {
            nextBytes(salt)
            nextBytes(iv)
        }
        val keyBytes = deriveKeyBytes(passphrase, salt)
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(jsonContent.toByteArray(Charsets.UTF_8))

        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val ctB64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        return "SSCBAK1|$saltB64|$ivB64|$ctB64"
    }

    fun decryptPortableBackup(encryptedPackage: String, passphrase: String): Result<String> {
        return try {
            val parts = encryptedPackage.trim().split("|")
            require(parts.size == 4 && parts[0] == "SSCBAK1") { "Invalid backup file format" }
            val salt = Base64.decode(parts[1], Base64.NO_WRAP)
            val iv = Base64.decode(parts[2], Base64.NO_WRAP)
            val ciphertext = Base64.decode(parts[3], Base64.NO_WRAP)

            val keyBytes = deriveKeyBytes(passphrase, salt)
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            val plain = String(cipher.doFinal(ciphertext), Charsets.UTF_8)
            Result.success(plain)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Incorrect passphrase or corrupted backup file", e))
        }
    }

    private fun deriveKeyBytes(secret: String, salt: ByteArray): ByteArray {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(secret.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_BITS)
        return factory.generateSecret(spec).encoded
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) {
            diff = diff or (a[i].toInt() xor b[i].toInt())
        }
        return diff == 0
    }
}
