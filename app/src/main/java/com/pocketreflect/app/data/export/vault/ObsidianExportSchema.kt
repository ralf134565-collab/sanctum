// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.export.vault

/**
 * Схема Markdown/ZIP-экспорта для Obsidian, Logseq и других локальных PKM.
 *
 * Структура архива:
 * ```
 * sanctum-export-YYYY-MM-DD/
 *   _meta/manifest.yaml
 *   journal/YYYY-MM-DD.md
 *   weekly/YYYY-Www.md          (опционально)
 *   manifesto.md                (опционально)
 * ```
 *
 * Front Matter каждой записи содержит `sanctum_schema: 1` — импорт принимает
 * только файлы с известной версией схемы.
 */
object ObsidianExportSchema {
    const val SCHEMA_VERSION: Int = 1

    const val ROOT_PREFIX = "sanctum-export"
    const val META_MANIFEST = "_meta/manifest.yaml"
    const val JOURNAL_DIR = "journal/"
    const val WEEKLY_DIR = "weekly/"
    const val MANIFESTO_FILE = "manifesto.md"

    const val FIELD_SCHEMA = "sanctum_schema"
    const val FIELD_DATE = "date"
    const val FIELD_TIMESTAMP = "timestamp"
    const val FIELD_MOOD_TAGS = "mood_tags"
    const val FIELD_MOOD_LABELS = "mood_labels"
    const val FIELD_TOMORROW_LINES = "tomorrow_focus_lines"
    const val FIELD_PROMPT = "prompt"
    const val FIELD_CUSTOM_Q = "custom_field_q"
    const val FIELD_CUSTOM_A = "custom_field_a"
    const val FIELD_MICRO_WINS = "micro_wins"
    const val FIELD_TOMORROW = "tomorrow_tasks"
    const val FIELD_REFLECTION = "reflection"
    const val FIELD_AI_REFLECTION = "ai_reflection"
    const val FIELD_TAGS = "tags"

    /** Версия экспортёра (совпадает с релизом приложения). */
    const val EXPORTER_VERSION: String = "1.0"
}
