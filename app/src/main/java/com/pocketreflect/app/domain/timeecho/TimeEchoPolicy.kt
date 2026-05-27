// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.timeecho

import com.pocketreflect.app.data.local.entity.JournalEntry
import com.pocketreflect.app.data.repository.JournalRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object TimeEchoPolicy {
    const val ECHO_WINDOW_DAYS = 1 // ±1 день вокруг 365-дневной отметки
    const val ECHO_TARGET_DAYS_AGO = 365L
    const val DISMISS_COOLDOWN_HOURS = 20L // не показывать снова в этот же день, если закрыли

    data class Echo(
        val entry: JournalEntry,
        val daysAgo: Long,
    )

    suspend fun findEcho(
        today: LocalDate,
        repository: JournalRepository,
        lastDismissedAtMs: Long?,
        currentTimeMs: Long,
    ): Echo? {
        if (lastDismissedAtMs != null) {
            val hoursSinceDismiss = (currentTimeMs - lastDismissedAtMs) / 3_600_000L
            if (hoursSinceDismiss < DISMISS_COOLDOWN_HOURS) return null
        }

        val targetBucket = today.minusDays(ECHO_TARGET_DAYS_AGO)
        val fromBucket = targetBucket.minusDays(ECHO_WINDOW_DAYS.toLong())
        val toBucket = targetBucket.plusDays(ECHO_WINDOW_DAYS.toLong())

        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val entry = repository.findEntryInDayRange(
            fromBucket.format(formatter),
            toBucket.format(formatter)
        ) ?: return null

        val entryLocalDate = LocalDate.parse(entry.dayBucket, formatter)
        val actualDaysAgo = today.toEpochDay() - entryLocalDate.toEpochDay()

        return Echo(entry, actualDaysAgo)
    }
}
