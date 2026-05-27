// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.ritual

import com.pocketreflect.app.data.local.entity.JournalEntry
import com.pocketreflect.app.domain.model.hasNegative
import com.pocketreflect.app.domain.model.hasPositive
import java.time.LocalDate

object ShortEveningPolicy {
    const val CONSECUTIVE_NEGATIVE_THRESHOLD = 3

    fun compute(
        lastEntries: List<JournalEntry>,
        today: LocalDate,
    ): RitualMode {
        if (lastEntries.size < CONSECUTIVE_NEGATIVE_THRESHOLD) return RitualMode.FULL

        // Сортируем записи по дате по убыванию и берем CONSECUTIVE_NEGATIVE_THRESHOLD штук
        val recentByDay = lastEntries
            .sortedByDescending { it.dayBucket }
            .take(CONSECUTIVE_NEGATIVE_THRESHOLD)

        if (recentByDay.size < CONSECUTIVE_NEGATIVE_THRESHOLD) return RitualMode.FULL

        // Проверяем непрерывность дат: вчера, позавчера, позапозавчера
        val consecutiveDates = (1..CONSECUTIVE_NEGATIVE_THRESHOLD)
            .map { today.minusDays(it.toLong()).toString() }

        val entryDates = recentByDay.map { it.dayBucket }

        val allRecentAreConsecutive = entryDates.containsAll(consecutiveDates)
        if (!allRecentAreConsecutive) return RitualMode.FULL

        // Проверяем, что во всех этих записях есть хотя бы один негативный тег и нет позитивных
        val allNegative = recentByDay.all { entry ->
            val tagSet = entry.moodTags.toSet()
            tagSet.hasNegative && !tagSet.hasPositive
        }

        return if (allNegative) RitualMode.SHORT else RitualMode.FULL
    }
}
