// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.transfer

/**
 * Константы формата файла `.pocketreflect`.
 *
 * Структура (бинарь):
 * ```
 * +------------------------------------------+
 * | Magic "PRFL"            4 bytes ASCII    |
 * +------------------------------------------+
 * | formatVersion           1 byte (u8)      |
 * +------------------------------------------+
 * | pbkdf2Iterations        4 bytes (u32 BE) |
 * +------------------------------------------+
 * | salt                    16 bytes         |
 * +------------------------------------------+
 * | iv (GCM nonce)          12 bytes         |
 * +------------------------------------------+
 * | ciphertext + GCM tag    var (tag = 16B)  |
 * +------------------------------------------+
 * ```
 *
 * Все поля заголовка — plaintext, чтобы пользователь без пароля не смог
 * сломать структуру файла, но также не получил никакой информации
 * о содержимом (заголовок не содержит даже количества записей).
 *
 * `formatVersion` отделён от `schemaVersion` внутри JSON-payload:
 *  - bump `formatVersion` — когда меняется бинарная упаковка (например,
 *    переход на ChaCha20-Poly1305 или потоковую упаковку через AAD);
 *  - bump `schemaVersion` — когда меняется структура `JournalEntry` /
 *    `AITrendProfile` (синхронно с Room DB version).
 */
internal object ExportFile {

    /** ASCII «PRFL». Не печатается легитимными редакторами как «текст». */
    val MAGIC: ByteArray = byteArrayOf(0x50, 0x52, 0x46, 0x4C)
    const val MAGIC_LEN: Int = 4

    /** Текущая версия упаковки файла. */
    const val FORMAT_VERSION: Byte = 1

    const val SALT_LEN: Int = 16
    const val IV_LEN: Int = 12

    /** AES-256: 32-байтовый derived key. */
    const val KEY_BITS: Int = 256

    /**
     * GCM authentication tag. 128 бит — стандарт для AES-GCM,
     * не сокращаем: ради сильной защиты от подделки файла.
     */
    const val GCM_TAG_BITS: Int = 128

    /**
     * PBKDF2 iterations по умолчанию.
     *
     * 200_000 — отправная планка, согласованная с OWASP'24 для PBKDF2-HMAC-SHA256.
     * Само число пишется в заголовок файла, чтобы будущие версии могли
     * увеличить его без breakage старых файлов.
     */
    const val DEFAULT_PBKDF2_ITERATIONS: Int = 200_000

    /**
     * Версия схемы JSON-payload. Совпадает с `schemaVersion` в DTO.
     * Будет bumb-нута синхронно с Room DB version при следующей миграции.
     */
    const val TRANSFER_SCHEMA_VERSION: Int = 1

    /** Размер заголовка целиком — для проверки «файл хотя бы достаточно длинный». */
    const val HEADER_LEN: Int = MAGIC_LEN + 1 + 4 + SALT_LEN + IV_LEN
}
