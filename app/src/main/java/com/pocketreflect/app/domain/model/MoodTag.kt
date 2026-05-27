// SPDX-License-Identifier: GPL-3.0-or-later
package com.pocketreflect.app.domain.model

import com.pocketreflect.app.core.locale.AppLanguage

/**
 * Аффективный лейбл, которым пользователь маркирует завершение дня.
 *
 * Намеренно НЕ используем числовые шкалы (1–10) — это «эмоциональное насилие»
 * над пользователем и плохо коррелирует с реальным состоянием
 * (см. affective labeling research, Lieberman et al.).
 *
 * Список фиксированный и неэкзотический: добавляем теги осторожно,
 * чтобы не превращать выбор в новую когнитивную нагрузку (парадокс выбора).
 *
 * Поле [storageKey] — стабильный идентификатор для сериализации в SQLite.
 * Категория [polarity] помогает ИИ-движку и UI быстро реагировать на негатив
 * (например, скрывать блок «микро-победы» при тревоге).
 */
enum class MoodTag(
    val storageKey: String,
    val displayName: String,
    val polarity: Polarity,
) {
    CALM(storageKey = "calm", displayName = "Спокойствие", polarity = Polarity.POSITIVE),
    JOY(storageKey = "joy", displayName = "Радость", polarity = Polarity.POSITIVE),
    GRATITUDE(storageKey = "gratitude", displayName = "Благодарность", polarity = Polarity.POSITIVE),
    FOCUSED(storageKey = "focused", displayName = "Сфокусированность", polarity = Polarity.POSITIVE),

    TIRED(storageKey = "tired", displayName = "Усталость", polarity = Polarity.NEUTRAL),
    OVERWHELMED(storageKey = "overwhelmed", displayName = "Перегруз", polarity = Polarity.NEUTRAL),

    ANXIETY(storageKey = "anxiety", displayName = "Тревога", polarity = Polarity.NEGATIVE),
    SADNESS(storageKey = "sadness", displayName = "Грусть", polarity = Polarity.NEGATIVE),
    IRRITATION(storageKey = "irritation", displayName = "Раздражение", polarity = Polarity.NEGATIVE),
    ;

    enum class Polarity { POSITIVE, NEUTRAL, NEGATIVE }

    fun displayName(language: AppLanguage): String =
        if (language.isEnglish) ENGLISH_NAMES[this] ?: displayName else displayName

    companion object {
        init {
            entries.forEach { tag ->
                require('|' !in tag.storageKey) {
                    "storageKey '${tag.storageKey}' contains delimiter '|'"
                }
            }
        }

        /** Безопасный парсинг из БД: неизвестные ключи (после миграции) тихо отбрасываем. */
        fun fromStorageKeyOrNull(key: String): MoodTag? =
            entries.firstOrNull { it.storageKey.equals(key, ignoreCase = true) }

        /** Полный набор тегов в порядке отображения на экране (позитив → нейтрал → негатив). */
        val orderedForUi: List<MoodTag> by lazy {
            entries.sortedBy { it.ordinal }
        }

        private val ENGLISH_NAMES: Map<MoodTag, String> = mapOf(
            CALM to "Calm",
            JOY to "Joy",
            GRATITUDE to "Gratitude",
            FOCUSED to "Focused",
            TIRED to "Tired",
            OVERWHELMED to "Overwhelmed",
            ANXIETY to "Anxiety",
            SADNESS to "Sadness",
            IRRITATION to "Irritation",
        )
    }
}

/**
 * Расширения над набором выбранных тегов — используются и в UI, и в AI-движке.
 * Один источник истины «есть ли тут негатив?», чтобы избежать рассинхрона.
 */
val Set<MoodTag>.hasNegative: Boolean
    get() = any { it.polarity == MoodTag.Polarity.NEGATIVE }

val Set<MoodTag>.hasPositive: Boolean
    get() = any { it.polarity == MoodTag.Polarity.POSITIVE }
