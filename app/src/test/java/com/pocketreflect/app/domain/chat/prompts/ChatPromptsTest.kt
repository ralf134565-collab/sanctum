// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.chat.prompts

import com.pocketreflect.app.core.locale.AppLanguage
import com.pocketreflect.app.domain.chat.ChatCustomPersonaPolicy
import com.pocketreflect.app.domain.chat.ChatMessage
import com.pocketreflect.app.domain.chat.ChatPersona
import com.pocketreflect.app.domain.chat.ChatRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatPromptsTest {

    @Test
    fun systemInstructions_coverBuiltInPersonas() {
        ChatCustomPersonaPolicy.BUILT_IN_PERSONAS.forEach { persona ->
            assertTrue(ChatPrompts.SYSTEM_INSTRUCTIONS.containsKey(persona))
            assertTrue(ChatPrompts.SYSTEM_INSTRUCTIONS[persona]!!.isNotBlank())
        }
    }

    @Test
    fun customSystemInstruction_includesSafetyKernelAndUserStyle() {
        val instruction = ChatPrompts.customSystemInstruction(
            userStylePrompt = "Тон: мягкий.",
            language = AppLanguage.RU,
        )
        assertTrue(instruction.contains("БАЗОВЫЕ ПРАВИЛА SANCTUM"))
        assertTrue(instruction.contains("--- Ваш стиль ---"))
        assertTrue(instruction.contains("Тон: мягкий."))
    }

    @Test
    fun buildChatUserPrompt_includesHistoryAndJournal() {
        val prompt = ChatPrompts.buildChatUserPrompt(
            history = listOf(
                ChatMessage(role = ChatRole.ASSISTANT, content = "Раньше", timestamp = 1L),
                ChatMessage(role = ChatRole.USER, content = "Привет", timestamp = 2L),
            ),
            journalSnippet = "День 2026-05-20: теги — Тревога",
            persona = ChatPersona.DEFAULT,
        )
        assertTrue(prompt.contains("Новое сообщение пользователя:"))
        assertTrue(prompt.contains("Привет"))
        assertTrue(prompt.contains("Собеседник: Раньше"))
        assertTrue(prompt.contains("дневника"))
    }
}
