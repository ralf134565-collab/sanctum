// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.transfer

import com.pocketreflect.app.data.local.entity.AITrendProfile
import com.pocketreflect.app.data.local.entity.JournalEntry
import com.pocketreflect.app.domain.model.MoodTag
import kotlinx.serialization.Serializable

/**
 * Корневой DTO зашифрованного бэкапа.
 *
 * Содержит ровно то, что нужно для переноса между устройствами:
 *  - схему (для будущих миграций);
 *  - время экспорта (чтобы пользователь видел его в UI «восстановлено из бэкапа от …»);
 *  - дневниковые записи и AI-профили.
 *
 * Сознательно отсутствует:
 *  - имя пользователя / e-mail / install-id / device-name — нет такой
 *    информации в БД, и здесь её тоже не появится (Acceptance criterion из брифа).
 */
@Serializable
data class TransferFileDto(
    val schemaVersion: Int,
    val exportedAt: Long,
    val entries: List<JournalEntryDto>,
    val profiles: List<AITrendProfileDto>,
)

/**
 * DTO-копия [JournalEntry] для сериализации.
 *
 * Отделяем от Room entity сознательно:
 *  - Room-аннотации не «протекают» в формат файла;
 *  - изменение схемы Room (например, переименование колонки) не ломает
 *    обратную совместимость файлов — DTO можно держать стабильной,
 *    а маппинг править в одном месте.
 *
 * `id` намеренно не сохраняем: при импорте PK генерируется заново,
 * а перенос id между устройствами никакой полезной семантики не несёт.
 *
 * `moodTags` сериализуем как список stable storage-keys (`MoodTag.storageKey`).
 * Это согласуется с тем, как теги хранятся в Room через
 * [com.pocketreflect.app.data.local.converter.JournalConverters]:
 * неизвестные ключи при импорте тихо отбрасываются.
 */
@Serializable
data class JournalEntryDto(
    val timestamp: Long,
    val dayBucket: String,
    val moodTags: List<String>,
    val microWins: String,
    val tomorrowTasks: String,
    val reflection: String,
    val promptShown: String,
    val aiReflection: String?,
    val customFieldAnswer: String = "",
    val customFieldQuestion: String = "",
)

@Serializable
data class AITrendProfileDto(
    val periodStart: Long,
    val periodEnd: Long,
    val generatedAt: Long,
    val entryCount: Int,
    val summary: String,
    val structuredJson: String?,
    val schemaVersion: Int,
)

// --- Мапперы entity <-> DTO ---

internal fun JournalEntry.toDto(): JournalEntryDto = JournalEntryDto(
    timestamp = timestamp,
    dayBucket = dayBucket,
    moodTags = moodTags.map { it.storageKey },
    microWins = microWins,
    tomorrowTasks = tomorrowTasks,
    reflection = reflection,
    promptShown = promptShown,
    aiReflection = aiReflection,
    customFieldAnswer = customFieldAnswer,
    customFieldQuestion = customFieldQuestion,
)

internal fun JournalEntryDto.toEntity(): JournalEntry = JournalEntry(
    id = 0L,
    timestamp = timestamp,
    dayBucket = dayBucket,
    moodTags = moodTags.mapNotNull(MoodTag::fromStorageKeyOrNull).distinct(),
    microWins = microWins,
    tomorrowTasks = tomorrowTasks,
    reflection = reflection,
    promptShown = promptShown,
    aiReflection = aiReflection,
    customFieldAnswer = customFieldAnswer,
    customFieldQuestion = customFieldQuestion,
)

internal fun AITrendProfile.toDto(): AITrendProfileDto = AITrendProfileDto(
    periodStart = periodStart,
    periodEnd = periodEnd,
    generatedAt = generatedAt,
    entryCount = entryCount,
    summary = summary,
    structuredJson = structuredJson,
    schemaVersion = schemaVersion,
)

internal fun AITrendProfileDto.toEntity(): AITrendProfile = AITrendProfile(
    id = 0L,
    periodStart = periodStart,
    periodEnd = periodEnd,
    generatedAt = generatedAt,
    entryCount = entryCount,
    summary = summary,
    structuredJson = structuredJson,
    schemaVersion = schemaVersion,
)
