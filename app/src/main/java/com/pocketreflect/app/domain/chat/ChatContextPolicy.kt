// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.chat

/**
 * Эвристика лимита контекста чата (символы, не токены).
 * После smoke на E2B можно подстроить [MAX_CHAT_CONTEXT_CHARS].
 */
object ChatContextPolicy {

    const val MAX_CHAT_CONTEXT_CHARS: Int = 12_000

    /** Резерв под ответ модели + служебный промпт. */
    const val RESPONSE_RESERVE_CHARS: Int = 2_048

    /** Макс. длина сниппета дневника. */
    const val MAX_JOURNAL_SNIPPET_CHARS: Int = 800

    data class ContextUsage(
        val usedChars: Int,
        val maxChars: Int,
        val percent: Int,
        val isFull: Boolean,
    ) {
        companion object {
            fun empty(maxChars: Int = MAX_CHAT_CONTEXT_CHARS): ContextUsage =
                ContextUsage(usedChars = 0, maxChars = maxChars, percent = 0, isFull = false)
        }
    }

    fun computeUsage(
        messages: List<ChatMessage>,
        journalSnippet: String?,
        manifestoSnippet: String? = null,
    ): ContextUsage {
        val messageChars = messages.sumOf { it.content.length }
        val journalChars = journalSnippet?.length ?: 0
        val manifestoChars = manifestoSnippet?.length ?: 0
        val used = messageChars + journalChars + manifestoChars
        val percent = ((used.toDouble() / MAX_CHAT_CONTEXT_CHARS) * 100)
            .toInt()
            .coerceIn(0, 100)
        return ContextUsage(
            usedChars = used,
            maxChars = MAX_CHAT_CONTEXT_CHARS,
            percent = percent,
            isFull = used >= MAX_CHAT_CONTEXT_CHARS,
        )
    }

    /**
     * Берёт хвост истории, который влезает в бюджет инференса
     * (без учёта journal — его добавляют отдельно в промпт).
     */
    fun trimHistoryForInference(
        messages: List<ChatMessage>,
        journalSnippetLength: Int,
        manifestoSnippetLength: Int = 0,
    ): List<ChatMessage> {
        val budget = MAX_CHAT_CONTEXT_CHARS - RESPONSE_RESERVE_CHARS -
            journalSnippetLength - manifestoSnippetLength
        if (budget <= 0) return emptyList()
        val result = ArrayList<ChatMessage>()
        var used = 0
        for (message in messages.asReversed()) {
            val len = message.content.length
            if (used + len > budget && result.isNotEmpty()) break
            if (len > budget && result.isEmpty()) {
                result.add(
                    message.copy(
                        content = message.content.takeLast(budget),
                    ),
                )
                break
            }
            result.add(message)
            used += len
            if (used >= budget) break
        }
        return result.asReversed()
    }
}
