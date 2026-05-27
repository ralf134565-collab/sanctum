// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.chat

import com.pocketreflect.app.data.local.entity.JournalEntry
import com.pocketreflect.app.core.locale.AppLanguage
import com.pocketreflect.app.domain.model.MoodTag

/**
 * Сжатый контекст дневника для чата — без простынь, только наблюдаемое.
 */
object ChatJournalSnippetBuilder {

    fun build(entries: List<JournalEntry>, language: AppLanguage = AppLanguage.RU): String {
        if (entries.isEmpty()) {
            return if (language.isEnglish) {
                "No journal entries for the selected period."
            } else {
                "За выбранный период записей в дневнике нет."
            }
        }
        val lines = entries
            .sortedByDescending { it.timestamp }
            .map { formatEntry(it, language) }
        val body = lines.joinToString(separator = "\n")
        return body.take(ChatContextPolicy.MAX_JOURNAL_SNIPPET_CHARS)
    }

    private fun formatEntry(entry: JournalEntry, language: AppLanguage): String {
        val tags = entry.moodTags.joinToString { tagLabel(it, language) }
        val parts = buildList {
            if (language.isEnglish) {
                add("Day ${entry.dayBucket}: tags — $tags")
                entry.microWins.trim().takeIf { it.isNotEmpty() }?.let {
                    add("  micro-wins: ${it.lineSequence().first()}")
                }
                entry.reflection.trim().takeIf { it.isNotEmpty() }?.let {
                    add("  reflection: ${it.take(120)}")
                }
                entry.tomorrowTasks.trim().takeIf { it.isNotEmpty() }?.let {
                    add("  tomorrow focus: ${it.lineSequence().first()}")
                }
                if (entry.customFieldQuestion.isNotBlank() && entry.customFieldAnswer.isNotBlank()) {
                    add(
                        "  ${entry.customFieldQuestion.trim()}: " +
                            entry.customFieldAnswer.trim().lineSequence().first().take(120),
                    )
                }
            } else {
                add("День ${entry.dayBucket}: теги — $tags")
                entry.microWins.trim().takeIf { it.isNotEmpty() }?.let {
                    add("  микро-победы: ${it.lineSequence().first()}")
                }
                entry.reflection.trim().takeIf { it.isNotEmpty() }?.let {
                    add("  рефлексия: ${it.take(120)}")
                }
                entry.tomorrowTasks.trim().takeIf { it.isNotEmpty() }?.let {
                    add("  фокус на завтра: ${it.lineSequence().first()}")
                }
                if (entry.customFieldQuestion.isNotBlank() && entry.customFieldAnswer.isNotBlank()) {
                    add(
                        "  ${entry.customFieldQuestion.trim()}: " +
                            entry.customFieldAnswer.trim().lineSequence().first().take(120),
                    )
                }
            }
        }
        return parts.joinToString(separator = "\n")
    }

    private fun tagLabel(tag: MoodTag, language: AppLanguage): String = tag.displayName(language)
}
