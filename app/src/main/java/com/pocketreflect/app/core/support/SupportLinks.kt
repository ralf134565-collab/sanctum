// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.core.support

/**
 * URL-адреса для блока поддержки проекта (исходный код и страницы пожертвований).
 */
object SupportLinks {
    const val GITHUB: String = "https://github.com/ralf134565-collab/sanctum"

    fun getSupportLink(isRussian: Boolean): String {
        return if (isRussian) {
            "https://github.com/ralf134565-collab/sanctum/blob/main/DONATE.ru.md"
        } else {
            "https://github.com/ralf134565-collab/sanctum/blob/main/DONATE.md"
        }
    }
}

