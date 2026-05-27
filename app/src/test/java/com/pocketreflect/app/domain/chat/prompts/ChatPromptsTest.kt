// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.chat.prompts

import com.pocketreflect.app.domain.chat.ChatMessage
import com.pocketreflect.app.domain.chat.ChatPersona
import com.pocketreflect.app.domain.chat.ChatRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatPromptsTest {

    @Test
    fun systemInstructions_coverAllPersonas() {
        ChatPersona.entries.forEach { persona ->
            assertTrue(ChatPrompts.SYSTEM_INSTRUCTIONS.containsKey(persona))
            assertTrue(ChatPrompts.SYSTEM_INSTRUCTIONS[persona]!!.isNotBlank())
        }
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
