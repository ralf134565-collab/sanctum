// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.model

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.security.MessageDigest

/**
 * Юнит-тесты на `ModelStorage.copyAndVerify`.
 *
 * Низкоуровневый API специально не привязан к [ModelManifest] — он принимает
 * `expectedSizeBytes` и `expectedSha256` как параметры. Это позволяет
 * проверить алгоритм на крошечном payload'е (~10 KB), вычислив реальный
 * SHA-256 в тесте, без воспроизведения 2.6 GB референса.
 */
class ModelStorageTest {

    @get:Rule
    val tempFolder: TemporaryFolder = TemporaryFolder()

    private lateinit var modelsDir: File
    private lateinit var storage: ModelStorage

    @Before
    fun setUp() {
        modelsDir = tempFolder.newFolder("models")
        storage = ModelStorage(modelsDir)
    }

    @Test
    fun `copyAndVerify succeeds when payload matches expected sha and size`() = runBlocking {
        val payload = "Pocket reflect is local-first. ".repeat(200).toByteArray(Charsets.UTF_8)
        val expectedSha = MessageDigest.getInstance("SHA-256").digest(payload).toHexForTest()
        val target = File(modelsDir, "demo.bin")

        var lastBytes = 0L
        val outcome = storage.copyAndVerify(
            input = ByteArrayInputStream(payload),
            expectedSizeBytes = payload.size.toLong(),
            expectedSha256 = expectedSha,
            targetFinalFile = target,
            onProgress = { copied, _ -> lastBytes = copied },
        )

        assertTrue("Ожидали Success, получили $outcome", outcome is ModelStorage.CopyOutcome.Success)
        val success = outcome as ModelStorage.CopyOutcome.Success
        assertEquals(target.absolutePath, success.file.absolutePath)
        assertEquals(payload.size.toLong(), success.sizeBytes)
        assertEquals(expectedSha, success.sha256Hex)
        assertTrue("Финальный файл должен существовать", target.exists())
        assertEquals(payload.size.toLong(), target.length())
        assertEquals(payload.size.toLong(), lastBytes)
        assertFalse(
            "Временный файл должен быть переименован, а не остаться",
            File(modelsDir, "demo.bin.tmp").exists(),
        )
    }

    @Test
    fun `copyAndVerify fails with IntegrityFailed on wrong sha and cleans up tmp`() = runBlocking {
        val payload = "verify-me".toByteArray(Charsets.UTF_8)
        val target = File(modelsDir, "demo.bin")
        val wrongSha = "0".repeat(64)

        val outcome = storage.copyAndVerify(
            input = ByteArrayInputStream(payload),
            expectedSizeBytes = payload.size.toLong(),
            expectedSha256 = wrongSha,
            targetFinalFile = target,
            onProgress = { _, _ -> },
        )

        assertTrue(
            "При несовпадении SHA должен быть IntegrityFailed, получили $outcome",
            outcome is ModelStorage.CopyOutcome.IntegrityFailed,
        )
        val failed = outcome as ModelStorage.CopyOutcome.IntegrityFailed
        assertEquals(wrongSha, failed.expectedSha256)
        assertEquals(payload.size.toLong(), failed.actualSizeBytes)
        assertFalse("Финальный файл не должен появиться", target.exists())
        assertFalse(
            "Временный файл должен быть удалён",
            File(modelsDir, "demo.bin.tmp").exists(),
        )
    }

    @Test
    fun `copyAndVerify fails with IntegrityFailed on size mismatch`() = runBlocking {
        val payload = "exact-10-b".toByteArray(Charsets.UTF_8)
        val sha = MessageDigest.getInstance("SHA-256").digest(payload).toHexForTest()
        val target = File(modelsDir, "demo.bin")

        val outcome = storage.copyAndVerify(
            input = ByteArrayInputStream(payload),
            expectedSizeBytes = 999L,
            expectedSha256 = sha,
            targetFinalFile = target,
            onProgress = { _, _ -> },
        )

        assertTrue(
            "При несовпадении размера должен быть IntegrityFailed, получили $outcome",
            outcome is ModelStorage.CopyOutcome.IntegrityFailed,
        )
        assertFalse(target.exists())
    }

    @Test
    fun `existingPath returns null when file is missing and File when present`() = runBlocking {
        assertEquals(null, storage.existingPath(ModelVariant.E2B))
        val payload = "any".toByteArray()
        val target = storage.resolvedFile(ModelVariant.E2B)
        val sha = MessageDigest.getInstance("SHA-256").digest(payload).toHexForTest()
        storage.copyAndVerify(
            input = ByteArrayInputStream(payload),
            expectedSizeBytes = payload.size.toLong(),
            expectedSha256 = sha,
            targetFinalFile = target,
            onProgress = { _, _ -> },
        )
        assertNotNull(storage.existingPath(ModelVariant.E2B))
    }

    @Test
    fun `delete removes the file and returns true when present`() = runBlocking {
        val payload = "delete-me".toByteArray()
        val target = storage.resolvedFile(ModelVariant.E4B)
        val sha = MessageDigest.getInstance("SHA-256").digest(payload).toHexForTest()
        storage.copyAndVerify(
            input = ByteArrayInputStream(payload),
            expectedSizeBytes = payload.size.toLong(),
            expectedSha256 = sha,
            targetFinalFile = target,
            onProgress = { _, _ -> },
        )
        assertTrue(target.exists())

        assertTrue(storage.delete(ModelVariant.E4B))
        assertFalse(target.exists())
        // Повторный delete на отсутствующем файле — тоже success.
        assertTrue(storage.delete(ModelVariant.E4B))
    }

    private fun ByteArray.toHexForTest(): String = joinToString("") {
        "%02x".format(it.toInt() and 0xFF)
    }
}
