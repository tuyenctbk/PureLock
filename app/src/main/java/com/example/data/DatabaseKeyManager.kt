package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import com.example.service.KeystoreHardwareSecurityManager
import java.security.SecureRandom
import java.util.Arrays

/**
 * Abstraction for wrapping/unwrapping the 256-bit SQLCipher database key using
 * hardware-backed Android Keystore AES-256-GCM (or a test double in unit tests).
 */
interface PassphraseKeyWrapper {
    fun encrypt(plainBytes: ByteArray): Pair<String, String>?
    fun decrypt(cipherBase64: String, ivBase64: String): ByteArray?
}

internal class HardwareKeystorePassphraseWrapper(
    private val keystoreManager: KeystoreHardwareSecurityManager
) : PassphraseKeyWrapper {
    override fun encrypt(plainBytes: ByteArray): Pair<String, String>? =
        keystoreManager.encryptBytes(plainBytes)

    override fun decrypt(cipherBase64: String, ivBase64: String): ByteArray? =
        keystoreManager.decryptBytes(cipherBase64, ivBase64)
}

/**
 * Manages the 256-bit SQLCipher database encryption passphrase.
 *
 * Security model:
 * - Generates a cryptographically strong 32-byte random passphrase on first run.
 * - Wraps (encrypts) the passphrase with the hardware-backed Android Keystore AES-256-GCM
 *   master key before writing ciphertext + IV to private SharedPreferences.
 * - Transparently migrates any legacy plaintext passphrase from earlier app versions into
 *   Keystore-wrapped storage and deletes the legacy plaintext entry.
 */
object DatabaseKeyManager {
    private const val TAG = "DatabaseKeyManager"
    const val PREFS_NAME = "purelock_sec_vault"
    const val LEGACY_KEY_ALIAS = "purelock_db_enc_key"
    const val WRAPPED_KEY_CIPHER_ALIAS = "purelock_db_enc_key_cipher"
    const val WRAPPED_KEY_IV_ALIAS = "purelock_db_enc_key_iv"
    const val PASSPHRASE_LENGTH_BYTES = 32

    private val lock = Any()

    fun getOrCreatePassphrase(context: Context): ByteArray {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val wrapper = HardwareKeystorePassphraseWrapper(KeystoreHardwareSecurityManager(appContext))
        return getOrCreatePassphrase(prefs, wrapper)
    }

    internal fun getOrCreatePassphrase(
        prefs: SharedPreferences,
        wrapper: PassphraseKeyWrapper
    ): ByteArray = synchronized(lock) {
        // 1. Primary path: unwrap Android Keystore AES-256-GCM encrypted passphrase
        val wrappedCipher = prefs.getString(WRAPPED_KEY_CIPHER_ALIAS, null)
        val wrappedIv = prefs.getString(WRAPPED_KEY_IV_ALIAS, null)
        if (!wrappedCipher.isNullOrEmpty() && !wrappedIv.isNullOrEmpty()) {
            val unwrapped = wrapper.decrypt(wrappedCipher, wrappedIv)
            if (unwrapped != null && unwrapped.size == PASSPHRASE_LENGTH_BYTES) {
                return unwrapped
            }
            Log.e(TAG, "Failed to unwrap hardware-protected SQLCipher key; checking legacy fallback.")
        }

        // 2. Seamless migration path: upgrade legacy plaintext key into Keystore-wrapped storage
        val legacyEncoded = prefs.getString(LEGACY_KEY_ALIAS, null)
        if (!legacyEncoded.isNullOrEmpty()) {
            val legacyBytes = try {
                Base64.decode(legacyEncoded, Base64.NO_WRAP)
            } catch (_: IllegalArgumentException) {
                null
            }
            if (legacyBytes != null && legacyBytes.size == PASSPHRASE_LENGTH_BYTES) {
                val wrapped = wrapper.encrypt(legacyBytes)
                if (wrapped != null) {
                    prefs.edit()
                        .putString(WRAPPED_KEY_CIPHER_ALIAS, wrapped.first)
                        .putString(WRAPPED_KEY_IV_ALIAS, wrapped.second)
                        .remove(LEGACY_KEY_ALIAS)
                        .commit()
                    Log.i(TAG, "Migrated legacy SQLCipher passphrase to Android Keystore AES-256-GCM wrapping.")
                }
                return legacyBytes
            }
        }

        // 3. Fresh install: generate a 256-bit cryptographically random SQLCipher passphrase
        val newPassphrase = ByteArray(PASSPHRASE_LENGTH_BYTES)
        SecureRandom().nextBytes(newPassphrase)

        val wrapped = wrapper.encrypt(newPassphrase)
        if (wrapped != null) {
            prefs.edit()
                .putString(WRAPPED_KEY_CIPHER_ALIAS, wrapped.first)
                .putString(WRAPPED_KEY_IV_ALIAS, wrapped.second)
                .remove(LEGACY_KEY_ALIAS)
                .commit()
        } else {
            // Fallback for environments without AndroidKeyStore (e.g., JVM unit tests)
            val encoded = Base64.encodeToString(newPassphrase, Base64.NO_WRAP)
            prefs.edit()
                .putString(LEGACY_KEY_ALIAS, encoded)
                .commit()
            Log.w(TAG, "Android Keystore unavailable; stored SQLCipher passphrase in private fallback prefs.")
        }

        return newPassphrase
    }

    fun isPassphraseHardwareProtected(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return isPassphraseHardwareProtected(prefs)
    }

    internal fun isPassphraseHardwareProtected(prefs: SharedPreferences): Boolean {
        val hasCipher = !prefs.getString(WRAPPED_KEY_CIPHER_ALIAS, null).isNullOrEmpty()
        val hasIv = !prefs.getString(WRAPPED_KEY_IV_ALIAS, null).isNullOrEmpty()
        val hasLegacyPlaintext = !prefs.getString(LEGACY_KEY_ALIAS, null).isNullOrEmpty()
        return hasCipher && hasIv && !hasLegacyPlaintext
    }

    fun wipePassphrase(passphrase: ByteArray) {
        Arrays.fill(passphrase, 0.toByte())
    }
}
