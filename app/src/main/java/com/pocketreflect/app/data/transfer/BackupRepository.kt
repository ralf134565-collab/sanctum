// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.transfer

import com.pocketreflect.app.core.time.Clock
import com.pocketreflect.app.data.repository.ImportMergeReport
import com.pocketreflect.app.data.repository.JournalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Координатор зашифрованного экспорта/импорта.
 *
 * Внешний контракт — две suspend-функции, обе на `Dispatchers.IO`:
 *  - [export] читает БД, сериализует, шифрует, пишет в [OutputStream]
 *    (поток обычно из `ContentResolver.openOutputStream(uri)` — Storage Access Framework).
 *  - [import] читает из [InputStream], расшифровывает, парсит, передаёт
 *    в [JournalRepository.mergeImport].
 *
 * Все ошибки нормализуются в [IllegalArgumentException] с человекочитаемым
 * сообщением (см. [ImportFileDecoder]). Это даёт UI-слою стабильную поверхность
 * для snackbar'ов без `try/catch` на каждую конкретную причину.
 */
interface BackupRepository {
    suspend fun export(
        out: OutputStream,
        password: CharArray,
    ): ExportSummary

    suspend fun import(
        input: InputStream,
        password: CharArray,
        overwrite: Boolean,
    ): ImportMergeReport

    suspend fun importDto(
        dto: TransferFileDto,
        overwrite: Boolean,
    ): ImportMergeReport
}

/**
 * Сколько чего экспортировано. Передаётся в UI для информативного snackbar'а
 * («Сохранено N записей»).
 */
data class ExportSummary(
    val entries: Int,
    val profiles: Int,
)

@Singleton
internal class DefaultBackupRepository @Inject constructor(
    private val journalRepository: JournalRepository,
    private val clock: Clock,
    private val encoder: ExportFileEncoder,
    private val decoder: ImportFileDecoder,
) : BackupRepository {

    override suspend fun export(
        out: OutputStream,
        password: CharArray,
    ): ExportSummary = withContext(Dispatchers.IO) {
        val entries = journalRepository.findAllEntries()
        val profiles = journalRepository.findAllProfiles()
        val payload = TransferFileDto(
            schemaVersion = ExportFile.TRANSFER_SCHEMA_VERSION,
            exportedAt = clock.nowMillis(),
            entries = entries.map { it.toDto() },
            profiles = profiles.map { it.toDto() },
        )
        encoder.encode(payload = payload, password = password, out = out)
        ExportSummary(entries = entries.size, profiles = profiles.size)
    }

    override suspend fun import(
        input: InputStream,
        password: CharArray,
        overwrite: Boolean,
    ): ImportMergeReport = withContext(Dispatchers.IO) {
        val dto = decoder.decode(input = input, password = password)
        importDto(dto, overwrite)
    }

    override suspend fun importDto(
        dto: TransferFileDto,
        overwrite: Boolean,
    ): ImportMergeReport = withContext(Dispatchers.IO) {
        val report = journalRepository.mergeImport(
            entries = dto.entries.map { it.toEntity() },
            profiles = dto.profiles.map { it.toEntity() },
            overwrite = overwrite,
        )
        report
    }
}
