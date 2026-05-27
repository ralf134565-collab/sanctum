// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.data.local.converter

import androidx.room.TypeConverter
import com.pocketreflect.app.domain.model.MoodTag

/**
 * TypeConverter'ы для Room.
 *
 * Почему НЕ JSON / kotlinx.serialization:
 *  - Не хочется тащить ещё одну рантайм-зависимость и Gradle-плагин ради
 *    тривиального списка стабильных enum-ключей.
 *  - Простой формат с разделителем — детерминированный, версионируется руками,
 *    не зависит от reflection, переживает Proguard/R8 без правил.
 *
 * Формат хранения тегов: "calm|gratitude|focused".
 * Разделитель `|` выбран намеренно: он запрещён в [MoodTag.storageKey]
 * (там только латиница в нижнем регистре), коллизий быть не может.
 */
class JournalConverters {

    @TypeConverter
    fun fromMoodTagList(tags: List<MoodTag>?): String =
        tags
            ?.distinct()
            ?.joinToString(separator = DELIMITER) { it.storageKey }
            .orEmpty()

    @TypeConverter
    fun toMoodTagList(raw: String?): List<MoodTag> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(DELIMITER)
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            // Неизвестные теги (после downgrade БД) тихо отбрасываем, не падая.
            .mapNotNull(MoodTag::fromStorageKeyOrNull)
            .distinct()
            .toList()
    }

    private companion object {
        const val DELIMITER = "|"
    }
}
