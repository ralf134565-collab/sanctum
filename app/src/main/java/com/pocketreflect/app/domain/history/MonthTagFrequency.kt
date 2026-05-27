// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.history

import com.pocketreflect.app.core.locale.AppLanguage
import com.pocketreflect.app.data.local.entity.JournalEntry
import com.pocketreflect.app.domain.model.MoodTag

data class TagFrequency(
    val tag: MoodTag,
    val count: Int,
)

/**
 * Агрегация частотности аффективных тегов за набор записей (обычно — один месяц).
 */
object MonthTagFrequency {

    const val DEFAULT_TOP_LIMIT = 3

    fun summarize(
        entries: List<JournalEntry>,
        limit: Int = DEFAULT_TOP_LIMIT,
    ): List<TagFrequency> {
        if (entries.isEmpty() || limit <= 0) return emptyList()

        val order = MoodTag.orderedForUi.withIndex().associate { it.value to it.index }

        return entries
            .asSequence()
            .flatMap { it.moodTags.asSequence() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<MoodTag, Int>> { it.value }
                    .thenBy { order[it.key] ?: Int.MAX_VALUE },
            )
            .take(limit)
            .map { (tag, count) -> TagFrequency(tag = tag, count = count) }
            .toList()
    }

    fun formatLine(
        tags: List<TagFrequency>,
        language: AppLanguage,
        separator: String = " • ",
    ): String? {
        if (tags.isEmpty()) return null
        return tags.joinToString(separator = separator) { frequency ->
            "${frequency.tag.displayName(language)} (${frequency.count})"
        }
    }
}
