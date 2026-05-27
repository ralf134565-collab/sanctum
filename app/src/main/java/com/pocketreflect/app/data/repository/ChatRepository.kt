// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.repository

import com.pocketreflect.app.domain.chat.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeMessages(): Flow<List<ChatMessage>>
    fun observeMessages(personaId: String): Flow<List<ChatMessage>>
    suspend fun insert(message: ChatMessage): Long
    suspend fun clearAll()
    suspend fun clearPersonaChat(personaId: String)
    suspend fun totalContentLength(): Int
    suspend fun totalContentLength(personaId: String): Int
    suspend fun messageCount(): Int
}
