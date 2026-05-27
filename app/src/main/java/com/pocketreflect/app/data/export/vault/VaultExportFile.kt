// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.export.vault

import com.pocketreflect.app.data.transfer.ExportFile

/**
 * Бинарная упаковка зашифрованного Markdown-vault (magic `PRFM`).
 *
 * Заголовок идентичен [ExportFile] (`.sanctum`), plaintext — ZIP-байты vault'а.
 */
internal object VaultExportFile {
    /** ASCII «PRFM» — Pocket Reflect / Markdown vault. */
    val MAGIC: ByteArray = byteArrayOf(0x50, 0x52, 0x46, 0x4D)

    const val FORMAT_VERSION: Byte = 1

    const val HEADER_LEN: Int = ExportFile.HEADER_LEN
    const val SALT_LEN: Int = ExportFile.SALT_LEN
    const val IV_LEN: Int = ExportFile.IV_LEN
    const val GCM_TAG_BITS: Int = ExportFile.GCM_TAG_BITS
    const val KEY_BITS: Int = ExportFile.KEY_BITS
    const val DEFAULT_PBKDF2_ITERATIONS: Int = ExportFile.DEFAULT_PBKDF2_ITERATIONS
}
