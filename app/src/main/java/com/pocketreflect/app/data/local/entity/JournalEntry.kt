// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pocketreflect.app.domain.model.MoodTag

/**
 * Запись «Итоги дня» в локальной БД (Room/SQLite).
 *
 * Архитектурные замечания:
 *  - Это Data-сущность Room. Маппинг на доменную модель (если усложнимся)
 *    делается в репозитории, чтобы Room-аннотации не «протекали» в domain.
 *  - Список тегов хранится как сериализованная строка через TypeConverter
 *    (`JournalConverters`). SQLite не поддерживает массивы нативно.
 *  - Индекс по `dayBucket` (день в локальной TZ устройства) позволит
 *    быстро искать «есть ли уже запись за сегодня?» без full scan.
 *  - `aiReflection` хранит сгенерированный локально (Gemma 4) текст,
 *    чтобы при повторном открытии экрана не пересчитывать.
 */
@Entity(
    tableName = "journal_entries",
    indices = [
        Index(value = ["dayBucket"], unique = true),
        Index(value = ["timestamp"]),
    ],
)
data class JournalEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** Эпоха в миллисекундах. Источник истины — System.currentTimeMillis(). */
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    /**
     * Локальный день в формате YYYY-MM-DD (TZ устройства).
     * Денормализация ради скорости и устойчивости к смене таймзоны.
     */
    @ColumnInfo(name = "dayBucket")
    val dayBucket: String,

    /** Список выбранных аффективных тегов (см. [MoodTag]). */
    @ColumnInfo(name = "moodTags")
    val moodTags: List<MoodTag>,

    /** Блок «Кристаллизация усилий». Может быть пустым, если был тег негатива. */
    @ColumnInfo(name = "microWins")
    val microWins: String,

    /** «Экстернализация задач» — фокусы на завтра (1–3 строки, валидация в UI/VM). */
    @ColumnInfo(name = "tomorrowTasks")
    val tomorrowTasks: String,

    /** Свободная рефлексия пользователя (под промпт дня). */
    @ColumnInfo(name = "reflection")
    val reflection: String,

    /** Какой промпт был показан в этот день (для воспроизводимости истории). */
    @ColumnInfo(name = "promptShown")
    val promptShown: String,

    /** Эмпатичный отклик локальной Gemma 4 — кэшируем, чтобы не пересчитывать. */
    @ColumnInfo(name = "aiReflection")
    val aiReflection: String?,

    /** Ответ на пользовательское поле (если было включено при сохранении). */
    @ColumnInfo(name = "customFieldAnswer")
    val customFieldAnswer: String = "",

    /** Снимок формулировки вопроса на момент сохранения. */
    @ColumnInfo(name = "customFieldQuestion")
    val customFieldQuestion: String = "",
)
