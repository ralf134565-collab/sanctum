// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.ritual

import com.pocketreflect.app.data.local.entity.JournalEntry
import com.pocketreflect.app.domain.model.MoodTag
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ShortEveningPolicyTest {

    private val today = LocalDate.of(2026, 5, 24)

    @Test
    fun testThreeNegativeDaysInRow() {
        val lastEntries = listOf(
            createEntry("2026-05-23", listOf(MoodTag.SADNESS)),
            createEntry("2026-05-22", listOf(MoodTag.ANXIETY)),
            createEntry("2026-05-21", listOf(MoodTag.IRRITATION))
        )
        val mode = ShortEveningPolicy.compute(lastEntries, today)
        assertEquals(RitualMode.SHORT, mode)
    }

    @Test
    fun testThreeNegativeDaysWithGap() {
        val lastEntries = listOf(
            createEntry("2026-05-23", listOf(MoodTag.SADNESS)),
            // Пропуск 22 мая
            createEntry("2026-05-21", listOf(MoodTag.ANXIETY)),
            createEntry("2026-05-20", listOf(MoodTag.IRRITATION))
        )
        val mode = ShortEveningPolicy.compute(lastEntries, today)
        assertEquals(RitualMode.FULL, mode)
    }

    @Test
    fun testThreeNegativeDaysButOneIsMixed() {
        val lastEntries = listOf(
            createEntry("2026-05-23", listOf(MoodTag.SADNESS)),
            createEntry("2026-05-22", listOf(MoodTag.ANXIETY, MoodTag.JOY)), // Смешанный день (есть позитивный тег)
            createEntry("2026-05-21", listOf(MoodTag.IRRITATION))
        )
        val mode = ShortEveningPolicy.compute(lastEntries, today)
        assertEquals(RitualMode.FULL, mode)
    }

    @Test
    fun testFourNegativeDays() {
        val lastEntries = listOf(
            createEntry("2026-05-23", listOf(MoodTag.SADNESS)),
            createEntry("2026-05-22", listOf(MoodTag.ANXIETY)),
            createEntry("2026-05-21", listOf(MoodTag.IRRITATION)),
            createEntry("2026-05-20", listOf(MoodTag.SADNESS))
        )
        val mode = ShortEveningPolicy.compute(lastEntries, today)
        assertEquals(RitualMode.SHORT, mode)
    }

    @Test
    fun testTwoNegativeOneNeutral() {
        val lastEntries = listOf(
            createEntry("2026-05-23", listOf(MoodTag.SADNESS)),
            createEntry("2026-05-22", listOf(MoodTag.TIRED)), // Нейтральный
            createEntry("2026-05-21", listOf(MoodTag.IRRITATION))
        )
        val mode = ShortEveningPolicy.compute(lastEntries, today)
        assertEquals(RitualMode.FULL, mode)
    }

    @Test
    fun testEmptyList() {
        val mode = ShortEveningPolicy.compute(emptyList(), today)
        assertEquals(RitualMode.FULL, mode)
    }

    private fun createEntry(dayBucket: String, tags: List<MoodTag>): JournalEntry {
        return JournalEntry(
            id = 0L,
            timestamp = 0L,
            dayBucket = dayBucket,
            moodTags = tags,
            microWins = "",
            tomorrowTasks = "",
            reflection = "",
            promptShown = "",
            aiReflection = null
        )
    }
}
