// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.timeecho

import com.pocketreflect.app.data.local.entity.JournalEntry
import com.pocketreflect.app.testing.FakeJournalRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class TimeEchoPolicyTest {

    @Test
    fun testTimeEchoPolicy() = runBlocking {
        val repository = FakeJournalRepository()
        val today = LocalDate.of(2026, 5, 24)

        // 1. Запись ровно 365 дней назад (24 мая 2025)
        val entry365 = JournalEntry(
            id = 1L,
            timestamp = 1748044800000L, // 2025-05-24
            dayBucket = "2025-05-24",
            moodTags = emptyList(),
            microWins = "",
            tomorrowTasks = "",
            reflection = "Год назад я написал это.",
            promptShown = "",
            aiReflection = null
        )
        repository.saveEntry(entry365)

        val echo = TimeEchoPolicy.findEcho(
            today = today,
            repository = repository,
            lastDismissedAtMs = null,
            currentTimeMs = System.currentTimeMillis()
        )

        assertNotNull(echo)
        assertEquals(1L, echo!!.entry.id)
        assertEquals(365L, echo.daysAgo)
    }

    @Test
    fun testTimeEchoPolicyInWindow() = runBlocking {
        val repository = FakeJournalRepository()
        val today = LocalDate.of(2026, 5, 24)

        // 2. Запись 366 дней назад (23 мая 2025) — входит в окно +-1 день
        val entry366 = JournalEntry(
            id = 2L,
            timestamp = 1747958400000L,
            dayBucket = "2025-05-23",
            moodTags = emptyList(),
            microWins = "",
            tomorrowTasks = "",
            reflection = "Почти год назад.",
            promptShown = "",
            aiReflection = null
        )
        repository.saveEntry(entry366)

        val echo = TimeEchoPolicy.findEcho(
            today = today,
            repository = repository,
            lastDismissedAtMs = null,
            currentTimeMs = System.currentTimeMillis()
        )

        assertNotNull(echo)
        assertEquals(2L, echo!!.entry.id)
        assertEquals(366L, echo.daysAgo)
    }

    @Test
    fun testTimeEchoPolicyOutOfWindow() = runBlocking {
        val repository = FakeJournalRepository()
        val today = LocalDate.of(2026, 5, 24)

        // 3. Запись 363 дня назад (27 мая 2025) — вне окна +-1 день
        val entry363 = JournalEntry(
            id = 3L,
            timestamp = 1748304000000L,
            dayBucket = "2025-05-27",
            moodTags = emptyList(),
            microWins = "",
            tomorrowTasks = "",
            reflection = "Рано.",
            promptShown = "",
            aiReflection = null
        )
        repository.saveEntry(entry363)

        val echo = TimeEchoPolicy.findEcho(
            today = today,
            repository = repository,
            lastDismissedAtMs = null,
            currentTimeMs = System.currentTimeMillis()
        )

        assertNull(echo)
    }

    @Test
    fun testTimeEchoPolicyCooldown() = runBlocking {
        val repository = FakeJournalRepository()
        val today = LocalDate.of(2026, 5, 24)

        val entry = JournalEntry(
            id = 1L,
            timestamp = 1748044800000L,
            dayBucket = "2025-05-24",
            moodTags = emptyList(),
            microWins = "",
            tomorrowTasks = "",
            reflection = "Год назад.",
            promptShown = "",
            aiReflection = null
        )
        repository.saveEntry(entry)

        val now = System.currentTimeMillis()

        // Скрыли 5 часов назад (< 20 часов) -> null
        val echoBlocked = TimeEchoPolicy.findEcho(
            today = today,
            repository = repository,
            lastDismissedAtMs = now - 5 * 3600 * 1000L,
            currentTimeMs = now
        )
        assertNull(echoBlocked)

        // Скрыли 21 час назад (>= 20 часов) -> найдена
        val echoAllowed = TimeEchoPolicy.findEcho(
            today = today,
            repository = repository,
            lastDismissedAtMs = now - 21 * 3600 * 1000L,
            currentTimeMs = now
        )
        assertNotNull(echoAllowed)
        assertEquals(1L, echoAllowed!!.entry.id)
    }
}
