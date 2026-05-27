// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.prompts

import com.pocketreflect.app.core.locale.AppLanguage
import com.pocketreflect.app.domain.model.MoodTag
import kotlin.random.Random

/**
 * Политика выбора промпта на основе выбранных эмоциональных тегов (Блок 1).
 * Реализует приоритет негативного/нейтрального фокуса над позитивным и fallback-цепочки.
 */
object MoodPromptPolicy {

    /**
     * Возвращает один доминантный тег на основе клинического приоритета:
     * NEGATIVE (Тревога, Грусть, Раздражение) > NEUTRAL (Усталость, Перегруз) > POSITIVE (Спокойствие, Радость, Благодарность, Сфокусированность).
     */
    fun resolveDominantTag(
        selectedTags: Set<MoodTag>,
        random: Random = Random.Default
    ): MoodTag? {
        if (selectedTags.isEmpty()) return null

        // 1. Проверяем негативные теги
        val negatives = selectedTags.filter { it.polarity == MoodTag.Polarity.NEGATIVE }
        if (negatives.isNotEmpty()) {
            return negatives[random.nextInt(negatives.size)]
        }

        // 2. Проверяем нейтральные теги
        val neutrals = selectedTags.filter { it.polarity == MoodTag.Polarity.NEUTRAL }
        if (neutrals.isNotEmpty()) {
            return neutrals[random.nextInt(neutrals.size)]
        }

        // 3. Выбираем из оставшихся позитивных тегов
        val positives = selectedTags.filter { it.polarity == MoodTag.Polarity.POSITIVE }
        if (positives.isNotEmpty()) {
            return positives[random.nextInt(positives.size)]
        }

        return null
    }

    /**
     * Возвращает пул промптов для конкретного тега.
     */
    fun getPoolForTag(language: AppLanguage, tag: MoodTag): List<String> {
        return DailyPrompts.poolForTag(language, tag)
    }

    /**
     * Возвращает пул промптов для полярности (все промпты тегов этой полярности).
     */
    fun getPoolForPolarity(language: AppLanguage, polarity: MoodTag.Polarity): List<String> {
        val tagsOfPolarity = MoodTag.entries.filter { it.polarity == polarity }
        return tagsOfPolarity.flatMap { getPoolForTag(language, it) }
    }

    /**
     * Возвращает универсальный / fallback пул промптов.
     */
    fun getUniversalPool(language: AppLanguage): List<String> {
        return if (language.isEnglish) DailyPromptsEn.UNIVERSAL else DailyPrompts.UNIVERSAL
    }

    /**
     * Возвращает полный пул для данного языка.
     */
    fun getFullLanguagePool(language: AppLanguage): List<String> {
        return if (language.isEnglish) DailyPromptsEn.POOL else DailyPrompts.all(language)
    }
}
