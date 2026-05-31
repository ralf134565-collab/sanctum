// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.repository

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pocketreflect.app.core.security.AuthSessionHolder
import com.pocketreflect.app.core.security.DefaultDatabaseAccess
import com.pocketreflect.app.data.local.entity.AITrendProfile
import com.pocketreflect.app.data.local.entity.JournalEntry
import com.pocketreflect.app.domain.model.MoodTag
import com.pocketreflect.app.testing.FakeClock
import com.pocketreflect.app.testing.FakeUserPreferencesRepository
import com.pocketreflect.app.testing.RobolectricRoomTestSupport
import com.pocketreflect.app.testing.StaticDatabaseProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Интеграция [JournalRepository.mergeImport] на in-memory Room.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class RoomJournalRepositoryMergeImportTest {

    private lateinit var room: RobolectricRoomTestSupport.InMemoryAppDatabase
    private lateinit var repository: RoomJournalRepository

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        room = RobolectricRoomTestSupport.inMemoryDatabase(context)
        val database = room.database
        val databaseProvider = StaticDatabaseProvider(database)
        val authSessionHolder = AuthSessionHolder(FakeClock(), databaseProvider).apply {
            markAuthenticated()
        }
        repository = RoomJournalRepository(
            databaseProvider = databaseProvider,
            databaseAccess = DefaultDatabaseAccess(databaseProvider, authSessionHolder),
        )
    }

    @After
    fun tearDown() {
        room.close()
    }

    @Test
    fun mergeImport_insertsNewEntriesAndProfiles() = runTest {
        val incomingEntry = sampleEntry(dayBucket = "2026-05-10")
        val incomingProfile = sampleProfile(periodStart = 10L, periodEnd = 20L, generatedAt = 30L)

        val report = repository.mergeImport(
            entries = listOf(incomingEntry),
            profiles = listOf(incomingProfile),
            overwrite = false,
        )

        assertEquals(1, report.insertedEntries)
        assertEquals(0, report.skippedEntries)
        assertEquals(1, report.insertedProfiles)
        assertEquals(0, report.skippedProfiles)
        assertEquals(1, repository.findAllEntries().size)
        assertEquals(1, repository.findAllProfiles().size)
    }

    @Test
    fun mergeImport_skipsDuplicateDayBucketWhenNotOverwrite() = runTest {
        repository.saveEntry(sampleEntry(dayBucket = "2026-05-11", reflection = "original"))

        val report = repository.mergeImport(
            entries = listOf(sampleEntry(dayBucket = "2026-05-11", reflection = "incoming")),
            profiles = emptyList(),
            overwrite = false,
        )

        assertEquals(0, report.insertedEntries)
        assertEquals(1, report.skippedEntries)
        assertEquals("original", repository.findAllEntries().single().reflection)
    }

    @Test
    fun mergeImport_overwritesExistingDayBucket() = runTest {
        repository.saveEntry(sampleEntry(dayBucket = "2026-05-12", reflection = "before"))

        val report = repository.mergeImport(
            entries = listOf(sampleEntry(dayBucket = "2026-05-12", reflection = "after")),
            profiles = emptyList(),
            overwrite = true,
        )

        assertEquals(1, report.insertedEntries)
        assertEquals(0, report.skippedEntries)
        assertEquals("after", repository.findAllEntries().single().reflection)
    }

    private fun sampleEntry(
        dayBucket: String,
        reflection: String = "text",
    ) = JournalEntry(
        timestamp = 1_715_000_000_000L,
        dayBucket = dayBucket,
        moodTags = listOf(MoodTag.CALM),
        microWins = "",
        tomorrowTasks = "",
        reflection = reflection,
        promptShown = "prompt",
        aiReflection = null,
    )

    private fun sampleProfile(
        periodStart: Long,
        periodEnd: Long,
        generatedAt: Long,
    ) = AITrendProfile(
        periodStart = periodStart,
        periodEnd = periodEnd,
        generatedAt = generatedAt,
        entryCount = 3,
        summary = "week",
        structuredJson = null,
        schemaVersion = 1,
    )
}
