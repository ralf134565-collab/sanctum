// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.chat

import com.pocketreflect.app.core.locale.AppLanguage

/**
 * Стиль собеседника в вкладке «Чат».
 *
 * Переработан под новые психологически выверенные архетипы рефлексии:
 * - GENTLE_MENTOR -> "Бережный проводник" (Gentle guide) - эмпатия, заземление
 * - EXPERIENCED_FRIEND -> "Честное зеркало" (Honest mirror) - Сократовский диалог, рефлексия слепых зон
 * - SUPPORTIVE_COACH -> "Реалист-прагматик" (Realist-pragmatic) - фокус на фактах и действиях
 * - FREE_DIALOG -> "Тихий слушатель" (Quiet listener) - ультра-короткие ответы для слива мыслей
 */
enum class ChatPersona(
    val storageKey: String,
    val displayName: String,
) {
    GENTLE_MENTOR("gentle_mentor", "Бережный проводник"),
    EXPERIENCED_FRIEND("experienced_friend", "Честное зеркало"),
    SUPPORTIVE_COACH("supportive_coach", "Реалист-прагматик"),
    FREE_DIALOG("free_dialog", "Тихий слушатель"),
    ;

    fun displayName(language: AppLanguage): String =
        if (language.isEnglish) ENGLISH_NAMES[this] ?: displayName else displayName

    companion object {
        val DEFAULT: ChatPersona = GENTLE_MENTOR

        fun fromStorageKey(raw: String?): ChatPersona =
            entries.firstOrNull { it.storageKey == raw } ?: DEFAULT

        private val ENGLISH_NAMES: Map<ChatPersona, String> = mapOf(
            GENTLE_MENTOR to "Gentle guide",
            EXPERIENCED_FRIEND to "Honest mirror",
            SUPPORTIVE_COACH to "Realist-pragmatic",
            FREE_DIALOG to "Quiet listener",
        )
    }
}
