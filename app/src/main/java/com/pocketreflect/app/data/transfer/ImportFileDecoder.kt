// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.transfer

import com.pocketreflect.app.data.export.vault.VaultExportFile
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Arrays
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Иерархия ошибок при импорте резервных копий.
 */
sealed class ImportError(message: String) : Exception(message) {
    class WrongPasswordOrCorrupt : ImportError("Wrong password or file corrupted")
    class NotABackup : ImportError("This is not a Sanctum backup file")
    class UnsupportedFormat : ImportError("Backup file created in a newer version of Sanctum")
}

/**
 * Расшифровывает поток `.sanctum` обратно в [TransferFileDto].
 *
 * Все ошибки маппятся в строгую типизированную иерархию [ImportError],
 * которая на уровне UI локализуется в человекочитаемые сообщения.
 *
 * Stream не закрывается — это ответственность вызывающего кода.
 */
@Singleton
class ImportFileDecoder @Inject constructor(
    private val json: Json,
) {

    fun decode(input: InputStream, password: CharArray): TransferFileDto {
        val plainBytes = decodeBytes(input = input, password = password, magic = ExportFile.MAGIC)
        try {
            val jsonText = String(plainBytes, Charsets.UTF_8)
            return json.decodeFromString(TransferFileDto.serializer(), jsonText)
        } catch (_: SerializationException) {
            throw ImportError.WrongPasswordOrCorrupt()
        } finally {
            Arrays.fill(plainBytes, 0)
        }
    }

    /**
     * Расшифровывает произвольный payload с проверкой magic (например ZIP vault).
     */
    fun decodeBytes(input: InputStream, password: CharArray, magic: ByteArray): ByteArray {
        val header = readHeader(input, expectedMagic = magic)
        val cipherText = readToEnd(input)

        val keyBytes = derivePbkdf2Key(password, header.salt, header.iterations)
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val plainBytes: ByteArray = try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey,
                GCMParameterSpec(ExportFile.GCM_TAG_BITS, header.iv),
            )
            cipher.doFinal(cipherText)
        } catch (_: AEADBadTagException) {
            throw ImportError.WrongPasswordOrCorrupt()
        } catch (_: javax.crypto.BadPaddingException) {
            throw ImportError.WrongPasswordOrCorrupt()
        } catch (_: javax.crypto.IllegalBlockSizeException) {
            throw ImportError.WrongPasswordOrCorrupt()
        } finally {
            Arrays.fill(keyBytes, 0)
        }
        return plainBytes
    }

    private data class Header(
        val iterations: Int,
        val salt: ByteArray,
        val iv: ByteArray,
    )

    private fun readHeader(input: InputStream, expectedMagic: ByteArray): Header {
        val raw = ByteArray(ExportFile.HEADER_LEN)
        readFully(input, raw, raw.size)

        for (i in expectedMagic.indices) {
            if (raw[i] != expectedMagic[i]) {
                throw ImportError.NotABackup()
            }
        }
        if (raw[ExportFile.MAGIC_LEN] != ExportFile.FORMAT_VERSION &&
            raw[ExportFile.MAGIC_LEN] != VaultExportFile.FORMAT_VERSION
        ) {
            throw ImportError.UnsupportedFormat()
        }
        val iterations = ByteBuffer.wrap(raw, ExportFile.MAGIC_LEN + 1, 4)
            .order(ByteOrder.BIG_ENDIAN)
            .int
        if (iterations <= 0) {
            throw ImportError.NotABackup()
        }
        val saltStart = ExportFile.MAGIC_LEN + 1 + 4
        val salt = raw.copyOfRange(saltStart, saltStart + ExportFile.SALT_LEN)
        val ivStart = saltStart + ExportFile.SALT_LEN
        val iv = raw.copyOfRange(ivStart, ivStart + ExportFile.IV_LEN)
        return Header(iterations = iterations, salt = salt, iv = iv)
    }

    private fun readFully(input: InputStream, dst: ByteArray, expected: Int) {
        var off = 0
        while (off < expected) {
            val read = try {
                input.read(dst, off, expected - off)
            } catch (_: IOException) {
                throw ImportError.NotABackup()
            }
            if (read < 0) throw ImportError.NotABackup()
            off += read
        }
    }

    private fun readToEnd(input: InputStream): ByteArray {
        return try {
            input.readBytes()
        } catch (_: IOException) {
            throw ImportError.WrongPasswordOrCorrupt()
        }
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
