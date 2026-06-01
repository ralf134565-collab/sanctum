// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.testing

import com.pocketreflect.app.data.local.entity.JournalEntry
import com.pocketreflect.app.domain.ai.GemmaLocalEngine
import com.pocketreflect.app.domain.chat.ChatMessage
import com.pocketreflect.app.domain.chat.ChatPersona
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Детерминированная фейк-реализация локального ИИ. Не делает delay,
 * чтобы тесты не зависели от времени.
 */
class FakeGemmaLocalEngine(
    var responseProvider: (JournalEntry) -> String = { "OK" },
    var summarizeWeekProvider: (List<JournalEntry>) -> GemmaLocalEngine.WeeklySummary = {
        GemmaLocalEngine.WeeklySummary(humanReadable = "fake-week", structuredJson = null)
    },
) : GemmaLocalEngine {
    var lastEntry: JournalEntry? = null
        private set
    var generateInvocations: Int = 0
        private set
    var summarizeWeekInvocations: Int = 0
        private set

    override suspend fun generatePromptResponse(
        entry: JournalEntry,
        personalManifesto: String?,
    ): String {
        generateInvocations++
        lastEntry = entry
        return responseProvider(entry)
    }

    override suspend fun summarizeWeek(
        entries: List<JournalEntry>,
        personalManifesto: String?,
    ): GemmaLocalEngine.WeeklySummary {
        summarizeWeekInvocations++
        return summarizeWeekProvider(entries)
    }

    var chatResponse: String = "fake chat"

    override fun streamChat(
        history: List<ChatMessage>,
        persona: ChatPersona,
        journalSnippet: String?,
        manifestoSnippet: String?,
        customPersonaPrompt: String?,
    ): Flow<String> = flowOf(chatResponse)

    override suspend fun isReady(): Boolean = true
    override suspend fun warmUp() = Unit
    override suspend fun release() = Unit
    override suspend fun summarizeChat(history: List<ChatMessage>): String = "fake summary"
}
