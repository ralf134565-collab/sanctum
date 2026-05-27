// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Защита от опечаток в хардкоженной таблице моделей.
 *
 * Эти проверки не валидируют, что хеши «правильные» (это невозможно без
 * скачивания 2.6+ GB файла), но защищают от опечаток вида:
 *  - размер с лишним/потерянным нулём;
 *  - SHA-256 с неправильным числом символов или нерегулярным регистром;
 *  - битая ссылка на HuggingFace.
 */
class ModelManifestTest {

    @Test
    fun `every variant has populated entry`() {
        ModelVariant.entries.forEach { variant ->
            val entry = ModelManifest.entryOf(variant)
            assertTrue(
                "expectedSizeBytes должен быть положительным для $variant",
                entry.expectedSizeBytes > 0L,
            )
            assertEquals(
                "SHA-256 для $variant должен быть длиной 64 hex-символа",
                64,
                entry.expectedSha256.length,
            )
            assertTrue(
                "SHA-256 для $variant должен быть в нижнем регистре hex",
                entry.expectedSha256.all { it in HEX_LOWER },
            )
            assertTrue(
                "Источник для $variant должен указывать на litert-community",
                entry.primarySourceUrl.startsWith("https://huggingface.co/litert-community/"),
            )
            assertTrue(
                "Источник для $variant должен ссылаться на .litertlm файл",
                entry.primarySourceUrl.contains(".litertlm"),
            )
            assertTrue(
                "displayName для $variant не должен быть пустым",
                entry.displayName.isNotBlank(),
            )
            assertTrue(
                "minRamGb для $variant должен быть положительным",
                entry.minRamGb > 0,
            )
            assertTrue(
                "latency range для $variant должен быть валидным",
                entry.latencyMinSec > 0 && entry.latencyMaxSec >= entry.latencyMinSec,
            )
        }
    }

    @Test
    fun `exactly one variant is marked recommended`() {
        val recommended = ModelVariant.entries.count { ModelManifest.entryOf(it).recommended }
        assertEquals(
            "Ровно один вариант должен быть рекомендованным; иначе UI не знает, на каком ставить акцент",
            1,
            recommended,
        )
    }

    @Test
    fun `e2b is the recommended variant`() {
        assertTrue(
            "E2B как более лёгкий по памяти — наш default-рекомендация",
            ModelManifest.entryOf(ModelVariant.E2B).recommended,
        )
        assertFalse(
            "E4B мощнее, но требует больше ОЗУ — не default",
            ModelManifest.entryOf(ModelVariant.E4B).recommended,
        )
    }

    @Test
    fun `sizes match research document numbers`() {
        // Эти числа взяты из HuggingFace API `lfs.size` на 2026-05-19 — см.
        // plans/phase-c-gemma-research.md, раздел 2. Несовпадение → нужно
        // ревизировать манифест.
        assertEquals(2_588_147_712L, ModelManifest.entryOf(ModelVariant.E2B).expectedSizeBytes)
        assertEquals(3_659_530_240L, ModelManifest.entryOf(ModelVariant.E4B).expectedSizeBytes)
    }

    private companion object {
        const val HEX_LOWER = "0123456789abcdef"
    }
}
