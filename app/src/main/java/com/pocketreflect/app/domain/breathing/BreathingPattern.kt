// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.breathing

/**
 * Режим дыхательного моста на экране «Сегодня».
 *
 * [RESONANT] — 5 с вдох / 5 с выдох (6 bpm), тактильное ведение.
 * [BOX] — классический квадрат 4-4-4-4.
 */
enum class BreathingPattern(val storageKey: String) {
    RESONANT("resonant"),
    BOX("box"),
    ;

    companion object {
        val DEFAULT: BreathingPattern = RESONANT

        fun fromStorageKey(raw: String?): BreathingPattern =
            entries.firstOrNull { it.storageKey == raw } ?: DEFAULT
    }
}
