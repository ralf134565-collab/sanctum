// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.model

/**
 * Захардкоженная справочная таблица моделей Gemma 4 (E2B / E4B).
 *
 * Источник значений — HuggingFace API репозиториев `litert-community/...` на 2026-05-19:
 *   - размер файла берётся из `lfs.size`;
 *   - ожидаемый SHA-256 — из `lfs.oid` (для git-LFS OID совпадает с SHA-256
 *     содержимого файла).
 *
 * Эти значения **не** должны вычисляться или скачиваться в рантайме — они
 * сознательно живут в коде, чтобы:
 *  - SHA-256 проверка стала криптографической гарантией (мы не доверяем
 *    тому, что прислал HF, пока не сверим с захардкоженным хешем);
 *  - обновление модели до новой версии всегда было осознанным
 *    code-change'ем, проходящим ревью, а не silent runtime-фетчем.
 *
 * Подробное обоснование — `plans/phase-c-gemma-research.md`, разделы 2 и 4.
 */
object ModelManifest {

    /**
     * Параметры конкретного варианта модели.
     *
     * @property displayName человеко-читаемое имя, показываемое в UI.
     * @property expectedSizeBytes ровно столько байт мы ожидаем при копировании.
     *  Несовпадение — повод отвергнуть файл, не дожидаясь конца SHA-256.
     * @property expectedSha256 hex-нижний-регистр SHA-256 контента файла.
     * @property primarySourceUrl прямая ссылка на скачивание; открывается
     *  во **внешнем браузере** через `Intent.ACTION_VIEW`. Приложение само
     *  ничего не качает — `android.permission.INTERNET` НЕ присутствует
     *  в манифесте.
     * @property ramRequirementHuman краткая подсказка по требованию к ОЗУ
     *  устройства (показывается на карточке варианта).
     * @property latencyHintHuman ожидаемая латентность одного ответа после
     *  прогрева — для UX-ожидания.
     * @property recommended единственный вариант, помеченный `true`, — это
     *  default-предложение в UI. Сейчас рекомендуем E2B как более лёгкий.
     */
    data class Entry(
        val displayName: String,
        val expectedSizeBytes: Long,
        val expectedSha256: String,
        val primarySourceUrl: String,
        val minRamGb: Int,
        val latencyMinSec: Int,
        val latencyMaxSec: Int,
        val recommended: Boolean,
    )

    private val entries: Map<ModelVariant, Entry> = mapOf(
        ModelVariant.E2B to Entry(
            displayName = "Gemma 4 E2B",
            expectedSizeBytes = 2_588_147_712L,
            expectedSha256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c",
            primarySourceUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm?download=true",
            minRamGb = 6,
            latencyMinSec = 2,
            latencyMaxSec = 4,
            recommended = true,
        ),
        ModelVariant.E4B to Entry(
            displayName = "Gemma 4 E4B",
            expectedSizeBytes = 3_659_530_240L,
            expectedSha256 = "0b2a8980ce155fd97673d8e820b4d29d9c7d99b8fa6806f425d969b145bd52e0",
            primarySourceUrl = "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm?download=true",
            minRamGb = 8,
            latencyMinSec = 4,
            latencyMaxSec = 7,
            recommended = false,
        ),
    )

    fun entryOf(variant: ModelVariant): Entry = entries.getValue(variant)

    fun allVariants(): List<ModelVariant> = entries.keys.toList()
}
