// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.export.vault

import com.pocketreflect.app.core.locale.AppLanguage
import com.pocketreflect.app.data.local.entity.JournalEntry
import com.pocketreflect.app.domain.model.MoodTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MarkdownVaultRoundTripTest {

    @Test
    fun renderAndParse_journalEntry_preservesCoreFields() {
        val entry = JournalEntry(
            timestamp = 1_715_000_000_000L,
            dayBucket = "2026-05-26",
            moodTags = listOf(MoodTag.CALM, MoodTag.FOCUSED),
            microWins = "Погулял без телефона",
            tomorrowTasks = "Позвонить маме\nНаписать отчёт",
            reflection = "День прошёл ровно.",
            promptShown = "Что дало опору?",
            aiReflection = "Вы замедлились — это уже шаг.",
            customFieldQuestion = "Главный урок",
            customFieldAnswer = "Принятие неопределённости",
        )

        val markdown = MarkdownEntryRenderer.render(entry, AppLanguage.DEFAULT)
        val parsed = MarkdownFrontMatterParser.parseJournalEntry(markdown)

        assertNotNull(parsed)
        assertEquals(entry.dayBucket, parsed!!.dayBucket)
        assertEquals(entry.timestamp, parsed.timestamp)
        assertEquals(entry.moodTags.toSet(), parsed.moodTags.toSet())
        assertEquals(entry.microWins, parsed.microWins)
        assertEquals(entry.tomorrowTasks, parsed.tomorrowTasks)
        assertEquals(entry.reflection, parsed.reflection)
        assertEquals(entry.promptShown, parsed.promptShown)
        assertEquals(entry.aiReflection, parsed.aiReflection)
        assertEquals(entry.customFieldQuestion, parsed.customFieldQuestion)
        assertEquals(entry.customFieldAnswer, parsed.customFieldAnswer)
    }

    @Test
    fun zipWriter_containsJournalFile() {
        val entry = JournalEntry(
            timestamp = 1_715_000_000_000L,
            dayBucket = "2026-05-26",
            moodTags = listOf(MoodTag.CALM),
            microWins = "",
            tomorrowTasks = "",
            reflection = "Test",
            promptShown = "Prompt",
            aiReflection = null,
        )
        val md = MarkdownEntryRenderer.render(entry, AppLanguage.EN)
        val files = listOf(
            VaultArchiveFile("sanctum-export-2026-05-26/journal/2026-05-26.md", md.toByteArray()),
        )
        val bytes = java.io.ByteArrayOutputStream().use { out ->
            VaultZipWriter.write(files, out)
            out.toByteArray()
        }
        val read = VaultZipReader.readAll(java.io.ByteArrayInputStream(bytes))
        assertEquals(1, read.size)
        assertEquals("sanctum-export-2026-05-26/journal/2026-05-26.md", read.first().path)
    }
}
