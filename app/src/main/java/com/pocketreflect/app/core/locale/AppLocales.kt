// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Per-app locale через AppCompat.
 *
 * Важно: [AppCompatDelegate.setApplicationLocales] пересоздаёт Activity.
 * Нельзя вызывать из [LaunchedEffect] в [com.pocketreflect.app.MainActivity] —
 * получится бесконечный restart и «моргание» всего UI.
 */
object AppLocales {

    @Volatile
    private var lastApplied: AppLanguage? = null

    /** @return `true`, если локаль реально изменилась и был вызван AppCompat. */
    fun apply(preference: AppLanguage): Boolean {
        if (lastApplied == preference) return false
        lastApplied = preference
        AppCompatDelegate.setApplicationLocales(localesFor(preference))
        return true
    }

    private fun localesFor(preference: AppLanguage): LocaleListCompat = when (preference) {
        AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
        AppLanguage.RU -> LocaleListCompat.forLanguageTags("ru")
        AppLanguage.EN -> LocaleListCompat.forLanguageTags("en")
    }
}
