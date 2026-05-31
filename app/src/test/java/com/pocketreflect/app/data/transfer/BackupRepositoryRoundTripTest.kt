// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.transfer

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pocketreflect.app.core.time.Clock
import com.pocketreflect.app.data.local.entity.JournalEntry
import com.pocketreflect.app.core.security.AuthSessionHolder
import com.pocketreflect.app.core.security.DefaultDatabaseAccess
import com.pocketreflect.app.data.repository.RoomJournalRepository
import com.pocketreflect.app.data.transfer.DefaultBackupRepository
import com.pocketreflect.app.domain.model.MoodTag
import com.pocketreflect.app.testing.FakeClock
import com.pocketreflect.app.testing.FakeUserPreferencesRepository
import com.pocketreflect.app.testing.RobolectricRoomTestSupport
import com.pocketreflect.app.testing.StaticDatabaseProvider
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Сквозной путь: Room → export → decrypt → mergeImport → Room.
 * Сообщения чата в `.sanctum` не входят (продуктовое решение).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class BackupRepositoryRoundTripTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val fastIterations = 1_000

    private lateinit var room: RobolectricRoomTestSupport.InMemoryAppDatabase
    private lateinit var journalRepository: RoomJournalRepository
    private lateinit var backupRepository: DefaultBackupRepository

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        room = RobolectricRoomTestSupport.inMemoryDatabase(context)
        val database = room.database
        val databaseProvider = StaticDatabaseProvider(database)
        val authSessionHolder = AuthSessionHolder(FakeClock(), databaseProvider).apply {
            markAuthenticated()
        }
        journalRepository = RoomJournalRepository(
            databaseProvider = databaseProvider,
            databaseAccess = DefaultDatabaseAccess(databaseProvider, authSessionHolder),
        )
        val clock = object : Clock {
            override fun nowMillis(): Long = 9_999L
            override fun today(): String = "2026-05-19"
        }
        backupRepository = DefaultBackupRepository(
            journalRepository = journalRepository,
            clock = clock,
            encoder = ExportFileEncoder(json),
            decoder = ImportFileDecoder(json),
        )
    }

    @After
    fun tearDown() {
        room.close()
    }

    @Test
    fun exportThenImport_restoresEntriesOnFreshDatabase() = runTest {
        journalRepository.saveEntry(
            JournalEntry(
                timestamp = 1_715_000_000_000L,
                dayBucket = "2026-05-19",
                moodTags = listOf(MoodTag.CALM, MoodTag.FOCUSED),
                microWins = "win",
                tomorrowTasks = "task",
                reflection = "reflect",
                promptShown = "prompt",
                aiReflection = "mentor",
            ),
        )

        val password = "backup-pass".toCharArray()
        val bytes = ByteArrayOutputStream().use { out ->
            backupRepository.export(out, password)
            out.toByteArray()
        }

        journalRepository.wipeAll()
        assertEquals(0, journalRepository.findAllEntries().size)

        val report = ByteArrayInputStream(bytes).use { input ->
            backupRepository.import(input, password, overwrite = false)
        }

        assertEquals(1, report.insertedEntries)
        assertEquals(0, report.skippedEntries)
        val restored = journalRepository.findAllEntries().single()
        assertEquals("2026-05-19", restored.dayBucket)
        assertEquals("reflect", restored.reflection)
        assertEquals("mentor", restored.aiReflection)
        assertEquals(listOf(MoodTag.CALM, MoodTag.FOCUSED), restored.moodTags)
    }
}
