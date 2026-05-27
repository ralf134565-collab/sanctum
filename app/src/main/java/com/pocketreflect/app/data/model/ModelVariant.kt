// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.model

/**
 * Поддерживаемые варианты локальной модели Gemma 4 (LiteRT-LM).
 *
 * Линейка `*n`-моделей с MatFormer-архитектурой, `E*B` обозначает
 * **effective billions of parameters** — реальный размер вложенной
 * подсети, активной во время инференса. Подробности см. в
 * `plans/phase-c-gemma-research.md`, раздел 2.
 *
 * Имена файлов фиксированы в `litert-community` репозиториях HuggingFace
 * и используются и как ключ для манифеста, и как фактическое имя
 * сохранённой копии в `filesDir/models/`.
 */
enum class ModelVariant(val storageFileName: String) {
    E2B(storageFileName = "gemma-4-E2B-it.litertlm"),
    E4B(storageFileName = "gemma-4-E4B-it.litertlm"),
}
