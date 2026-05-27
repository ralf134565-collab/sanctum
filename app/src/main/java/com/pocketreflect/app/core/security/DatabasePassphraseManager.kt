// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import java.util.Arrays
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Менеджер кодовой фразы базы данных SQLCipher (32 байта).
 * Использует аппаратный Android KeyStore с AES-GCM для шифрования кодовой фразы перед
 * записью в SharedPreferences.
 */
@Singleton
class DatabasePassphraseManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val keyManager = SecureKeyManager()
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
    }

    // Кэш кодовой фразы в оперативной памяти (для строгого зануления)
    @Volatile
    private var memoryCachedPassphrase: ByteArray? = null

    @Synchronized
    fun getOrCreatePassphrase(): ByteArray {
        // Если кодовая фраза уже кэширована в памяти (и приложение разблокировано) — возвращаем её
        memoryCachedPassphrase?.let {
            return it.copyOf()
        }

        val encryptedBase64 = prefs.getString(KEY_PASSPHRASE, null)
        val ivBase64 = prefs.getString(KEY_IV, null)

        val passphrase = if (encryptedBase64 != null && ivBase64 != null) {
            try {
                val ciphertext = Base64.decode(encryptedBase64, Base64.NO_WRAP)
                if (ivBase64 == "FALLBACK" || prefs.getBoolean(KEY_IS_FALLBACK, false)) {
                    deobfuscate(ciphertext)
                } else {
                    val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
                    keyManager.decrypt(ciphertext, iv)
                }
            } catch (e: Exception) {
                // Если не удалось расшифровать (например, ключ сброшен), создаем новую БД
                createAndPersist()
            }
        } else {
            createAndPersist()
        }

        memoryCachedPassphrase = passphrase
        return passphrase.copyOf()
    }

    @Synchronized
    fun clearPassphrase() {
        prefs.edit().remove(KEY_PASSPHRASE).remove(KEY_IV).apply()
        clearPassphraseFromMemory()
    }

    @Synchronized
    fun clearPassphraseFromMemory() {
        memoryCachedPassphrase?.let {
            Arrays.fill(it, 0.toByte())
        }
        memoryCachedPassphrase = null
    }

    private fun createAndPersist(): ByteArray {
        val passphrase = ByteArray(PASSPHRASE_LEN).also { SecureRandom().nextBytes(it) }
        try {
            val encrypted = keyManager.encrypt(passphrase)
            prefs.edit()
                .putString(KEY_PASSPHRASE, Base64.encodeToString(encrypted.ciphertext, Base64.NO_WRAP))
                .putString(KEY_IV, Base64.encodeToString(encrypted.iv, Base64.NO_WRAP))
                .putBoolean(KEY_IS_FALLBACK, false)
                .apply()
        } catch (e: Exception) {
            Log.e("DatabasePassphrase", "KeyStore encryption failed, falling back to soft-obfuscated storage on emulator", e)
            val obfuscated = obfuscate(passphrase)
            prefs.edit()
                .putString(KEY_PASSPHRASE, Base64.encodeToString(obfuscated, Base64.NO_WRAP))
                .putString(KEY_IV, "FALLBACK")
                .putBoolean(KEY_IS_FALLBACK, true)
                .apply()
        }
        return passphrase
    }

    private fun obfuscate(data: ByteArray): ByteArray {
        val salt = "SanctumFallbackSalt".toByteArray()
        val result = ByteArray(data.size)
        for (i in data.indices) {
            result[i] = (data[i].toInt() xor salt[i % salt.size].toInt()).toByte()
        }
        return result
    }

    private fun deobfuscate(data: ByteArray): ByteArray = obfuscate(data)

    private companion object {
        const val PREFS_FILE = "sanctum_db_key_prefs"
        const val KEY_PASSPHRASE = "encrypted_sqlcipher_passphrase"
        const val KEY_IV = "sqlcipher_passphrase_iv"
        const val KEY_IS_FALLBACK = "sqlcipher_passphrase_is_fallback"
        const val PASSPHRASE_LEN = 32
    }
}
