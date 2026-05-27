// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.repository

import androidx.room.withTransaction
import com.pocketreflect.app.core.security.AuthSessionHolder
import com.pocketreflect.app.core.security.DatabaseProvider
import com.pocketreflect.app.core.time.DayBucket
import com.pocketreflect.app.data.local.entity.AITrendProfile
import com.pocketreflect.app.data.local.entity.JournalEntry
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Абстракция над локальным хранилищем для presentation- и domain-слоёв.
 */
interface JournalRepository {

    fun observeHistory(): Flow<List<JournalEntry>>
    fun observeToday(): Flow<JournalEntry?>
    suspend fun findToday(): JournalEntry?

    suspend fun findByDay(dayBucket: String): JournalEntry?

    suspend fun saveEntry(entry: JournalEntry): Long

    suspend fun delete(entry: JournalEntry)
    suspend fun findById(id: Long): JournalEntry?
    fun observeById(id: Long): Flow<JournalEntry?>

    suspend fun entriesForLastDays(days: Int): List<JournalEntry>

    suspend fun findEntryInDayRange(fromDayBucket: String, toDayBucket: String): JournalEntry?

    suspend fun findLastNEntries(count: Int): List<JournalEntry>

    fun observeTrendProfiles(): Flow<List<AITrendProfile>>
    suspend fun latestTrendProfile(): AITrendProfile?
    suspend fun saveTrendProfile(profile: AITrendProfile): Long

    suspend fun wipeAll()

    suspend fun findAllEntries(): List<JournalEntry>

    suspend fun findAllProfiles(): List<AITrendProfile>

    suspend fun mergeImport(
        entries: List<JournalEntry>,
        profiles: List<AITrendProfile>,
        overwrite: Boolean,
    ): ImportMergeReport
}

data class ImportMergeReport(
    val insertedEntries: Int,
    val skippedEntries: Int,
    val insertedProfiles: Int,
    val skippedProfiles: Int,
)

/**
 * Room-реализация [JournalRepository].
 * Биндинг — в `di/RepositoryModule.kt`.
 */
@Singleton
class RoomJournalRepository @Inject constructor(
    private val databaseProvider: DatabaseProvider,
    private val authSessionHolder: Lazy<AuthSessionHolder>,
) : JournalRepository {

    private fun journalDao() = databaseProvider.get().journalDao()
    private fun aiTrendProfileDao() = databaseProvider.get().aiTrendProfileDao()
    private fun chatMessageDao() = databaseProvider.get().chatMessageDao()

    private fun dbReadyFlow(): Flow<Boolean> = combine(
        authSessionHolder.get().isAuthenticated,
        databaseProvider.isLocked,
        databaseProvider.revision,
    ) { authenticated, locked, _ -> authenticated && !locked }

    override fun observeHistory(): Flow<List<JournalEntry>> =
        dbReadyFlow().flatMapLatest { ready ->
            if (ready) {
                runCatching { journalDao().observeAll() }
                    .getOrElse { emptyFlow() }
                    .catch { emit(emptyList()) }
            } else {
                emptyFlow()
            }
        }

    override fun observeToday(): Flow<JournalEntry?> =
        dbReadyFlow().flatMapLatest { ready ->
            if (ready) {
                runCatching { journalDao().observeByDay(DayBucket.today()) }
                    .getOrElse { emptyFlow() }
                    .catch { emit(null) }
            } else {
                emptyFlow()
            }
        }

    override suspend fun findToday(): JournalEntry? =
        journalDao().findByDay(DayBucket.today())

    override suspend fun findByDay(dayBucket: String): JournalEntry? =
        journalDao().findByDay(dayBucket)

    override suspend fun saveEntry(entry: JournalEntry): Long {
        val existing = journalDao().findByDay(entry.dayBucket)
        val normalized = if (existing != null) entry.copy(id = existing.id) else entry
        return journalDao().upsert(normalized)
    }

    override suspend fun delete(entry: JournalEntry) = journalDao().delete(entry)

    override suspend fun findById(id: Long): JournalEntry? = journalDao().findById(id)

    override fun observeById(id: Long): Flow<JournalEntry?> =
        dbReadyFlow().flatMapLatest { ready ->
            if (ready) {
                runCatching { journalDao().observeById(id) }
                    .getOrElse { emptyFlow() }
                    .catch { emit(null) }
            } else {
                emptyFlow()
            }
        }

    override suspend fun entriesForLastDays(days: Int): List<JournalEntry> {
        val fromDate = DayBucket.toLocalDate(DayBucket.today())
            .minusDays((days - 1).toLong())
        return journalDao().entriesSinceDayBucket(DayBucket.fromLocalDate(fromDate))
    }

    override suspend fun findEntryInDayRange(fromDayBucket: String, toDayBucket: String): JournalEntry? =
        journalDao().findEntryInDayRange(fromDayBucket, toDayBucket)

    override suspend fun findLastNEntries(count: Int): List<JournalEntry> =
        journalDao().findLastNEntries(count)

    override fun observeTrendProfiles(): Flow<List<AITrendProfile>> =
        dbReadyFlow().flatMapLatest { ready ->
            if (ready) {
                runCatching { aiTrendProfileDao().observeAll() }
                    .getOrElse { emptyFlow() }
                    .catch { emit(emptyList()) }
            } else {
                emptyFlow()
            }
        }

    override suspend fun latestTrendProfile(): AITrendProfile? = aiTrendProfileDao().latest()

    override suspend fun saveTrendProfile(profile: AITrendProfile): Long =
        aiTrendProfileDao().insert(profile)

    override suspend fun wipeAll() {
        databaseProvider.get().withTransaction {
            journalDao().deleteAll()
            aiTrendProfileDao().deleteAll()
            chatMessageDao().deleteAll()
        }
    }

    override suspend fun findAllEntries(): List<JournalEntry> = journalDao().findAll()

    override suspend fun findAllProfiles(): List<AITrendProfile> = aiTrendProfileDao().findAll()

    override suspend fun mergeImport(
        entries: List<JournalEntry>,
        profiles: List<AITrendProfile>,
        overwrite: Boolean,
    ): ImportMergeReport = databaseProvider.get().withTransaction {
        val existingByDay: Map<String, JournalEntry> = journalDao().findAll()
            .associateBy { it.dayBucket }
        val existingProfileKeys: Set<Triple<Long, Long, Long>> =
            aiTrendProfileDao().findAll()
                .mapTo(HashSet()) { Triple(it.periodStart, it.periodEnd, it.generatedAt) }

        var insertedEntries = 0
        var skippedEntries = 0
        for (incoming in entries) {
            val existing = existingByDay[incoming.dayBucket]
            if (existing == null) {
                journalDao().upsert(incoming.copy(id = 0L))
                insertedEntries++
            } else if (overwrite) {
                journalDao().upsert(incoming.copy(id = existing.id))
                insertedEntries++
            } else {
                skippedEntries++
            }
        }

        var insertedProfiles = 0
        var skippedProfiles = 0
        for (incoming in profiles) {
            val key = Triple(incoming.periodStart, incoming.periodEnd, incoming.generatedAt)
            if (key !in existingProfileKeys) {
                aiTrendProfileDao().insert(incoming.copy(id = 0L))
                insertedProfiles++
            } else if (overwrite) {
                aiTrendProfileDao().insert(incoming.copy(id = 0L))
                insertedProfiles++
            } else {
                skippedProfiles++
            }
        }

        ImportMergeReport(
            insertedEntries = insertedEntries,
            skippedEntries = skippedEntries,
            insertedProfiles = insertedProfiles,
            skippedProfiles = skippedProfiles,
        )
    }
}
