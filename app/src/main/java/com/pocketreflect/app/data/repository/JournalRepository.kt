// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.repository

import androidx.room.withTransaction
import com.pocketreflect.app.core.security.DatabaseAccess
import com.pocketreflect.app.core.security.DatabaseProvider
import com.pocketreflect.app.core.time.DayBucket
import com.pocketreflect.app.data.local.entity.AITrendProfile
import com.pocketreflect.app.data.local.entity.JournalEntry
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

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
    private val databaseAccess: DatabaseAccess,
) : JournalRepository {

    override fun observeHistory(): Flow<List<JournalEntry>> =
        databaseAccess.observeWhenReady(emptyList()) {
            databaseProvider.get().journalDao().observeAll()
        }

    override fun observeToday(): Flow<JournalEntry?> =
        databaseAccess.observeWhenReady(null) {
            databaseProvider.get().journalDao().observeByDay(DayBucket.today())
        }

    override suspend fun findToday(): JournalEntry? =
        databaseAccess.whenReady {
            databaseProvider.get().journalDao().findByDay(DayBucket.today())
        }

    override suspend fun findByDay(dayBucket: String): JournalEntry? =
        databaseAccess.whenReady {
            databaseProvider.get().journalDao().findByDay(dayBucket)
        }

    override suspend fun saveEntry(entry: JournalEntry): Long =
        databaseAccess.whenReady {
            val journalDao = databaseProvider.get().journalDao()
            val existing = journalDao.findByDay(entry.dayBucket)
            val normalized = if (existing != null) entry.copy(id = existing.id) else entry
            journalDao.upsert(normalized)
        }

    override suspend fun delete(entry: JournalEntry) =
        databaseAccess.whenReady {
            databaseProvider.get().journalDao().delete(entry)
        }

    override suspend fun findById(id: Long): JournalEntry? =
        databaseAccess.whenReady {
            databaseProvider.get().journalDao().findById(id)
        }

    override fun observeById(id: Long): Flow<JournalEntry?> =
        databaseAccess.observeWhenReady(null) {
            databaseProvider.get().journalDao().observeById(id)
        }

    override suspend fun entriesForLastDays(days: Int): List<JournalEntry> =
        databaseAccess.whenReady {
            val journalDao = databaseProvider.get().journalDao()
            val fromDate = DayBucket.toLocalDate(DayBucket.today())
                .minusDays((days - 1).toLong())
            journalDao.entriesSinceDayBucket(DayBucket.fromLocalDate(fromDate))
        }

    override suspend fun findEntryInDayRange(fromDayBucket: String, toDayBucket: String): JournalEntry? =
        databaseAccess.whenReady {
            databaseProvider.get().journalDao().findEntryInDayRange(fromDayBucket, toDayBucket)
        }

    override suspend fun findLastNEntries(count: Int): List<JournalEntry> =
        databaseAccess.whenReady {
            databaseProvider.get().journalDao().findLastNEntries(count)
        }

    override fun observeTrendProfiles(): Flow<List<AITrendProfile>> =
        databaseAccess.observeWhenReady(emptyList()) {
            databaseProvider.get().aiTrendProfileDao().observeAll()
        }

    override suspend fun latestTrendProfile(): AITrendProfile? =
        databaseAccess.whenReady {
            databaseProvider.get().aiTrendProfileDao().latest()
        }

    override suspend fun saveTrendProfile(profile: AITrendProfile): Long =
        databaseAccess.whenReady {
            databaseProvider.get().aiTrendProfileDao().insert(profile)
        }

    override suspend fun wipeAll() {
        databaseAccess.whenReady {
            val db = databaseProvider.get()
            db.withTransaction {
                db.journalDao().deleteAll()
                db.aiTrendProfileDao().deleteAll()
                db.chatMessageDao().deleteAll()
            }
        }
    }

    override suspend fun findAllEntries(): List<JournalEntry> =
        databaseAccess.whenReady {
            databaseProvider.get().journalDao().findAll()
        }

    override suspend fun findAllProfiles(): List<AITrendProfile> =
        databaseAccess.whenReady {
            databaseProvider.get().aiTrendProfileDao().findAll()
        }

    override suspend fun mergeImport(
        entries: List<JournalEntry>,
        profiles: List<AITrendProfile>,
        overwrite: Boolean,
    ): ImportMergeReport = databaseAccess.whenReady {
        val db = databaseProvider.get()
        db.withTransaction {
            val journalDao = db.journalDao()
            val aiTrendProfileDao = db.aiTrendProfileDao()
            val existingByDay: Map<String, JournalEntry> = journalDao.findAll()
                .associateBy { it.dayBucket }
            val existingProfileKeys: Set<Triple<Long, Long, Long>> =
                aiTrendProfileDao.findAll()
                    .mapTo(HashSet()) { Triple(it.periodStart, it.periodEnd, it.generatedAt) }

            var insertedEntries = 0
            var skippedEntries = 0
            for (incoming in entries) {
                val existing = existingByDay[incoming.dayBucket]
                if (existing == null) {
                    journalDao.upsert(incoming.copy(id = 0L))
                    insertedEntries++
                } else if (overwrite) {
                    journalDao.upsert(incoming.copy(id = existing.id))
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
                    aiTrendProfileDao.insert(incoming.copy(id = 0L))
                    insertedProfiles++
                } else if (overwrite) {
                    aiTrendProfileDao.insert(incoming.copy(id = 0L))
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
}
