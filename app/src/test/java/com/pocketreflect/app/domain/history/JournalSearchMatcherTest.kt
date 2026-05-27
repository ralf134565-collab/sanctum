// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.history

import com.pocketreflect.app.core.locale.AppLanguage
import com.pocketreflect.app.data.local.entity.JournalEntry
import com.pocketreflect.app.domain.model.MoodTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JournalSearchMatcherTest {

    @Test
    fun `short query is not active`() {
        assertFalse(JournalSearchMatcher.isActiveQuery("a"))
        assertFalse(JournalSearchMatcher.isActiveQuery("  "))
    }

    @Test
    fun `matches reflection case insensitively`() {
        val entry = sampleEntry(reflection = "Сегодня был важный созвон")
        assertTrue(JournalSearchMatcher.matches(entry, "созвон", AppLanguage.RU))
    }

    @Test
    fun `matches micro wins and tasks`() {
        val wins = sampleEntry(microWins = "Закрыл три задачи")
        val tasks = sampleEntry(tomorrowTasks = "1) Подготовить презентацию")
        assertTrue(JournalSearchMatcher.matches(wins, "задачи", AppLanguage.RU))
        assertTrue(JournalSearchMatcher.matches(tasks, "презентацию", AppLanguage.RU))
    }

    @Test
    fun `matches mood tag display name`() {
        val entry = sampleEntry(moodTags = listOf(MoodTag.ANXIETY))
        assertTrue(JournalSearchMatcher.matches(entry, "тревога", AppLanguage.RU))
    }

    @Test
    fun `any token matches for multi word query`() {
        val entry = sampleEntry(reflection = "Спокойный вечер дома")
        assertTrue(JournalSearchMatcher.matches(entry, "работа дом", AppLanguage.RU))
        assertFalse(JournalSearchMatcher.matches(entry, "работа офис", AppLanguage.RU))
    }

    @Test
    fun `match preview returns matching line`() {
        val entry = sampleEntry(
            reflection = "",
            tomorrowTasks = "1) Купить хлеб\n2) Позвонить маме",
        )
        assertEquals(
            "2) Позвонить маме",
            JournalSearchMatcher.matchPreview(entry, "маме", AppLanguage.RU),
        )
    }

    @Test
    fun `match preview is null when no hit`() {
        val entry = sampleEntry(reflection = "Тихий вечер")
        assertNull(JournalSearchMatcher.matchPreview(entry, "офис", AppLanguage.RU))
    }

    private fun sampleEntry(
        reflection: String = "",
        microWins: String = "",
        tomorrowTasks: String = "",
        moodTags: List<MoodTag> = emptyList(),
    ): JournalEntry = JournalEntry(
        id = 1L,
        timestamp = 0L,
        dayBucket = "2026-05-24",
        moodTags = moodTags,
        microWins = microWins,
        tomorrowTasks = tomorrowTasks,
        reflection = reflection,
        promptShown = "Prompt",
        aiReflection = null,
    )
}
