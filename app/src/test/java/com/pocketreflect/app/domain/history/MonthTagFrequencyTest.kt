// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.history

import com.pocketreflect.app.core.locale.AppLanguage
import com.pocketreflect.app.data.local.entity.JournalEntry
import com.pocketreflect.app.domain.model.MoodTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthTagFrequencyTest {

    @Test
    fun summarize_returns_top_three_by_count() {
        val entries = listOf(
            entry(tags = listOf(MoodTag.CALM, MoodTag.TIRED)),
            entry(tags = listOf(MoodTag.CALM)),
            entry(tags = listOf(MoodTag.CALM, MoodTag.JOY)),
            entry(tags = listOf(MoodTag.TIRED)),
            entry(tags = listOf(MoodTag.ANXIETY)),
        )

        val result = MonthTagFrequency.summarize(entries, limit = 3)

        assertEquals(3, result.size)
        assertEquals(MoodTag.CALM, result[0].tag)
        assertEquals(3, result[0].count)
        assertEquals(MoodTag.TIRED, result[1].tag)
        assertEquals(2, result[1].count)
        assertEquals(MoodTag.JOY, result[2].tag)
        assertEquals(1, result[2].count)
    }

    @Test
    fun summarize_uses_ui_order_for_ties() {
        val entries = listOf(
            entry(tags = listOf(MoodTag.JOY)),
            entry(tags = listOf(MoodTag.CALM)),
        )

        val result = MonthTagFrequency.summarize(entries, limit = 2)

        assertEquals(MoodTag.CALM, result[0].tag)
        assertEquals(MoodTag.JOY, result[1].tag)
    }

    @Test
    fun summarize_empty_entries_returns_empty_list() {
        assertTrue(MonthTagFrequency.summarize(emptyList()).isEmpty())
    }

    @Test
    fun formatLine_returns_null_for_empty_tags() {
        assertNull(MonthTagFrequency.formatLine(emptyList(), AppLanguage.RU))
    }

    @Test
    fun formatLine_builds_russian_line() {
        val line = MonthTagFrequency.formatLine(
            tags = listOf(
                TagFrequency(MoodTag.CALM, 12),
                TagFrequency(MoodTag.TIRED, 8),
            ),
            language = AppLanguage.RU,
        )

        assertEquals("Спокойствие (12) • Усталость (8)", line)
    }

    private fun entry(tags: List<MoodTag>): JournalEntry = JournalEntry(
        id = 0L,
        timestamp = 0L,
        dayBucket = "2026-05-01",
        moodTags = tags,
        microWins = "",
        tomorrowTasks = "",
        reflection = "",
        promptShown = "",
        aiReflection = null,
    )
}
