// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pocketreflect.app.data.local.entity.AITrendProfile
import kotlinx.coroutines.flow.Flow

/**
 * DAO для хранения сжатого ИИ-профиля ментальных трендов.
 * Записи только append-only: новые суммаризации не затирают старые,
 * чтобы можно было ретроспективно посмотреть «как мне было месяц назад».
 */
@Dao
interface AITrendProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: AITrendProfile): Long

    @Query("SELECT * FROM ai_trend_profiles ORDER BY periodEnd DESC")
    fun observeAll(): Flow<List<AITrendProfile>>

    /** Снимок всех профилей — для экспорта в зашифрованный бэкап. */
    @Query("SELECT * FROM ai_trend_profiles ORDER BY periodEnd ASC")
    suspend fun findAll(): List<AITrendProfile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(profiles: List<AITrendProfile>): List<Long>

    /** Последний валидный профиль — то, что мы будем подмешивать в контекст модели. */
    @Query(
        "SELECT * FROM ai_trend_profiles " +
            "WHERE TRIM(summary) != '' " +
            "ORDER BY generatedAt DESC LIMIT 1",
    )
    suspend fun latest(): AITrendProfile?

    @Query("DELETE FROM ai_trend_profiles WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Полная очистка таблицы (используется только в [wipeAll] репозитория). */
    @Query("DELETE FROM ai_trend_profiles")
    suspend fun deleteAll()
}
