// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

/**
 * Файловое хранилище подключённой модели.
 *
 * Жизненный цикл одного файла:
 *  1. Пользователь выбирает скачанный `.litertlm` через SAF — это даёт `Uri`.
 *  2. UI открывает `InputStream` через `ContentResolver` и передаёт сюда.
 *  3. [copyAndVerify] копирует поток в `<modelsDir>/<final>.tmp`, одновременно
 *     обновляя `MessageDigest("SHA-256")`. Один проход — нельзя читать
 *     многогигабайтный файл дважды.
 *  4. По завершении сравниваем размер и хеш с захардкоженными ожиданиями
 *     ([ModelManifest]). Только при полном совпадении делаем атомарный
 *     `renameTo(final)` — иначе удаляем `.tmp` и возвращаем [CopyOutcome.IntegrityFailed].
 *  5. На любой `Throwable` в середине процесса `.tmp` удаляется.
 *
 * Класс намеренно НЕ берёт `Context` напрямую — принимает уже резолвленный
 * `modelsDir`. Это позволяет писать pure-JVM тесты с `TemporaryFolder`,
 * без Robolectric. Production-инстанс собирается в `DataStoreModule`.
 */
class ModelStorage(private val modelsDir: File) {

    fun resolvedFile(variant: ModelVariant): File = File(ensureDir(), variant.storageFileName)

    /**
     * Файл присутствует И ненулевого размера. Размер `0` означает обрыв
     * предыдущего копирования, который не успел удалить файл.
     */
    fun existingPath(variant: ModelVariant): File? =
        resolvedFile(variant).takeIf { it.exists() && it.length() > 0 }

    fun delete(variant: ModelVariant): Boolean {
        val file = resolvedFile(variant)
        return if (file.exists()) file.delete() else true
    }

    /**
     * Однопроходное copy + SHA-256 + size verify.
     *
     * Вынесено в публичный API отдельно от [attach], чтобы юнит-тесты могли
     * прогонять алгоритм на маленьких payload'ах со своими ожидаемыми хешами,
     * не пытаясь воспроизводить 2.6 GB референс из манифеста.
     */
    suspend fun copyAndVerify(
        input: InputStream,
        expectedSizeBytes: Long,
        expectedSha256: String,
        targetFinalFile: File,
        onProgress: (bytesCopied: Long, expectedTotalBytes: Long) -> Unit,
    ): CopyOutcome = withContext(Dispatchers.IO) {
        ensureDir()
        val parent = targetFinalFile.parentFile ?: modelsDir
        if (!parent.exists()) parent.mkdirs()
        val tmpFile = File(parent, "${targetFinalFile.name}.tmp")
        if (tmpFile.exists()) tmpFile.delete()

        val digest = MessageDigest.getInstance("SHA-256")
        var bytesCopied = 0L
        try {
            FileOutputStream(tmpFile).use { out ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    out.write(buffer, 0, read)
                    digest.update(buffer, 0, read)
                    bytesCopied += read
                    onProgress(bytesCopied, expectedSizeBytes)
                }
                out.flush()
                // Гарантируем, что байты физически дошли до диска до renameTo.
                // На некоторых эмуляторах без sync renameTo «успешен», но данные
                // могут пропасть при ребуте.
                runCatching { out.fd.sync() }
            }
        } catch (t: Throwable) {
            tmpFile.delete()
            throw t
        }

        val actualSha = digest.digest().toLowerHex()
        if (bytesCopied != expectedSizeBytes || actualSha != expectedSha256) {
            tmpFile.delete()
            return@withContext CopyOutcome.IntegrityFailed(
                expectedSha256 = expectedSha256,
                actualSha256 = actualSha,
                expectedSizeBytes = expectedSizeBytes,
                actualSizeBytes = bytesCopied,
            )
        }

        if (targetFinalFile.exists()) targetFinalFile.delete()
        val renamed = tmpFile.renameTo(targetFinalFile)
        if (!renamed) {
            tmpFile.delete()
            return@withContext CopyOutcome.RenameFailed
        }
        CopyOutcome.Success(
            file = targetFinalFile,
            sha256Hex = actualSha,
            sizeBytes = bytesCopied,
        )
    }

    /**
     * Высокоуровневая обёртка для production-кода: ожидаемые размер и хеш
     * берутся из [ModelManifest], а целевой файл — из [resolvedFile].
     */
    suspend fun attach(
        variant: ModelVariant,
        input: InputStream,
        onProgress: (bytesCopied: Long, expectedTotalBytes: Long) -> Unit,
    ): CopyOutcome {
        val entry = ModelManifest.entryOf(variant)
        return copyAndVerify(
            input = input,
            expectedSizeBytes = entry.expectedSizeBytes,
            expectedSha256 = entry.expectedSha256,
            targetFinalFile = resolvedFile(variant),
            onProgress = onProgress,
        )
    }

    private fun ensureDir(): File {
        if (!modelsDir.exists()) modelsDir.mkdirs()
        return modelsDir
    }

    sealed interface CopyOutcome {
        data class Success(
            val file: File,
            val sha256Hex: String,
            val sizeBytes: Long,
        ) : CopyOutcome

        data class IntegrityFailed(
            val expectedSha256: String,
            val actualSha256: String,
            val expectedSizeBytes: Long,
            val actualSizeBytes: Long,
        ) : CopyOutcome

        data object RenameFailed : CopyOutcome
    }

    private companion object {
        // 4 MB — баланс между throughput и нагрузкой на GC. Меньше — overhead
        // на синхронизацию буфера, больше — ощутимое давление на native heap.
        const val BUFFER_SIZE = 4 * 1024 * 1024
    }
}
