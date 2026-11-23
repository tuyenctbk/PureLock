package com.example

import android.content.Context
import android.util.Base64
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.DatabaseKeyManager
import com.example.data.PassphraseKeyWrapper
import com.example.data.PureLockDatabase
import com.example.data.model.EncryptedVaultEntity
import com.example.data.model.LockedAppEntity
import com.example.data.model.SecurityLogEntity
import com.example.data.model.UserSettingEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PureLockDatabaseSecurityTest {

    /**
     * Software AES-256-GCM implementation of PassphraseKeyWrapper for deterministic JVM/Robolectric
     * testing where hardware AndroidKeyStore is not provisioned.
     */
    private class SoftwareAesGcmKeyWrapper : PassphraseKeyWrapper {
        private val secretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

        override fun encrypt(plainBytes: ByteArray): Pair<String, String> {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val encrypted = cipher.doFinal(plainBytes)
            return Pair(
                Base64.encodeToString(encrypted, Base64.NO_WRAP),
                Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
            )
        }

        override fun decrypt(cipherBase64: String, ivBase64: String): ByteArray {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            return cipher.doFinal(Base64.decode(cipherBase64, Base64.NO_WRAP))
        }
    }

    @Test
    fun `database key manager generates 256-bit wrapped key and persists stably`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("test_key_fresh", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val wrapper = SoftwareAesGcmKeyWrapper()

        val firstKey = DatabaseKeyManager.getOrCreatePassphrase(prefs, wrapper)
        val secondKey = DatabaseKeyManager.getOrCreatePassphrase(prefs, wrapper)

        assertEquals(DatabaseKeyManager.PASSPHRASE_LENGTH_BYTES, firstKey.size)
        assertArrayEquals(firstKey, secondKey)
        assertTrue(DatabaseKeyManager.isPassphraseHardwareProtected(prefs))
        assertNull(prefs.getString(DatabaseKeyManager.LEGACY_KEY_ALIAS, null))
        assertNotNull(prefs.getString(DatabaseKeyManager.WRAPPED_KEY_CIPHER_ALIAS, null))
        assertNotNull(prefs.getString(DatabaseKeyManager.WRAPPED_KEY_IV_ALIAS, null))

        DatabaseKeyManager.wipePassphrase(firstKey)
        assertTrue(firstKey.all { it == 0.toByte() })
    }

    @Test
    fun `database key manager migrates legacy plaintext key to hardware-wrapped storage`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("test_key_migration", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val wrapper = SoftwareAesGcmKeyWrapper()

        val legacyRaw = ByteArray(DatabaseKeyManager.PASSPHRASE_LENGTH_BYTES).also {
            SecureRandom().nextBytes(it)
        }
        prefs.edit()
            .putString(
                DatabaseKeyManager.LEGACY_KEY_ALIAS,
                Base64.encodeToString(legacyRaw, Base64.NO_WRAP)
            )
            .commit()

        assertFalse(DatabaseKeyManager.isPassphraseHardwareProtected(prefs))

        val migratedKey = DatabaseKeyManager.getOrCreatePassphrase(prefs, wrapper)
        assertArrayEquals(legacyRaw, migratedKey)
        assertTrue(DatabaseKeyManager.isPassphraseHardwareProtected(prefs))
        assertNull(prefs.getString(DatabaseKeyManager.LEGACY_KEY_ALIAS, null))

        // Subsequent reads unwrap the same exact key from AES-GCM ciphertext
        val subsequentKey = DatabaseKeyManager.getOrCreatePassphrase(prefs, wrapper)
        assertArrayEquals(legacyRaw, subsequentKey)
    }

    @Test
    fun `database key manager refuses to overwrite existing wrapped key on transient decrypt failure`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("test_key_no_overwrite", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val workingWrapper = SoftwareAesGcmKeyWrapper()

        val originalKey = DatabaseKeyManager.getOrCreatePassphrase(prefs, workingWrapper)
        val originalCipher = prefs.getString(DatabaseKeyManager.WRAPPED_KEY_CIPHER_ALIAS, null)
        val originalIv = prefs.getString(DatabaseKeyManager.WRAPPED_KEY_IV_ALIAS, null)

        val failingWrapper = object : PassphraseKeyWrapper {
            override fun encrypt(plainBytes: ByteArray): Pair<String, String>? = workingWrapper.encrypt(plainBytes)
            override fun decrypt(cipherBase64: String, ivBase64: String): ByteArray? = null
        }

        var threw = false
        try {
            DatabaseKeyManager.getOrCreatePassphrase(prefs, failingWrapper)
        } catch (_: IllegalStateException) {
            threw = true
        }

        assertTrue("Expected IllegalStateException when unwrap fails for existing wrapped key", threw)
        assertEquals(originalCipher, prefs.getString(DatabaseKeyManager.WRAPPED_KEY_CIPHER_ALIAS, null))
        assertEquals(originalIv, prefs.getString(DatabaseKeyManager.WRAPPED_KEY_IV_ALIAS, null))

        // Recovery succeeds cleanly once Keystore becomes available again
        val recoveredKey = DatabaseKeyManager.getOrCreatePassphrase(prefs, workingWrapper)
        assertArrayEquals(originalKey, recoveredKey)
    }

    @Test
    fun `sqlcipher header detector distinguishes plaintext sqlite from encrypted salt header`() {
        val plaintextHeader = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
        assertFalse(PureLockDatabase.isSqlCipherHeaderEncrypted(plaintextHeader))

        val encryptedSaltHeader = ByteArray(16) { index -> (index * 17 + 0xA3).toByte() }
        assertTrue(PureLockDatabase.isSqlCipherHeaderEncrypted(encryptedSaltHeader))

        val tooShort = ByteArray(8)
        assertFalse(PureLockDatabase.isSqlCipherHeaderEncrypted(tooShort))

        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempPlainFile = File(context.cacheDir, "plain_test.db").apply {
            writeBytes(plaintextHeader + ByteArray(64))
        }
        val tempEncFile = File(context.cacheDir, "enc_test.db").apply {
            writeBytes(encryptedSaltHeader + ByteArray(64))
        }
        try {
            assertFalse(PureLockDatabase.isEncryptedDatabaseFile(tempPlainFile))
            assertTrue(PureLockDatabase.isEncryptedDatabaseFile(tempEncFile))
        } finally {
            tempPlainFile.delete()
            tempEncFile.delete()
        }
    }

    @Test
    fun `room database schema supports vault app lock logs and settings roundtrip`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, PureLockDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val insertedId = db.encryptedVaultDao().insertVaultItem(
                EncryptedVaultEntity(
                    title = "Hardware Key",
                    secretContent = "s3cr3t-value",
                    category = "PASSWORD",
                    username = "admin@purelock.local",
                    isFavorite = true
                )
            )
            val fetched = db.encryptedVaultDao().getVaultItemById(insertedId)
            assertNotNull(fetched)
            assertEquals("Hardware Key", fetched?.title)
            assertEquals("s3cr3t-value", fetched?.secretContent)
            assertTrue(fetched?.isFavorite == true)

            db.appLockDao().upsertApp(
                LockedAppEntity(
                    packageName = "com.example.banking",
                    appName = "Banking",
                    category = "FINANCIAL",
                    isLocked = true
                )
            )
            val lockedApps = db.appLockDao().getActiveLockedApps().first()
            assertEquals(1, lockedApps.size)
            assertEquals("com.example.banking", lockedApps.first().packageName)

            db.userSettingDao().upsertSetting(UserSettingEntity("theme_mode", "DARK"))
            assertEquals("DARK", db.userSettingDao().getSetting("theme_mode")?.value)

            db.logDao().insertLog(SecurityLogEntity(action = "DB_TEST", details = "Verified"))
            assertEquals(1, db.logDao().getAllLogs().first().size)
        } finally {
            db.close()
        }
    }
}
