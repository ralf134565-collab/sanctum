// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.repository

/**
 * Режим оформления приложения. Без «как в системе» — отдельно от будущего выбора языка.
 */
enum class AppThemeMode(val storageKey: String) {
    DARK("dark"),
    LIGHT("light"),
    ;

    companion object {
        val DEFAULT: AppThemeMode = DARK

        fun fromStorageKey(raw: String?): AppThemeMode =
            entries.firstOrNull { it.storageKey == raw } ?: DEFAULT
    }
}
