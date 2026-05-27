// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.repository

import android.util.Log
import com.pocketreflect.app.data.local.entity.ChatMessageEntity
import com.pocketreflect.app.domain.chat.ChatMessage
import com.pocketreflect.app.domain.chat.ChatRole

private const val TAG = "ChatMessageMapper"

internal fun ChatMessageEntity.toDomain(): ChatMessage? {
    val role = ChatRole.fromStorageKey(this.role)
    if (role == null) {
        Log.w(TAG, "Unknown chat role '${this.role}' for message id=$id — skipping")
        return null
    }
    return ChatMessage(
        id = id,
        role = role,
        content = content,
        timestamp = timestamp,
        personaId = personaId,
    )
}

internal fun ChatMessage.toEntity(): ChatMessageEntity = ChatMessageEntity(
    id = id,
    role = role.storageKey,
    content = content,
    timestamp = timestamp,
    personaId = personaId,
)
