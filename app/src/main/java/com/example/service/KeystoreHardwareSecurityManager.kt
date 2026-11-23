package com.example.service

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeystoreHardwareSecurityManager(private val context: Context) {
    companion object {
        private const val KEYSTORE_ALIAS = "PureLockTEEMasterKey"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private val keyStoreLock = Any()

        @Volatile
        private var cachedSecretKey: SecretKey? = null
    }

    init {
        getOrCreateSecretKey()
    }

    private fun getOrCreateSecretKey(forceReload: Boolean = false): SecretKey? {
        if (!forceReload) {
            cachedSecretKey?.let { return it }
        }
        return synchronized(keyStoreLock) {
            if (!forceReload) {
                cachedSecretKey?.let { return@synchronized it }
            }
            try {
                val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
                if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
                    val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                    val builder = KeyGenParameterSpec.Builder(
                        KEYSTORE_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        builder.setIsStrongBoxBacked(false)
                    }
                    keyGenerator.init(builder.build())
                    keyGenerator.generateKey()
                }
                val key = keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey
                cachedSecretKey = key
                key
            } catch (e: Exception) {
                cachedSecretKey = null
                e.printStackTrace()
                null
            }
        }
    }

    fun isKeystoreAvailable(): Boolean {
        return getOrCreateSecretKey() != null
    }

    fun encryptBytes(plainBytes: ByteArray): Pair<String, String>? {
        return try {
            val secretKey = getOrCreateSecretKey() ?: return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainBytes)
            Pair(
                Base64.encodeToString(encryptedBytes, Base64.NO_WRAP),
                Base64.encodeToString(iv, Base64.NO_WRAP)
            )
        } catch (e: Exception) {
            // Retry once with a fresh Keystore lookup in case the cached handle was invalidated
            try {
                val refreshedKey = getOrCreateSecretKey(forceReload = true) ?: return null
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, refreshedKey)
                val iv = cipher.iv
                val encryptedBytes = cipher.doFinal(plainBytes)
                Pair(
                    Base64.encodeToString(encryptedBytes, Base64.NO_WRAP),
                    Base64.encodeToString(iv, Base64.NO_WRAP)
                )
            } catch (retryError: Exception) {
                retryError.printStackTrace()
                null
            }
        }
    }

    fun decryptBytes(encryptedBase64: String, ivBase64: String): ByteArray? {
        return try {
            val secretKey = getOrCreateSecretKey() ?: return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val decodedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            cipher.doFinal(decodedBytes)
        } catch (e: Exception) {
            try {
                val refreshedKey = getOrCreateSecretKey(forceReload = true) ?: return null
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
                val spec = GCMParameterSpec(128, iv)
                cipher.init(Cipher.DECRYPT_MODE, refreshedKey, spec)
                val decodedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)
                cipher.doFinal(decodedBytes)
            } catch (retryError: Exception) {
                retryError.printStackTrace()
                null
            }
        }
    }

    fun encryptData(plainText: String): Pair<String, String>? {
        return encryptBytes(plainText.toByteArray(Charsets.UTF_8))
    }

    fun decryptData(encryptedBase64: String, ivBase64: String): String? {
        val decrypted = decryptBytes(encryptedBase64, ivBase64) ?: return null
        return String(decrypted, Charsets.UTF_8)
    }
}
