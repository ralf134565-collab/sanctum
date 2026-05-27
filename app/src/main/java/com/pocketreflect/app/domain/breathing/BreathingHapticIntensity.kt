// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.breathing

/**
 * Сила тактильного ведения в резонансном режиме.
 *
 * [GENTLE] — дискретные мягкие импульсы (рекомендуется).
 * [MODERATE] — чуть заметнее, но без непрерывного «жужжания».
 */
enum class BreathingHapticIntensity(val storageKey: String) {
    GENTLE("gentle"),
    MODERATE("moderate"),
    ;

    companion object {
        val DEFAULT: BreathingHapticIntensity = GENTLE

        fun fromStorageKey(raw: String?): BreathingHapticIntensity =
            entries.firstOrNull { it.storageKey == raw } ?: DEFAULT
    }
}
