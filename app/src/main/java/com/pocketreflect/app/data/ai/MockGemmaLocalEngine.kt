// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.ai

import com.pocketreflect.app.core.locale.AppLanguageResolver
import com.pocketreflect.app.data.ai.mock.MockGemmaTextPools
import com.pocketreflect.app.data.local.entity.JournalEntry
import com.pocketreflect.app.domain.ai.GemmaLocalEngine
import com.pocketreflect.app.domain.chat.ChatMessage
import com.pocketreflect.app.domain.chat.ChatPersona
import com.pocketreflect.app.domain.chat.ChatRole
import com.pocketreflect.app.domain.model.MoodTag
import com.pocketreflect.app.domain.model.hasNegative
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class MockGemmaLocalEngine @Inject constructor(
    private val languageResolver: AppLanguageResolver,
) : GemmaLocalEngine {

    override suspend fun generatePromptResponse(
        entry: JournalEntry,
        personalManifesto: String?,
    ): String {
        delay(450)
        val pools = MockGemmaTextPools.forLanguage(languageResolver.resolvedNow())
        val tags = entry.moodTags.toSet()

        return when {
            MoodTag.ANXIETY in tags -> randomOf(pools.anxiety)
            MoodTag.SADNESS in tags -> randomOf(pools.sadness)
            MoodTag.IRRITATION in tags -> randomOf(pools.irritation)
            MoodTag.OVERWHELMED in tags || MoodTag.TIRED in tags -> randomOf(pools.tired)
            tags.hasNegative -> randomOf(pools.generalSupport)
            MoodTag.JOY in tags -> randomOf(pools.joy)
            MoodTag.GRATITUDE in tags -> randomOf(pools.gratitude)
            MoodTag.CALM in tags -> randomOf(pools.calm)
            MoodTag.FOCUSED in tags -> randomOf(pools.focused)
            else -> randomOf(pools.neutral)
        }
    }

    override suspend fun summarizeWeek(
        entries: List<JournalEntry>,
        personalManifesto: String?,
    ): GemmaLocalEngine.WeeklySummary {
        delay(800)
        val language = languageResolver.resolvedNow()
        val grouped = entries
            .flatMap { it.moodTags }
            .groupingBy { it.displayName(language) }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .joinToString { "${it.key} ×${it.value}" }

        val summary = if (entries.isEmpty()) {
            if (language.isEnglish) {
                "No entries this week. That's okay — come back when you feel like it."
            } else {
                "На этой неделе записей не было. Это нормально — возвращайтесь, когда захочется."
            }
        } else if (language.isEnglish) {
            "Main moods this week: $grouped. " +
                "You're still noticing how you feel — that's already a skill."
        } else {
            "Преобладающие состояния недели: $grouped. " +
                "Замечаю, что вы продолжаете замечать своё состояние — это уже навык."
        }
        return GemmaLocalEngine.WeeklySummary(humanReadable = summary, structuredJson = null)
    }

    override suspend fun isReady(): Boolean = true

    override fun streamChat(
        history: List<ChatMessage>,
        persona: ChatPersona,
        journalSnippet: String?,
        manifestoSnippet: String?,
        customPersonaPrompt: String?,
    ): Flow<String> = flow {
        delay(300)
        val lastUser = history.lastOrNull { it.role == ChatRole.USER }?.content.orEmpty()
        val base = pickChatResponse(persona, lastUser, journalSnippet)
        val words = base.split(Regex("(?<=\\s)"))
        for (word in words) {
            emit(word)
            delay(35)
        }
    }

    private suspend fun pickChatResponse(
        persona: ChatPersona,
        lastUser: String,
        journalSnippet: String?,
    ): String {
        val pools = MockGemmaTextPools.forLanguage(languageResolver.resolvedNow())
        val pool = when (persona) {
            ChatPersona.CUSTOM -> pools.gentleMentorChat
            ChatPersona.GENTLE_MENTOR -> pools.gentleMentorChat
            ChatPersona.EXPERIENCED_FRIEND -> pools.friendChat
            ChatPersona.SUPPORTIVE_COACH -> pools.coachChat
            ChatPersona.FREE_DIALOG -> pools.freeDialogChat
        }
        val journalHint = if (!journalSnippet.isNullOrBlank()) pools.journalHint else ""
        val echo = if (lastUser.length > 20) pools.echoLong else ""
        return randomOf(pool) + echo + journalHint
    }

    override suspend fun warmUp() = Unit

    override suspend fun release() = Unit

    override suspend fun summarizeChat(history: List<ChatMessage>): String {
        delay(600)
        val language = languageResolver.resolvedNow()
        return if (language.isEnglish) {
            "The user and the assistant discussed various feelings, reflected on their day, and planned next steps to cultivate mindfulness."
        } else {
            "Пользователь и ассистент обсудили текущие переживания, подвели итоги дня и наметили шаги для обретения душевного спокойствия."
        }
    }

    private fun randomOf(list: List<String>): String = list[Random.nextInt(list.size)]
}
