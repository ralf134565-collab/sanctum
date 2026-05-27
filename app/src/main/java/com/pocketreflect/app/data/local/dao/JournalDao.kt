// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.pocketreflect.app.data.local.entity.JournalEntry
import kotlinx.coroutines.flow.Flow

/**
 * DAO для записей «Итоги дня».
 *
 * Соглашения:
 *  - Чтение списков отдаётся через `Flow` → Compose автоматически реагирует
 *    на изменения БД, никаких ручных рефрешей.
 *  - Запись — suspend, чтобы вызовы шли вне UI-потока (Room сам уводит
 *    их на IO-диспатчер из своего пула).
 *  - Используем `Upsert`, чтобы экран «Итоги дня» мог идемпотентно
 *    перезаписать запись текущего дня без поиска по id (см. dayBucket).
 */
@Dao
interface JournalDao {

    @Upsert
    suspend fun upsert(entry: JournalEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<JournalEntry>)

    @Delete
    suspend fun delete(entry: JournalEntry)

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Вся история — для экрана календаря/ленты. */
    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<JournalEntry>>

    /**
     * Записи за календарный месяц по префиксу [dayBucket] (`YYYY-MM`).
     * Используется для агрегации частотности тегов и других помесячных отчётов.
     */
    @Query(
        """
        SELECT * FROM journal_entries
        WHERE dayBucket LIKE :monthPrefix || '%'
        ORDER BY timestamp DESC
        """,
    )
    suspend fun findByMonthPrefix(monthPrefix: String): List<JournalEntry>

    /**
     * Снимок всей истории.
     * Используется операцией экспорта (`BackupRepository.export`) — там
     * `Flow` не нужен, а одноразовый запрос проще для транзакционности.
     */
    @Query("SELECT * FROM journal_entries ORDER BY timestamp ASC")
    suspend fun findAll(): List<JournalEntry>

    /** Запись за конкретный локальный день, если уже есть. */
    @Query("SELECT * FROM journal_entries WHERE dayBucket = :dayBucket LIMIT 1")
    suspend fun findByDay(dayBucket: String): JournalEntry?

    /** Поиск записи в диапазоне дат (для Feature 5 "Time Echo" или Feature 2 "Short Evening") */
    @Query("SELECT * FROM journal_entries WHERE dayBucket BETWEEN :fromDayBucket AND :toDayBucket ORDER BY dayBucket DESC LIMIT 1")
    suspend fun findEntryInDayRange(fromDayBucket: String, toDayBucket: String): JournalEntry?

    /** Точечная загрузка записи по id (для экрана детали). */
    @Query("SELECT * FROM journal_entries WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): JournalEntry?

    /** Реактивная подписка на запись по id — чтобы UI обновлялся, если её удалят. */
    @Query("SELECT * FROM journal_entries WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<JournalEntry?>

    /** Реактивный аналог — для подсветки «уже записано сегодня» в UI. */
    @Query("SELECT * FROM journal_entries WHERE dayBucket = :dayBucket LIMIT 1")
    fun observeByDay(dayBucket: String): Flow<JournalEntry?>

    /** Найти последние N записей по дате */
    @Query("SELECT * FROM journal_entries ORDER BY dayBucket DESC LIMIT :limit")
    suspend fun findLastNEntries(limit: Int): List<JournalEntry>

    /**
     * Окно последних N календарных дней (включая сегодня) — для еженедельной
     * суммаризации. Фильтр по [dayBucket], а не по [timestamp], чтобы записи
     * за прошлые дни с «полуденным» timestamp не выпадали из окна.
     */
    @Query("SELECT * FROM journal_entries WHERE dayBucket >= :fromDayBucket ORDER BY timestamp ASC")
    suspend fun entriesSinceDayBucket(fromDayBucket: String): List<JournalEntry>

    @Query("SELECT COUNT(*) FROM journal_entries")
    suspend fun count(): Int

    /**
     * Безусловное удаление всех записей.
     * Использовать только в составе `wipeAll` репозитория (внутри транзакции).
     */
    @Query("DELETE FROM journal_entries")
    suspend fun deleteAll()
}
