// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.repository

import com.pocketreflect.app.data.local.entity.JournalEntry
import com.pocketreflect.app.domain.chat.ChatPersona
import com.pocketreflect.app.testing.FakeDailyPromptsHistoryRepository
import com.pocketreflect.app.testing.FakeJournalRepository
import com.pocketreflect.app.testing.FakeUserPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserDataRepositoryTest {

    private val journal = FakeJournalRepository()
    private val prefs = FakeUserPreferencesRepository()
    private val promptsHistory = FakeDailyPromptsHistoryRepository(listOf("prompt-a", "prompt-b"))

    private val repository = DefaultUserDataRepository(
        journalRepository = journal,
        userPreferencesRepository = prefs,
        promptsHistory = promptsHistory,
    )

    @Test
    fun wipeAllUserContent_clearsJournalPrefsAndPromptHistory() = runTest {
        journal.seedEntries(
            listOf(
                JournalEntry(
                    timestamp = 1L,
                    dayBucket = "2026-05-20",
                    moodTags = emptyList(),
                    microWins = "",
                    tomorrowTasks = "",
                    reflection = "test",
                    promptShown = "q",
                    aiReflection = null,
                ),
            ),
        )
        prefs.setBiometricLockEnabled(true)
        prefs.setAutoLockTimeout(AutoLockTimeout.FIVE_MINUTES)
        prefs.setChatDisclaimerAccepted(true)
        prefs.setChatPersona(ChatPersona.SUPPORTIVE_COACH)

        repository.wipeAllUserContent()

        assertEquals(1, journal.wipeInvocations)
        assertTrue(journal.entriesSnapshot().isEmpty())
        assertEquals(1, prefs.clearChatInvocations)
        assertFalse(prefs.chatDisclaimerAccepted.first())
        assertEquals(ChatPersona.DEFAULT, prefs.chatPersona.first())
        assertEquals(1, promptsHistory.clearInvocations)
        assertTrue(promptsHistory.snapshot.isEmpty())
        assertTrue(prefs.biometricLockEnabled.first())
        assertEquals(AutoLockTimeout.FIVE_MINUTES, prefs.autoLockTimeout.first())
    }

    @Test
    fun wipeAllUserContent_whenJournalFails_doesNotClearPrefs() = runTest {
        journal.shouldThrowOnWipe = IllegalStateException("db")

        var threw = false
        try {
            repository.wipeAllUserContent()
        } catch (_: IllegalStateException) {
            threw = true
        }

        assertTrue(threw)
        assertEquals(0, prefs.clearChatInvocations)
        assertEquals(0, promptsHistory.clearInvocations)
    }
}
