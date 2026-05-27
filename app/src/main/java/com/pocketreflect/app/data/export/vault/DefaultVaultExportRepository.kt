// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.export.vault

import com.pocketreflect.app.core.locale.AppLanguage
import com.pocketreflect.app.core.locale.AppLanguageResolver
import com.pocketreflect.app.core.time.Clock
import com.pocketreflect.app.data.local.entity.AITrendProfile
import com.pocketreflect.app.data.local.entity.JournalEntry
import com.pocketreflect.app.data.repository.ImportMergeReport
import com.pocketreflect.app.data.repository.JournalRepository
import com.pocketreflect.app.data.repository.UserPreferencesRepository
import com.pocketreflect.app.data.transfer.ExportFileEncoder
import com.pocketreflect.app.data.transfer.ImportError
import com.pocketreflect.app.data.transfer.ImportFileDecoder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@Singleton
internal class DefaultVaultExportRepository @Inject constructor(
    private val journalRepository: JournalRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val appLanguageResolver: AppLanguageResolver,
    private val clock: Clock,
    private val exportFileEncoder: ExportFileEncoder,
    private val importFileDecoder: ImportFileDecoder,
) : VaultExportRepository {

    override suspend fun export(
        out: OutputStream,
        options: VaultExportOptions,
        password: CharArray?,
    ): VaultExportSummary = withContext(Dispatchers.IO) {
        if (options.encrypt && password == null) {
            error("Password required for encrypted vault export")
        }

        val language = appLanguageResolver.resolvedNow()
        val entries = journalRepository.findAllEntries().sortedBy { it.dayBucket }
        val profiles = if (options.includeWeeklyProfiles) {
            journalRepository.findAllProfiles().sortedBy { it.generatedAt }
        } else {
            emptyList()
        }
        val manifesto = if (options.includeManifesto) {
            userPreferencesRepository.personalManifesto.first()
        } else {
            ""
        }

        val zipBytes = buildZipBytes(
            exportedAt = clock.nowMillis(),
            entries = entries,
            profiles = profiles,
            manifesto = manifesto,
            options = options,
            language = language,
        )

        if (options.encrypt) {
            exportFileEncoder.encodeBytes(
                payload = zipBytes,
                password = password!!,
                out = out,
                magic = VaultExportFile.MAGIC,
                formatVersion = VaultExportFile.FORMAT_VERSION,
            )
        } else {
            out.write(zipBytes)
            out.flush()
        }

        VaultExportSummary(
            entries = entries.size,
            weeklyProfiles = profiles.size,
            includesManifesto = options.includeManifesto && manifesto.isNotBlank(),
            encrypted = options.encrypt,
        )
    }

    override suspend fun importFromStream(
        input: InputStream,
        password: CharArray?,
        overwrite: Boolean,
    ): ImportMergeReport = withContext(Dispatchers.IO) {
        val peek = input.markSupported()
        val zipBytes = if (peek) {
            input.mark(VaultExportFile.HEADER_LEN)
            val header = ByteArray(VaultExportFile.HEADER_LEN)
            val read = input.read(header)
            input.reset()
            if (read == VaultExportFile.HEADER_LEN && header.startsWith(VaultExportFile.MAGIC)) {
                if (password == null) throw VaultImportError.WrongPasswordOrCorrupt()
                try {
                    importFileDecoder.decodeBytes(
                        input = input,
                        password = password,
                        magic = VaultExportFile.MAGIC,
                    )
                } catch (_: ImportError.WrongPasswordOrCorrupt) {
                    throw VaultImportError.WrongPasswordOrCorrupt()
                } catch (_: ImportError.NotABackup) {
                    throw VaultImportError.NotAVault()
                } catch (_: ImportError.UnsupportedFormat) {
                    throw VaultImportError.UnsupportedFormat()
                }
            } else {
                input.readBytes()
            }
        } else {
            input.readBytes()
        }

        if (!isZipBytes(zipBytes)) {
            throw VaultImportError.NotAVault()
        }

        val entries = parseJournalEntries(zipBytes)
        if (entries.isEmpty()) {
            throw VaultImportError.NoEntries()
        }

        journalRepository.mergeImport(
            entries = entries,
            profiles = emptyList(),
            overwrite = overwrite,
        )
    }

    private fun buildZipBytes(
        exportedAt: Long,
        entries: List<JournalEntry>,
        profiles: List<AITrendProfile>,
        manifesto: String,
        options: VaultExportOptions,
        language: AppLanguage,
    ): ByteArray {
        val exportDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val root = "${ObsidianExportSchema.ROOT_PREFIX}-$exportDate"
        val files = mutableListOf<VaultArchiveFile>()

        files += VaultArchiveFile(
            path = "$root/${ObsidianExportSchema.META_MANIFEST}",
            content = VaultManifestBuilder.build(
                exportedAt = exportedAt,
                entryCount = entries.size,
                weeklyCount = profiles.size,
                includeManifesto = options.includeManifesto && manifesto.isNotBlank(),
                localeTag = language.storageKey ?: "system",
                appVersion = ObsidianExportSchema.EXPORTER_VERSION,
            ).toByteArray(Charsets.UTF_8),
        )

        entries.forEach { entry ->
            files += VaultArchiveFile(
                path = "$root/${ObsidianExportSchema.JOURNAL_DIR}${entry.dayBucket}.md",
                content = MarkdownEntryRenderer.render(entry, language).toByteArray(Charsets.UTF_8),
            )
        }

        profiles.forEach { profile ->
            files += VaultArchiveFile(
                path = "$root/${ObsidianExportSchema.WEEKLY_DIR}${
                    MarkdownWeeklyProfileRenderer.weekFileName(profile.periodEnd)
                }",
                content = MarkdownWeeklyProfileRenderer.render(profile, language)
                    .toByteArray(Charsets.UTF_8),
            )
        }

        if (options.includeManifesto && manifesto.isNotBlank()) {
            files += VaultArchiveFile(
                path = "$root/${ObsidianExportSchema.MANIFESTO_FILE}",
                content = MarkdownManifestoRenderer.render(manifesto, language)
                    .toByteArray(Charsets.UTF_8),
            )
        }

        return ByteArrayOutputStream().use { buffer ->
            VaultZipWriter.write(files, buffer)
            buffer.toByteArray()
        }
    }

    private fun parseJournalEntries(zipBytes: ByteArray): List<JournalEntry> =
        VaultZipReader.readAll(ByteArrayInputStream(zipBytes))
            .asSequence()
            .filter { entry ->
                entry.path.contains("/${ObsidianExportSchema.JOURNAL_DIR}") &&
                    entry.path.endsWith(".md", ignoreCase = true)
            }
            .mapNotNull { entry ->
                MarkdownFrontMatterParser.parseJournalEntry(
                    entry.content.toString(Charsets.UTF_8),
                )
            }
            .distinctBy { it.dayBucket }
            .sortedBy { it.dayBucket }
            .toList()

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (size < prefix.size) return false
        for (i in prefix.indices) {
            if (this[i] != prefix[i]) return false
        }
        return true
    }

    private fun isZipBytes(bytes: ByteArray): Boolean =
        bytes.size >= 4 &&
            bytes[0] == 0x50.toByte() &&
            bytes[1] == 0x4B.toByte()
}
