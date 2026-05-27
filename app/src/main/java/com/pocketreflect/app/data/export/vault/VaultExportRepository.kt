// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.export.vault

import com.pocketreflect.app.data.repository.ImportMergeReport

/**
 * Опции Markdown/ZIP-экспорта для Obsidian и других PKM.
 */
data class VaultExportOptions(
    val includeWeeklyProfiles: Boolean = true,
    val includeManifesto: Boolean = true,
    val encrypt: Boolean = false,
)

data class VaultExportSummary(
    val entries: Int,
    val weeklyProfiles: Int,
    val includesManifesto: Boolean,
    val encrypted: Boolean,
)

sealed class VaultImportError(message: String) : Exception(message) {
    class WrongPasswordOrCorrupt : VaultImportError("Wrong password or corrupted vault")
    class NotAVault : VaultImportError("Not a Sanctum vault export")
    class UnsupportedFormat : VaultImportError("Vault created in a newer version")
    class NoEntries : VaultImportError("No journal entries found in archive")
}

interface VaultExportRepository {
    suspend fun export(
        out: java.io.OutputStream,
        options: VaultExportOptions,
        password: CharArray?,
    ): VaultExportSummary

    suspend fun importFromStream(
        input: java.io.InputStream,
        password: CharArray?,
        overwrite: Boolean,
    ): ImportMergeReport
}
