// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pocketreflect.app.core.security.AuthSessionHolder
import com.pocketreflect.app.core.security.DatabasePassphraseManager
import com.pocketreflect.app.data.local.entity.JournalEntry
import com.pocketreflect.app.testing.FakeClock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RoomDatabaseProviderTest {

    private lateinit var context: Context
    private lateinit var provider: RoomDatabaseProvider

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(com.pocketreflect.app.data.local.AppDatabase.DATABASE_NAME)
        provider = RoomDatabaseProvider(
            context = context,
            passphraseManager = DatabasePassphraseManager(context),
        )
    }

    @Test
    fun unlock_afterLock_allowsQueriesAgain() = runTest {
        val auth = AuthSessionHolder(FakeClock(), provider)
        auth.markAuthenticated()

        provider.get().journalDao().upsert(
            JournalEntry(
                timestamp = 1L,
                dayBucket = "2026-05-25",
                moodTags = emptyList(),
                microWins = "",
                tomorrowTasks = "",
                reflection = "ok",
                promptShown = "p",
                aiReflection = null,
            ),
        )

        auth.onAppBackgrounded()
        auth.markAuthenticated()

        val entry = provider.get().journalDao().findByDay("2026-05-25")
        assertNotNull(entry)
        assertEquals("ok", entry?.reflection)
    }
}
