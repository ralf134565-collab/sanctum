// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.chat

/**
 * Роль сообщения в локальном чате.
 * SYSTEM не храним в Room — только runtime в промптах.
 */
enum class ChatRole {
    USER,
    ASSISTANT,
    ;

    val storageKey: String = name.lowercase()

    companion object {
        fun fromStorageKey(raw: String?): ChatRole? =
            entries.firstOrNull { it.storageKey == raw?.lowercase() }
    }
}
