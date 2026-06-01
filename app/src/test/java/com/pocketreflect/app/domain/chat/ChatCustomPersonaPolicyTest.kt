// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.chat

import com.pocketreflect.app.core.locale.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatCustomPersonaPolicyTest {

    @Test
    fun isConfigured_requiresEnabledAndNonBlankPrompt() {
        assertFalse(ChatCustomPersonaPolicy.isConfigured(false, "стиль"))
        assertFalse(ChatCustomPersonaPolicy.isConfigured(true, "   "))
        assertTrue(ChatCustomPersonaPolicy.isConfigured(true, "стиль"))
    }

    @Test
    fun resolveActivePersona_fallsBackWhenCustomNotConfigured() {
        assertEquals(
            ChatPersona.DEFAULT,
            ChatCustomPersonaPolicy.resolveActivePersona(
                stored = ChatPersona.CUSTOM,
                enabled = false,
                prompt = "стиль",
            ),
        )
    }

    @Test
    fun chipDisplayName_usesCustomNameOrDefault() {
        assertEquals(
            "Мой наставник",
            ChatCustomPersonaPolicy.chipDisplayName("Мой наставник", AppLanguage.RU),
        )
        assertEquals(
            "Свой стиль",
            ChatCustomPersonaPolicy.chipDisplayName(null, AppLanguage.RU),
        )
        assertEquals(
            "Custom style",
            ChatCustomPersonaPolicy.chipDisplayName("", AppLanguage.EN),
        )
    }
}
