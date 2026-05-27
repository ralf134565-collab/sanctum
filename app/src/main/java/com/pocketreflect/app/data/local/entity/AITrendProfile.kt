// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Компактный «AI-профиль ментальных трендов».
 *
 * Назначение (Future-Proofing согласно архитектуре):
 *  Раз в неделю WorkManager поднимает локальную Gemma 4, прогоняет через неё
 *  историю последних 7 (или больше) дней и сохраняет здесь СЖАТУЮ выжимку:
 *   - ведущие эмоции недели,
 *   - повторяющиеся темы тревоги,
 *   - микро-прогресс пользователя.
 *
 *  В чате с пользователем (или в подсказке промпта) мы потом передаём
 *  модели не сырую историю (контекст-окно дорогое), а именно этот профиль.
 *
 *  Поле [summary] хранит уже сжатый, человеко-читаемый текст,
 *  поле [structuredJson] — опциональный «машинный» срез
 *  (теги/счётчики/паттерны), чтобы можно было строить графики или
 *  feed-ить обратно в модель без re-parsing.
 */
@Entity(tableName = "ai_trend_profiles")
data class AITrendProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** Начало периода суммаризации (включительно), epoch millis. */
    @ColumnInfo(name = "periodStart")
    val periodStart: Long,

    /** Конец периода суммаризации (включительно), epoch millis. */
    @ColumnInfo(name = "periodEnd")
    val periodEnd: Long,

    /** Когда профиль был сформирован моделью (для аудита и инвалидации). */
    @ColumnInfo(name = "generatedAt")
    val generatedAt: Long,

    /** Сколько записей в БД попало в эту суммаризацию. */
    @ColumnInfo(name = "entryCount")
    val entryCount: Int,

    /** Человеко-читаемая выжимка (используется как контекст для следующих ответов). */
    @ColumnInfo(name = "summary")
    val summary: String,

    /** Опциональный JSON со структурированной агрегацией (теги, частоты, темы). */
    @ColumnInfo(name = "structuredJson")
    val structuredJson: String?,

    /** Версия prompt-стратегии суммаризации (для будущих миграций пайплайна). */
    @ColumnInfo(name = "schemaVersion")
    val schemaVersion: Int = 1,
)
