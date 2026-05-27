// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.testing

import com.pocketreflect.app.data.local.entity.AITrendProfile
import com.pocketreflect.app.data.local.entity.JournalEntry
import com.pocketreflect.app.data.repository.ImportMergeReport
import com.pocketreflect.app.data.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory реализация [JournalRepository] для plain JUnit-тестов.
 *
 * Хранит данные в `MutableStateFlow<List<JournalEntry>>` — это позволяет
 * проверять реактивные подписки (observeHistory, observeById) без Room.
 *
 * Не потокобезопасен и не претендует — для unit-тестов этого достаточно
 * (тесты гоняются single-threaded через TestDispatcher).
 */
class FakeJournalRepository : JournalRepository {

    private val entriesFlow = MutableStateFlow<List<JournalEntry>>(emptyList())
    private val trendsFlow = MutableStateFlow<List<AITrendProfile>>(emptyList())

    /** Что подставлять в `findToday()` (по умолчанию — null = «сегодня ещё не записан»). */
    var todayOverride: JournalEntry? = null

    /** Срабатывает при первом saveToday — для assert'ов в тестах. */
    var lastSaved: JournalEntry? = null
        private set
    var saveInvocations: Int = 0
        private set

    var shouldThrowOnSave: Throwable? = null
    var shouldThrowOnWipe: Throwable? = null
    var wipeInvocations: Int = 0
        private set

    fun seedEntries(entries: List<JournalEntry>) {
        entriesFlow.value = entries
    }

    fun entriesSnapshot(): List<JournalEntry> = entriesFlow.value

    override fun observeHistory(): Flow<List<JournalEntry>> = entriesFlow

    override fun observeToday(): Flow<JournalEntry?> =
        entriesFlow.map { it.firstOrNull { e -> e.dayBucket == todayOverride?.dayBucket } }

    override suspend fun findToday(): JournalEntry? = todayOverride

    override suspend fun findByDay(dayBucket: String): JournalEntry? =
        entriesFlow.value.firstOrNull { it.dayBucket == dayBucket }

    override suspend fun saveEntry(entry: JournalEntry): Long {
        saveInvocations++
        shouldThrowOnSave?.let { throw it }
        lastSaved = entry
        val list = entriesFlow.value.toMutableList()
        val existingIdx = list.indexOfFirst { it.dayBucket == entry.dayBucket }
        val withId = if (existingIdx >= 0) {
            val previous = list[existingIdx]
            entry.copy(id = previous.id).also { list[existingIdx] = it }
        } else {
            entry.copy(id = (list.maxOfOrNull { it.id } ?: 0L) + 1).also { list.add(it) }
        }
        entriesFlow.value = list
        todayOverride = withId
        return withId.id
    }

    override suspend fun delete(entry: JournalEntry) {
        entriesFlow.value = entriesFlow.value.filterNot { it.id == entry.id }
        if (todayOverride?.id == entry.id) todayOverride = null
    }

    override suspend fun findById(id: Long): JournalEntry? =
        entriesFlow.value.firstOrNull { it.id == id }

    override fun observeById(id: Long): Flow<JournalEntry?> =
        entriesFlow.map { it.firstOrNull { e -> e.id == id } }

    override suspend fun entriesForLastDays(days: Int): List<JournalEntry> =
        entriesFlow.value

    override suspend fun findEntryInDayRange(fromDayBucket: String, toDayBucket: String): JournalEntry? =
        entriesFlow.value
            .filter { it.dayBucket >= fromDayBucket && it.dayBucket <= toDayBucket }
            .maxByOrNull { it.dayBucket }

    override suspend fun findLastNEntries(count: Int): List<JournalEntry> =
        entriesFlow.value
            .sortedByDescending { it.dayBucket }
            .take(count)

    override fun observeTrendProfiles(): Flow<List<AITrendProfile>> = trendsFlow

    override suspend fun latestTrendProfile(): AITrendProfile? =
        trendsFlow.value
            .asSequence()
            .filter { it.summary.isNotBlank() }
            .maxByOrNull { it.generatedAt }

    override suspend fun saveTrendProfile(profile: AITrendProfile): Long {
        val withId = profile.copy(id = (trendsFlow.value.maxOfOrNull { it.id } ?: 0L) + 1)
        trendsFlow.value = trendsFlow.value + withId
        return withId.id
    }

    override suspend fun wipeAll() {
        shouldThrowOnWipe?.let { throw it }
        wipeInvocations++
        entriesFlow.value = emptyList()
        trendsFlow.value = emptyList()
        todayOverride = null
    }

    override suspend fun findAllEntries(): List<JournalEntry> = entriesFlow.value

    override suspend fun findAllProfiles(): List<AITrendProfile> = trendsFlow.value

    override suspend fun mergeImport(
        entries: List<JournalEntry>,
        profiles: List<AITrendProfile>,
        overwrite: Boolean,
    ): ImportMergeReport {
        val existingByDay = entriesFlow.value.associateBy { it.dayBucket }
        val merged = entriesFlow.value.toMutableList()
        var insertedEntries = 0
        var skippedEntries = 0
        for (incoming in entries) {
            val existing = existingByDay[incoming.dayBucket]
            if (existing == null) {
                merged += incoming.copy(id = (merged.maxOfOrNull { it.id } ?: 0L) + 1)
                insertedEntries++
            } else if (overwrite) {
                val idx = merged.indexOfFirst { it.id == existing.id }
                if (idx >= 0) merged[idx] = incoming.copy(id = existing.id)
                insertedEntries++
            } else {
                skippedEntries++
            }
        }
        entriesFlow.value = merged

        val existingProfileKeys = trendsFlow.value
            .mapTo(HashSet()) { Triple(it.periodStart, it.periodEnd, it.generatedAt) }
        val mergedProfiles = trendsFlow.value.toMutableList()
        var insertedProfiles = 0
        var skippedProfiles = 0
        for (incoming in profiles) {
            val key = Triple(incoming.periodStart, incoming.periodEnd, incoming.generatedAt)
            if (key !in existingProfileKeys || overwrite) {
                mergedProfiles += incoming.copy(
                    id = (mergedProfiles.maxOfOrNull { it.id } ?: 0L) + 1,
                )
                insertedProfiles++
            } else {
                skippedProfiles++
            }
        }
        trendsFlow.value = mergedProfiles

        return ImportMergeReport(
            insertedEntries = insertedEntries,
            skippedEntries = skippedEntries,
            insertedProfiles = insertedProfiles,
            skippedProfiles = skippedProfiles,
        )
    }
}
