// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.export.vault

import com.pocketreflect.app.data.local.entity.JournalEntry
import com.pocketreflect.app.domain.model.MoodTag

/**
 * Парсер YAML Front Matter Sanctum-записей (schema v1).
 */
internal object MarkdownFrontMatterParser {

    data class ParsedNote(
        val schemaVersion: Int,
        val fields: Map<String, String>,
    )

    fun parseJournalEntry(markdown: String): JournalEntry? {
        val parsed = parse(markdown) ?: return null
        if (parsed.schemaVersion != ObsidianExportSchema.SCHEMA_VERSION) return null

        val dayBucket = parsed.fields[ObsidianExportSchema.FIELD_DATE]?.trim().orEmpty()
        if (dayBucket.isBlank()) return null

        val timestamp = parsed.fields[ObsidianExportSchema.FIELD_TIMESTAMP]?.toLongOrNull()
            ?: return null

        val moodTags = parseInlineList(parsed.fields[ObsidianExportSchema.FIELD_MOOD_TAGS])
            .mapNotNull(MoodTag::fromStorageKeyOrNull)
            .distinct()

        return JournalEntry(
            id = 0L,
            timestamp = timestamp,
            dayBucket = dayBucket,
            moodTags = moodTags,
            microWins = parsed.fields[ObsidianExportSchema.FIELD_MICRO_WINS].orEmpty(),
            tomorrowTasks = parsed.fields[ObsidianExportSchema.FIELD_TOMORROW].orEmpty(),
            reflection = parsed.fields[ObsidianExportSchema.FIELD_REFLECTION].orEmpty(),
            promptShown = parsed.fields[ObsidianExportSchema.FIELD_PROMPT].orEmpty(),
            aiReflection = parsed.fields[ObsidianExportSchema.FIELD_AI_REFLECTION]
                ?.takeIf { it.isNotBlank() },
            customFieldAnswer = parsed.fields[ObsidianExportSchema.FIELD_CUSTOM_A].orEmpty(),
            customFieldQuestion = parsed.fields[ObsidianExportSchema.FIELD_CUSTOM_Q].orEmpty(),
        )
    }

    fun parse(markdown: String): ParsedNote? {
        val normalized = markdown.replace("\r\n", "\n")
        if (!normalized.startsWith("---\n")) return null
        val end = normalized.indexOf("\n---", 4)
        if (end < 0) return null
        val yaml = normalized.substring(4, end)
        val fields = parseYamlBlock(yaml)
        val schema = fields[ObsidianExportSchema.FIELD_SCHEMA]?.trim()?.toIntOrNull()
            ?: return null
        return ParsedNote(schemaVersion = schema, fields = fields)
    }

    private fun parseYamlBlock(yaml: String): Map<String, String> {
        val result = linkedMapOf<String, String>()
        val lines = yaml.lines()
        var index = 0
        while (index < lines.size) {
            val line = lines[index]
            val colon = line.indexOf(':')
            if (colon <= 0) {
                index++
                continue
            }
            val key = line.substring(0, colon).trim()
            val rawValue = line.substring(colon + 1).trim()
            if (rawValue == "|" || rawValue == ">") {
                val block = readBlockScalar(lines, index + 1)
                result[key] = block.text
                index = block.nextIndex
            } else if (rawValue.startsWith("[") && rawValue.endsWith("]")) {
                result[key] = rawValue
                index++
            } else {
                result[key] = unquote(rawValue)
                index++
            }
        }
        return result
    }

    private data class BlockResult(val text: String, val nextIndex: Int)

    private fun readBlockScalar(lines: List<String>, start: Int): BlockResult {
        val builder = StringBuilder()
        var index = start
        while (index < lines.size) {
            val line = lines[index]
            if (line.isNotBlank() && !line.startsWith("  ") && line.contains(':')) break
            if (line.startsWith("  ")) {
                if (builder.isNotEmpty()) builder.append('\n')
                builder.append(line.removePrefix("  "))
            } else if (line.isBlank()) {
                if (builder.isNotEmpty()) builder.append('\n')
            } else {
                break
            }
            index++
        }
        return BlockResult(builder.toString(), index)
    }

    private fun parseInlineList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        val trimmed = raw.trim()
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return emptyList()
        val inner = trimmed.substring(1, trimmed.length - 1).trim()
        if (inner.isBlank()) return emptyList()
        return inner.split(',').map { unquote(it.trim()) }.filter { it.isNotBlank() }
    }

    private fun unquote(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith('"') && trimmed.endsWith('"') && trimmed.length >= 2) {
            return trimmed.substring(1, trimmed.length - 1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
        }
        return trimmed
    }
}
