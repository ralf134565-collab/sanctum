// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.locale

import java.util.Locale

/**
 * Язык интерфейса и доменных текстов (промпты, mock-ИИ).
 * [SYSTEM] следует локали устройства; [RU]/[EN] — явный выбор в настройках.
 */
enum class AppLanguage(val storageKey: String?) {
    SYSTEM(null),
    RU("ru"),
    EN("en"),
    ;

    companion object {
        val DEFAULT: AppLanguage = SYSTEM

        fun fromStorageKey(raw: String?): AppLanguage =
            when (raw) {
                RU.storageKey -> RU
                EN.storageKey -> EN
                else -> SYSTEM
            }

        /**
         * Для промптов и mock-ответов нужен только [RU] или [EN].
         * [SYSTEM] → `en*` в [Locale.getDefault], иначе [RU].
         */
        fun resolve(
            preference: AppLanguage,
            deviceLocale: Locale = Locale.getDefault(),
        ): AppLanguage =
            when (preference) {
                RU, EN -> preference
                SYSTEM -> {
                    val lang = deviceLocale.language.lowercase(Locale.ROOT)
                    if (lang.startsWith("en")) EN else RU
                }
            }
    }

    val isEnglish: Boolean get() = this == EN
}
