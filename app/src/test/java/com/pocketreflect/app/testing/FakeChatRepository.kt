// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.testing

import com.pocketreflect.app.data.repository.ChatRepository
import com.pocketreflect.app.domain.chat.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeChatRepository : ChatRepository {
    private val messages = MutableStateFlow<List<ChatMessage>>(emptyList())

    override fun observeMessages(): Flow<List<ChatMessage>> = messages.asStateFlow()

    override fun observeMessages(personaId: String): Flow<List<ChatMessage>> {
        return observeMessages()
    }

    override suspend fun insert(message: ChatMessage): Long {
        val id = (messages.value.maxOfOrNull { it.id } ?: 0L) + 1L
        messages.value = messages.value + message.copy(id = id)
        return id
    }

    override suspend fun clearAll() {
        messages.value = emptyList()
    }

    override suspend fun clearPersonaChat(personaId: String) {
        messages.value = messages.value.filter { it.personaId != personaId }
    }

    override suspend fun totalContentLength(): Int =
        messages.value.sumOf { it.content.length }

    override suspend fun totalContentLength(personaId: String): Int =
        messages.value.filter { it.personaId == personaId }.sumOf { it.content.length }

    override suspend fun messageCount(): Int = messages.value.size
}
