// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.transfer

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Сериализует [TransferFileDto] в зашифрованный поток.
 *
 * Алгоритмы (см. [ExportFile]):
 *  - KDF: PBKDF2-HMAC-SHA256, 200_000 итераций, 16-байтовая случайная соль.
 *  - AEAD: AES-256/GCM/NoPadding, 12-байтовый случайный nonce, 128-битный tag.
 *
 * Безопасность пароля:
 *  - `password` принимаем как [CharArray] (не [String]) — `String` иммутабелен
 *    и живёт в string pool, его невозможно стереть из памяти.
 *  - Derived key и raw byte-форма пароля зануляются сразу после использования
 *    через [Arrays.fill]. Сам [CharArray] остаётся у вызывающего кода
 *    (его задача — занулить после возврата).
 *
 * Поток `out` не закрывается — закрытие — обязанность вызывающего
 * (он, скорее всего, владелец `ContentResolver.openOutputStream(...)`).
 */
@Singleton
internal class ExportFileEncoder @Inject constructor(
    private val json: Json,
) {

    /**
     * Кодирует [payload] в `out`. Использует свежую соль и nonce — поэтому
     * один и тот же payload с одним и тем же паролем даёт разный ciphertext.
     */
    fun encode(
        payload: TransferFileDto,
        password: CharArray,
        out: OutputStream,
        iterations: Int = ExportFile.DEFAULT_PBKDF2_ITERATIONS,
    ) {
        val plainJson = json.encodeToString(payload)
        encodeBytes(
            payload = plainJson.toByteArray(Charsets.UTF_8),
            password = password,
            out = out,
            magic = ExportFile.MAGIC,
            formatVersion = ExportFile.FORMAT_VERSION,
            iterations = iterations,
        )
    }

    /**
     * Шифрует произвольный payload (например ZIP vault) с указанным magic.
     */
    fun encodeBytes(
        payload: ByteArray,
        password: CharArray,
        out: OutputStream,
        magic: ByteArray,
        formatVersion: Byte,
        iterations: Int = ExportFile.DEFAULT_PBKDF2_ITERATIONS,
    ) {
        val random = SecureRandom()
        val salt = ByteArray(ExportFile.SALT_LEN).also(random::nextBytes)
        val iv = ByteArray(ExportFile.IV_LEN).also(random::nextBytes)

        val plainBytes = payload.copyOf()
        val keyBytes = derivePbkdf2Key(password, salt, iterations)
        val secretKey = SecretKeySpec(keyBytes, "AES")
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.ENCRYPT_MODE,
                secretKey,
                GCMParameterSpec(ExportFile.GCM_TAG_BITS, iv),
            )
            val cipherText = cipher.doFinal(plainBytes)

            writeHeader(out, magic = magic, formatVersion = formatVersion, iterations = iterations, salt = salt, iv = iv)
            out.write(cipherText)
            out.flush()
        } finally {
            Arrays.fill(keyBytes, 0)
            Arrays.fill(plainBytes, 0)
        }
    }

    private fun writeHeader(
        out: OutputStream,
        magic: ByteArray,
        formatVersion: Byte,
        iterations: Int,
        salt: ByteArray,
        iv: ByteArray,
    ) {
        out.write(magic)
        out.write(byteArrayOf(formatVersion))
        out.write(
            ByteBuffer.allocate(4)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(iterations)
                .array()
        )
        out.write(salt)
        out.write(iv)
    }

    private fun derivePbkdf2Key(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, ExportFile.KEY_BITS)
        try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            return factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
