// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.history

import com.pocketreflect.app.core.locale.AppLanguage
import com.pocketreflect.app.data.local.entity.JournalEntry

/**
 * Локальный поиск по записям дневника — подстрока по ключевым словам,
 * без FTS и морфологии. Достаточно для личного архива на устройстве.
 */
object JournalSearchMatcher {

    const val MIN_QUERY_LENGTH = 2

    fun isActiveQuery(query: String): Boolean =
        normalize(query).length >= MIN_QUERY_LENGTH

    fun matches(entry: JournalEntry, query: String, language: AppLanguage): Boolean {
        val tokens = tokens(query)
        if (tokens.isEmpty()) return true

        val haystacks = searchableTexts(entry, language).map(::normalize)
        return tokens.any { token ->
            haystacks.any { haystack -> haystack.contains(token) }
        }
    }

    /**
     * Первая строка из пользовательских полей, где нашлось совпадение —
     * для превью в результатах поиска.
     */
    fun matchPreview(entry: JournalEntry, query: String, language: AppLanguage): String? {
        val tokens = tokens(query)
        if (tokens.isEmpty()) return null

        return searchableTexts(entry, language)
            .asSequence()
            .flatMap { text -> text.lineSequence() }
            .map { it.trim() }
            .firstOrNull { line ->
                line.isNotBlank() && tokens.any { token ->
                    normalize(line).contains(token)
                }
            }
    }

    fun normalize(query: String): String =
        query.trim().replace(Regex("\\s+"), " ").lowercase()

    private fun tokens(query: String): List<String> {
        if (!isActiveQuery(query)) return emptyList()
        return normalize(query).split(' ').filter { it.isNotEmpty() }
    }

    private fun searchableTexts(entry: JournalEntry, language: AppLanguage): List<String> =
        buildList {
            add(entry.reflection)
            add(entry.microWins)
            add(entry.tomorrowTasks)
            add(entry.customFieldAnswer)
            add(entry.customFieldQuestion)
            add(entry.promptShown)
            entry.aiReflection?.let { add(it) }
            entry.moodTags.forEach { tag -> add(tag.displayName(language)) }
        }.filter { it.isNotBlank() }
}
