// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.chat

/**
 * Сообщение чата — доменная модель для UI и [com.pocketreflect.app.domain.ai.GemmaLocalEngine].
 */
data class ChatMessage(
    val id: Long = 0L,
    val role: ChatRole,
    val content: String,
    val timestamp: Long,
    /** Персона ASSISTANT на момент ответа; null для USER. */
    val personaId: String? = null,
)
