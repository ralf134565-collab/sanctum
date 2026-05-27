// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.transfer

import kotlinx.serialization.json.Json
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * End-to-end проверки бинарного формата `.sanctum`.
 *
 * Контракты, которые тут защищены от регрессий:
 *  1. Round-trip без изменений payload'а.
 *  2. Кривой пароль → `ImportError.WrongPasswordOrCorrupt`
 *     для «not bit-correct ciphertext» и «wrong password» — атакующий не должен
 *     различать «пароль не тот» от «байт битый».
 *  3. Сломанный magic → `ImportError.NotABackup`.
 *  4. Мутированный ciphertext → как «битый или неверный пароль» (AEAD-тег ловит).
 *  5. Empty payload (нет записей) — корректный round-trip.
 *  6. Sanity-проверка, что plaintext НЕ виден в заголовке: первые 200 байт
 *     файла не содержат подстроки `dayBucket`.
 *
 * Сам по себе тест плоский (без MockK / Hilt): нам нужны только
 * `Json` от kotlinx, encoder и decoder, которые работают в JVM.
 */
class EncryptedTransferRoundTripTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    /**
     * Для скорости в тесте режем PBKDF2 до 1k итераций — это сокращает время
     * round-trip с ~500ms до ~5ms. Production-код по-прежнему пишет
     * `DEFAULT_PBKDF2_ITERATIONS` (200k); декодер берёт число из заголовка.
     */
    private val fastIterations = 1_000

    private val encoder = ExportFileEncoder(json)
    private val decoder = ImportFileDecoder(json)

    private fun samplePayload(includeData: Boolean = true): TransferFileDto = TransferFileDto(
        schemaVersion = ExportFile.TRANSFER_SCHEMA_VERSION,
        exportedAt = 1_715_000_000_000L,
        entries = if (!includeData) emptyList() else listOf(
            JournalEntryDto(
                timestamp = 1_715_000_000_000L,
                dayBucket = "2026-05-19",
                moodTags = listOf("calm", "focused"),
                microWins = "ran 5km",
                tomorrowTasks = "deploy phase B",
                reflection = "сегодня нормально",
                promptShown = "what went well",
                aiReflection = null,
            ),
            JournalEntryDto(
                timestamp = 1_714_000_000_000L,
                dayBucket = "2026-05-12",
                moodTags = listOf("tired"),
                microWins = "",
                tomorrowTasks = "",
                reflection = "слишком много встреч",
                promptShown = "what drained you",
                aiReflection = "ты устал от внешних коммуникаций",
            ),
        ),
        profiles = if (!includeData) emptyList() else listOf(
            AITrendProfileDto(
                periodStart = 1_713_000_000_000L,
                periodEnd = 1_715_000_000_000L,
                generatedAt = 1_715_000_000_000L,
                entryCount = 7,
                summary = "стабильная неделя с переменной нагрузкой",
                structuredJson = """{"top_tag":"focused"}""",
                schemaVersion = 1,
            ),
        ),
    )

    @Test
    fun `round trip восстанавливает payload без изменений`() {
        val payload = samplePayload()
        val password = "ОченьСильныйПароль-2026".toCharArray()
        val out = ByteArrayOutputStream()
        encoder.encode(payload, password.copyOf(), out, iterations = fastIterations)

        val decoded = decoder.decode(ByteArrayInputStream(out.toByteArray()), password.copyOf())

        assertEquals(payload, decoded)
    }

    @Test
    fun `пустой payload корректно проходит round trip`() {
        val payload = samplePayload(includeData = false)
        val password = "p".toCharArray()
        val out = ByteArrayOutputStream()
        encoder.encode(payload, password.copyOf(), out, iterations = fastIterations)

        val decoded = decoder.decode(ByteArrayInputStream(out.toByteArray()), password.copyOf())

        assertEquals(0, decoded.entries.size)
        assertEquals(0, decoded.profiles.size)
        assertEquals(payload.schemaVersion, decoded.schemaVersion)
    }

    @Test
    fun `неверный пароль → ImportError WrongPasswordOrCorrupt`() {
        val payload = samplePayload()
        val out = ByteArrayOutputStream()
        encoder.encode(payload, "правильный".toCharArray(), out, iterations = fastIterations)

        assertThrows(ImportError.WrongPasswordOrCorrupt::class.java) {
            decoder.decode(ByteArrayInputStream(out.toByteArray()), "неверный".toCharArray())
        }
    }

    @Test
    fun `битый magic → ImportError NotABackup`() {
        val payload = samplePayload()
        val out = ByteArrayOutputStream()
        encoder.encode(payload, "any".toCharArray(), out, iterations = fastIterations)

        val bytes = out.toByteArray()
        // Перепишем первые 4 байта на «JUNK», чтобы сломать magic.
        bytes[0] = 'J'.code.toByte()
        bytes[1] = 'U'.code.toByte()
        bytes[2] = 'N'.code.toByte()
        bytes[3] = 'K'.code.toByte()

        assertThrows(ImportError.NotABackup::class.java) {
            decoder.decode(ByteArrayInputStream(bytes), "any".toCharArray())
        }
    }

    @Test
    fun `мутированный ciphertext → ImportError WrongPasswordOrCorrupt`() {
        val payload = samplePayload()
        val out = ByteArrayOutputStream()
        encoder.encode(payload, "any".toCharArray(), out, iterations = fastIterations)

        val bytes = out.toByteArray()
        // Header (HEADER_LEN) — plain. Берём середину ciphertext, флипаем младший бит.
        val middle = (ExportFile.HEADER_LEN + bytes.size) / 2
        bytes[middle] = (bytes[middle].toInt() xor 0x01).toByte()

        assertThrows(ImportError.WrongPasswordOrCorrupt::class.java) {
            decoder.decode(ByteArrayInputStream(bytes), "any".toCharArray())
        }
    }

    @Test
    fun `первые 4 байта файла = ASCII PRFL и plaintext не утекает в заголовок`() {
        val payload = samplePayload()
        val out = ByteArrayOutputStream()
        encoder.encode(payload, "any".toCharArray(), out, iterations = fastIterations)

        val bytes = out.toByteArray()
        // Magic — ровно «PRFL».
        assertArrayEquals(ExportFile.MAGIC, bytes.copyOfRange(0, ExportFile.MAGIC_LEN))

        // В первых 200 байтах не должно быть JSON-полей: даже метаданные
        // (`dayBucket`, `moodTags`, `schemaVersion`) — внутри ciphertext.
        val head = String(
            bytes.copyOfRange(0, minOf(200, bytes.size)),
            Charsets.ISO_8859_1, // latin-1 показывает любой байт как символ — удобно для проверки
        )
        assertFalse("plaintext dayBucket leaked into header", head.contains("dayBucket"))
        assertFalse("plaintext moodTags leaked into header", head.contains("moodTags"))
        assertFalse("plaintext schemaVersion leaked into header", head.contains("schemaVersion"))
    }

    @Test
    fun `один и тот же payload с одним паролем даёт разный ciphertext (свежие salt и iv)`() {
        val payload = samplePayload()
        val pwd1 = "shared".toCharArray()
        val pwd2 = "shared".toCharArray()
        val out1 = ByteArrayOutputStream().also { encoder.encode(payload, pwd1, it, iterations = fastIterations) }
        val out2 = ByteArrayOutputStream().also { encoder.encode(payload, pwd2, it, iterations = fastIterations) }

        assertNotNull(out1.toByteArray())
        assertNotNull(out2.toByteArray())
        // Ciphertext'ы должны различаться — иначе у нас детерминированный
        // вывод и атакующий по равенству файлов определит «у вас не меняется payload».
        assertNotEquals(
            "ciphertexts must differ: salt+iv должны быть свежими на каждый encode",
            out1.toByteArray().toList(),
            out2.toByteArray().toList(),
        )
        // Но magic в обоих одинаков.
        assertTrue(
            out1.toByteArray().copyOfRange(0, 4).contentEquals(ExportFile.MAGIC)
        )
        assertTrue(
            out2.toByteArray().copyOfRange(0, 4).contentEquals(ExportFile.MAGIC)
        )
    }
}
