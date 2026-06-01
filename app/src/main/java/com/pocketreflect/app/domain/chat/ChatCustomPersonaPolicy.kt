// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.chat

import com.pocketreflect.app.core.locale.AppLanguage

object ChatCustomPersonaPolicy {

    const val DISPLAY_NAME_MAX = 40
    const val PROMPT_MAX = 800

    val BUILT_IN_PERSONAS: List<ChatPersona> =
        ChatPersona.entries.filter { it != ChatPersona.CUSTOM }

    fun isConfigured(enabled: Boolean, prompt: String): Boolean =
        enabled && prompt.trim().isNotBlank()

    fun isSelectable(enabled: Boolean, prompt: String, persona: ChatPersona): Boolean =
        persona != ChatPersona.CUSTOM || isConfigured(enabled, prompt)

    fun resolveActivePersona(
        stored: ChatPersona,
        enabled: Boolean,
        prompt: String,
    ): ChatPersona = when {
        stored == ChatPersona.CUSTOM && !isConfigured(enabled, prompt) -> ChatPersona.DEFAULT
        else -> stored
    }

    fun chipDisplayName(
        customName: String?,
        language: AppLanguage,
    ): String {
        val trimmed = customName?.trim().orEmpty()
        if (trimmed.isNotEmpty()) return trimmed.take(DISPLAY_NAME_MAX)
        return ChatPersona.CUSTOM.displayName(language)
    }

    fun normalizeDisplayName(raw: String): String = raw.trim().take(DISPLAY_NAME_MAX)

    fun normalizePrompt(raw: String): String = raw.trim().take(PROMPT_MAX)
}
